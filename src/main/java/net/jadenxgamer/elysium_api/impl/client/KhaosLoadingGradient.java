package net.jadenxgamer.elysium_api.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.util.UniversalModLoaderHelper;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30C;

import java.util.ArrayList;
import java.util.List;

public final class KhaosLoadingGradient {

    private static final float GRADIENT_FADE_IN_MILLIS = 1250.0F;
    private static final int GRADIENT_COLOR = 0x9100BA;
    private static final int GRADIENT_HEIGHT_NUMERATOR = 2;
    private static final int GRADIENT_HEIGHT_DENOMINATOR = 5;

    private static final int ICON_SIZE = 16;
    private static final int ICON_SPACING = 3;
    private static final float ICON_PULSE_PERIOD_MILLIS = 1000.0F;

    private static final float GRADIENT_COLOR_R = ((GRADIENT_COLOR >> 16) & 0xFF) / 255.0F;
    private static final float GRADIENT_COLOR_G = ((GRADIENT_COLOR >> 8) & 0xFF) / 255.0F;
    private static final float GRADIENT_COLOR_B = (GRADIENT_COLOR & 0xFF) / 255.0F;

    private static final String GRADIENT_VERTEX_SHADER = """
            #version 150 core
            
            in vec2 position;
            in vec2 tex;
            out vec2 vTex;
            uniform vec2 screenSize;
            uniform vec4 region;
            
            void main() {
                vTex = tex;
                vec2 pixelPos = mix(region.xy, region.zw, position);
                gl_Position = vec4((pixelPos / screenSize) * 2.0 - 1.0, 0.0, 1.0);
            }
            """;

    private static final String GRADIENT_FRAGMENT_SHADER = """
            #version 150 core
            
            uniform sampler2D tex;
            uniform float fadeAlpha;
            uniform vec3 gradientColor;
            in vec2 vTex;
            out vec4 fragColor;
            
            void main() {
                vec4 c = texture(tex, vTex);
                float brightness = max(max(c.r, c.g), c.b);
                vec3 normalized = c.rgb / max(brightness, 0.001);
                float redness = clamp((normalized.r - 0.5 * normalized.g - 0.5 * normalized.b) * 2.0, 0.0, 1.0);
                float gradient = 1.0 - smoothstep(0.1, 0.95, vTex.y);
                float blend = redness * gradient * fadeAlpha;
                vec3 resultRGB = mix(c.rgb, gradientColor, blend);
                fragColor = vec4(resultRGB, 1.0);
            }
            """;

    private static final String ICON_VERTEX_SHADER = """
            #version 150 core
            
            in vec2 position;
            uniform vec2 screenSize;
            uniform vec2 quadPosition;
            uniform vec2 quadSize;
            uniform vec4 uvRect;
            out vec2 vTex;
            
            void main() {
                vec2 pixelPos = quadPosition + position * quadSize;
                vTex = vec2(mix(uvRect.x, uvRect.z, position.x), mix(uvRect.y, uvRect.w, position.y));
                gl_Position = vec4((pixelPos / screenSize) * 2.0 - 1.0, 0.0, 1.0);
            }
            """;

    private static final String ICON_FRAGMENT_SHADER = """
            #version 150 core
            
            uniform sampler2D tex;
            uniform float fadeAlpha;
            in vec2 vTex;
            out vec4 fragColor;
            
            void main() {
                vec4 c = texture(tex, vTex);
                fragColor = vec4(c.rgb, c.a * fadeAlpha);
            }
            """;

    private static final float[] QUAD_VERTICES = {
            0.0F, 0.0F, 0.0F, 0.0F,
            1.0F, 0.0F, 1.0F, 0.0F,
            0.0F, 1.0F, 0.0F, 1.0F,
            1.0F, 1.0F, 1.0F, 1.0F
    };

    private static int gradientProgram = 0;
    private static int gradientUniformTex = -1;
    private static int gradientUniformFadeAlpha = -1;
    private static int gradientUniformColor = -1;
    private static int gradientUniformScreenSize = -1;
    private static int gradientUniformRegion = -1;
    private static boolean gradientFailed = false;

    private static int iconProgram = 0;
    private static int iconUniformTex = -1;
    private static int iconUniformFadeAlpha = -1;
    private static int iconUniformScreenSize = -1;
    private static int iconUniformQuadPosition = -1;
    private static int iconUniformQuadSize = -1;
    private static int iconUniformUvRect = -1;
    private static boolean iconFailed = false;

    private static int quadVao = 0;

    private static int gradientCaptureTexture = 0;
    private static int gradientCaptureWidth = -1;
    private static int gradientCaptureHeight = -1;

    private static ResourceLocation iconTexture;
    private static List<LoadingIcon> cachedLoadedIcons;

    private KhaosLoadingGradient() {}

    public static float computeGradientAlpha(long elapsedMillis, long fadeOutStartMillis, long fadeInStartMillis, boolean fadeInActive) {
        if (UniversalModLoaderHelper.isProductionEnvironment()) return 0.0F;
        float fadeInAlpha = Mth.clamp((float) elapsedMillis / GRADIENT_FADE_IN_MILLIS, 0.0F, 1.0F);
        return Math.min(fadeInAlpha, computeFadeAlpha(fadeOutStartMillis, fadeInStartMillis, fadeInActive));
    }

    private static float computeFadeAlpha(long fadeOutStartMillis, long fadeInStartMillis, boolean fadeInActive) {
        long now = Util.getMillis();
        float fadeOutProgress = fadeOutStartMillis > -1L ? (float) (now - fadeOutStartMillis) / 1000.0F : -1.0F;
        if (fadeOutProgress >= 1.0F) return 1.0F - Mth.clamp(fadeOutProgress - 1.0F, 0.0F, 1.0F);
        if (fadeInActive) {
            float fadeInProgress = fadeInStartMillis > -1L ? (float) (now - fadeInStartMillis) / 500.0F : -1.0F;
            return Mth.clamp(fadeInProgress, 0.0F, 1.0F);
        }
        return 1.0F;
    }

    private static float computeIconPulseAlpha(int index, int count, float fadeAlpha) {
        double fullCycle = ICON_PULSE_PERIOD_MILLIS;

        if (count <= 1) {
            double phase = ((double) Util.getMillis() / fullCycle) * Math.PI * 2.0;
            double pulse = 0.75 + 0.25 * Math.sin(phase);
            return fadeAlpha * (float) pulse;
        }

        double halfCycle = fullCycle / 2.0;
        double staggerMillis = halfCycle / count;
        double timeInCycle = Util.getMillis() % fullCycle;
        double iconStart = index * staggerMillis;
        double iconEnd = iconStart + staggerMillis;

        float progress;
        if (timeInCycle < halfCycle) {
            if (timeInCycle < iconStart) progress = 1.0f;
            else if (timeInCycle >= iconEnd) progress = 0.0f;
            else progress = 1.0f - (float) ((timeInCycle - iconStart) / staggerMillis);
        } else {
            double timeInPhase = timeInCycle - halfCycle;
            if (timeInPhase < iconStart) progress = 0.0f;
            else if (timeInPhase >= iconEnd) progress = 1.0f;
            else progress = (float) ((timeInPhase - iconStart) / staggerMillis);
        }

        float pulse = 0.5f + 0.5f * progress;
        return fadeAlpha * pulse;
    }

    public static void drawGui(GuiGraphics guiGraphics, float fadeAlpha) {
        if (UniversalModLoaderHelper.isProductionEnvironment()) return;
        int alphaBits = Mth.ceil(fadeAlpha * 255.0F) << 24;
        int screenHeight = guiGraphics.guiHeight();
        int gradientTop = screenHeight - screenHeight * GRADIENT_HEIGHT_NUMERATOR / GRADIENT_HEIGHT_DENOMINATOR;
        guiGraphics.fillGradient(0, gradientTop, guiGraphics.guiWidth(), screenHeight, GRADIENT_COLOR, alphaBits | GRADIENT_COLOR);
    }

    public static void drawFramebuffer(int framebufferWidth, int framebufferHeight, float fadeAlpha) {
        if (UniversalModLoaderHelper.isProductionEnvironment()) return;
        if (fadeAlpha <= 0.0F) return;
        if (!ensureGradientProgram()) return;

        int gradientHeight = framebufferHeight * GRADIENT_HEIGHT_NUMERATOR / GRADIENT_HEIGHT_DENOMINATOR;
        if (gradientHeight <= 0 || framebufferWidth <= 0) return;

        ensureQuadGeometry();
        ensureGradientCaptureTexture(framebufferWidth, gradientHeight);

        GlState previous = GlState.capture();

        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, gradientCaptureTexture);
        GL30C.glCopyTexSubImage2D(GL30C.GL_TEXTURE_2D, 0, 0, 0, 0, 0, framebufferWidth, gradientHeight);

        GL30C.glDisable(GL30C.GL_DEPTH_TEST);
        GL30C.glDisable(GL30C.GL_BLEND);
        GL30C.glViewport(0, 0, framebufferWidth, framebufferHeight);
        GL30C.glUseProgram(gradientProgram);
        GL30C.glUniform1i(gradientUniformTex, 0);
        GL30C.glUniform1f(gradientUniformFadeAlpha, fadeAlpha);
        GL30C.glUniform3f(gradientUniformColor, GRADIENT_COLOR_R, GRADIENT_COLOR_G, GRADIENT_COLOR_B);
        GL30C.glUniform2f(gradientUniformScreenSize, (float) framebufferWidth, (float) framebufferHeight);
        GL30C.glUniform4f(gradientUniformRegion, 0.0F, 0.0F, (float) framebufferWidth, (float) gradientHeight);
        GL30C.glActiveTexture(GL30C.GL_TEXTURE0);
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, gradientCaptureTexture);
        GL30C.glBindVertexArray(quadVao);
        GL30C.glDrawArrays(GL30C.GL_TRIANGLE_STRIP, 0, 4);

        previous.restore();
    }

    public static void drawGuiIcons(GuiGraphics guiGraphics, float fadeAlpha) {
        if (UniversalModLoaderHelper.isProductionEnvironment()) return;
        List<LoadingIcon> icons = getLoadedIcons();
        if (icons.isEmpty() || fadeAlpha <= 0.0F) return;

        int guiWidth = guiGraphics.guiWidth();
        int guiHeight = guiGraphics.guiHeight();

        int count = icons.size();
        int totalWidth = count * ICON_SIZE + (count - 1) * ICON_SPACING;
        int startX = (guiWidth - totalWidth) / 2;
        int y = guiHeight - 6 - ICON_SIZE;

        ResourceLocation texture = getIconTexture();
        boolean blendWasEnabled = GL30C.glIsEnabled(GL30C.GL_BLEND);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int i = 0; i < count; i++) {
            LoadingIcon icon = icons.get(i);
            int x = startX + i * (ICON_SIZE + ICON_SPACING);
            float iconAlpha = computeIconPulseAlpha(i, count, fadeAlpha);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, iconAlpha);
            guiGraphics.blit(texture, x, y, ICON_SIZE, ICON_SIZE, icon.u, icon.v, 16, 16, 32, 32);
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (!blendWasEnabled) RenderSystem.disableBlend();
    }

    public static void drawFramebufferIcons(int framebufferWidth, int framebufferHeight, float fadeAlpha) {
        if (UniversalModLoaderHelper.isProductionEnvironment()) return;
        List<LoadingIcon> icons = getLoadedIcons();
        if (icons.isEmpty() || fadeAlpha <= 0.0F) return;
        if (!ensureIconProgram()) return;

        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(getIconTexture());
        int textureId = texture.getId();
        if (textureId == 0) return;

        float guiScale = (float) framebufferWidth / Minecraft.getInstance().getWindow().getGuiScaledWidth();
        float iconSize = ICON_SIZE * guiScale;
        float iconSpacing = ICON_SPACING * guiScale;
        float totalWidth = icons.size() * iconSize + (icons.size() - 1) * iconSpacing;
        float startX = (framebufferWidth - totalWidth) / 2.0F;
        float y = 44 * guiScale;

        ensureQuadGeometry();

        GlState previous = GlState.capture();
        GL30C.glDisable(GL30C.GL_DEPTH_TEST);
        GL30C.glEnable(GL30C.GL_BLEND);
        GL30C.glBlendFunc(GL30C.GL_SRC_ALPHA, GL30C.GL_ONE_MINUS_SRC_ALPHA);
        GL30C.glViewport(0, 0, framebufferWidth, framebufferHeight);

        GL30C.glUseProgram(iconProgram);
        GL30C.glUniform2f(iconUniformScreenSize, (float) framebufferWidth, (float) framebufferHeight);
        GL30C.glActiveTexture(GL30C.GL_TEXTURE0);
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, textureId);
        GL30C.glUniform1i(iconUniformTex, 0);
        GL30C.glBindVertexArray(quadVao);

        float textureSize = 32;
        for (int i = 0; i < icons.size(); i++) {
            LoadingIcon icon = icons.get(i);
            float x = startX + i * (iconSize + iconSpacing);
            float iconAlpha = computeIconPulseAlpha(i, icons.size(), fadeAlpha);
            GL30C.glUniform1f(iconUniformFadeAlpha, iconAlpha);
            GL30C.glUniform2f(iconUniformQuadPosition, x, y);
            GL30C.glUniform2f(iconUniformQuadSize, iconSize, iconSize);
            GL30C.glUniform4f(iconUniformUvRect,
                    icon.u / textureSize,
                    (icon.v + 16) / textureSize,
                    (icon.u + 16) / textureSize,
                    icon.v / textureSize);
            GL30C.glDrawArrays(GL30C.GL_TRIANGLE_STRIP, 0, 4);
        }

        previous.restore();
    }

    private static List<LoadingIcon> getLoadedIcons() {
        if (cachedLoadedIcons == null) {
            List<LoadingIcon> icons = new ArrayList<>(4);
            if (UniversalModLoaderHelper.isModLoaded("elysium_api")) icons.add(LoadingIcon.ELYSIUM);
            if (UniversalModLoaderHelper.isModLoaded("asphodel_gradle")) icons.add(LoadingIcon.ASPHODEL);
            if (UniversalModLoaderHelper.isModLoaded("tartarus_scripting")) icons.add(LoadingIcon.TARTARUS);
            if (UniversalModLoaderHelper.isModLoaded("erebus_modmaker")) icons.add(LoadingIcon.EREBUS);
            cachedLoadedIcons = icons;
        }
        return cachedLoadedIcons;
    }

    private static ResourceLocation getIconTexture() {
        if (iconTexture == null) iconTexture = ElysiumAPI.elysiumPath("textures/gui/loading_icons.png");
        return iconTexture;
    }

    public enum LoadingIcon {
        ELYSIUM(0.0F, 0.0F),
        ASPHODEL(16.0F, 0.0F),
        TARTARUS(0.0F, 16.0F),
        EREBUS(16.0F, 16.0F);

        final float u;
        final float v;

        LoadingIcon(float u, float v) {
            this.u = u;
            this.v = v;
        }
    }

    private static boolean ensureGradientProgram() {
        if (gradientProgram != 0) return true;
        if (gradientFailed) return false;

        int vs = compileShader(GL30C.GL_VERTEX_SHADER, GRADIENT_VERTEX_SHADER);
        if (vs == 0) {
            gradientFailed = true;
            return false;
        }
        int fs = compileShader(GL30C.GL_FRAGMENT_SHADER, GRADIENT_FRAGMENT_SHADER);
        if (fs == 0) {
            GL30C.glDeleteShader(vs);
            gradientFailed = true;
            return false;
        }
        int program = linkProgram(vs, fs);
        GL30C.glDeleteShader(vs);
        GL30C.glDeleteShader(fs);
        if (program == 0) {
            gradientFailed = true;
            return false;
        }
        gradientProgram = program;
        gradientUniformTex = GL30C.glGetUniformLocation(program, "tex");
        gradientUniformFadeAlpha = GL30C.glGetUniformLocation(program, "fadeAlpha");
        gradientUniformColor = GL30C.glGetUniformLocation(program, "gradientColor");
        gradientUniformScreenSize = GL30C.glGetUniformLocation(program, "screenSize");
        gradientUniformRegion = GL30C.glGetUniformLocation(program, "region");
        return true;
    }

    private static boolean ensureIconProgram() {
        if (iconProgram != 0) return true;
        if (iconFailed) return false;

        int vs = compileShader(GL30C.GL_VERTEX_SHADER, ICON_VERTEX_SHADER);
        if (vs == 0) {
            iconFailed = true;
            return false;
        }
        int fs = compileShader(GL30C.GL_FRAGMENT_SHADER, ICON_FRAGMENT_SHADER);
        if (fs == 0) {
            GL30C.glDeleteShader(vs);
            iconFailed = true;
            return false;
        }
        int program = linkProgram(vs, fs);
        GL30C.glDeleteShader(vs);
        GL30C.glDeleteShader(fs);
        if (program == 0) {
            iconFailed = true;
            return false;
        }
        iconProgram = program;
        iconUniformTex = GL30C.glGetUniformLocation(program, "tex");
        iconUniformFadeAlpha = GL30C.glGetUniformLocation(program, "fadeAlpha");
        iconUniformScreenSize = GL30C.glGetUniformLocation(program, "screenSize");
        iconUniformQuadPosition = GL30C.glGetUniformLocation(program, "quadPosition");
        iconUniformQuadSize = GL30C.glGetUniformLocation(program, "quadSize");
        iconUniformUvRect = GL30C.glGetUniformLocation(program, "uvRect");
        return true;
    }

    private static int compileShader(int type, String source) {
        int shader = GL30C.glCreateShader(type);
        GL30C.glShaderSource(shader, source);
        GL30C.glCompileShader(shader);
        if (GL30C.glGetShaderi(shader, GL30C.GL_COMPILE_STATUS) == 0) {
            GL30C.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static int linkProgram(int vertexShader, int fragmentShader) {
        int program = GL30C.glCreateProgram();
        GL30C.glBindAttribLocation(program, 0, "position");
        GL30C.glBindAttribLocation(program, 1, "tex");
        GL30C.glAttachShader(program, vertexShader);
        GL30C.glAttachShader(program, fragmentShader);
        GL30C.glLinkProgram(program);
        if (GL30C.glGetProgrami(program, GL30C.GL_LINK_STATUS) == 0) {
            GL30C.glDeleteProgram(program);
            return 0;
        }
        return program;
    }

    private static void ensureQuadGeometry() {
        if (quadVao != 0) return;

        quadVao = GL30C.glGenVertexArrays();
        int quadVbo = GL30C.glGenBuffers();

        GL30C.glBindVertexArray(quadVao);
        GL30C.glBindBuffer(GL30C.GL_ARRAY_BUFFER, quadVbo);
        GL30C.glBufferData(GL30C.GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(QUAD_VERTICES.length).put(QUAD_VERTICES).flip(), GL30C.GL_STATIC_DRAW);

        GL30C.glEnableVertexAttribArray(0);
        GL30C.glVertexAttribPointer(0, 2, GL30C.GL_FLOAT, false, 16, 0L);
        GL30C.glEnableVertexAttribArray(1);
        GL30C.glVertexAttribPointer(1, 2, GL30C.GL_FLOAT, false, 16, 8L);

        GL30C.glBindVertexArray(0);
        GL30C.glBindBuffer(GL30C.GL_ARRAY_BUFFER, 0);
    }

    private static void ensureGradientCaptureTexture(int width, int height) {
        if (gradientCaptureTexture == 0) gradientCaptureTexture = GL30C.glGenTextures();
        if (gradientCaptureWidth == width && gradientCaptureHeight == height) return;

        int previousTexture = GL30C.glGetInteger(GL30C.GL_TEXTURE_BINDING_2D);
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, gradientCaptureTexture);
        GL30C.glTexImage2D(GL30C.GL_TEXTURE_2D, 0, GL30C.GL_RGBA8, width, height, 0, GL30C.GL_RGBA, GL30C.GL_UNSIGNED_BYTE, 0L);
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MIN_FILTER, GL30C.GL_NEAREST);
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MAG_FILTER, GL30C.GL_NEAREST);
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_S, GL30C.GL_CLAMP_TO_EDGE);
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_T, GL30C.GL_CLAMP_TO_EDGE);
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, previousTexture);
        gradientCaptureWidth = width;
        gradientCaptureHeight = height;
    }

    private record GlState(int program, int texture, int vao, int activeTexture, int[] viewport, boolean depthTest, boolean blend) {

        static GlState capture() {
            int[] viewport = new int[4];
            GL30C.glGetIntegerv(GL30C.GL_VIEWPORT, viewport);
            return new GlState(
                    GL30C.glGetInteger(GL30C.GL_CURRENT_PROGRAM),
                    GL30C.glGetInteger(GL30C.GL_TEXTURE_BINDING_2D),
                    GL30C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING),
                    GL30C.glGetInteger(GL30C.GL_ACTIVE_TEXTURE),
                    viewport,
                    GL30C.glIsEnabled(GL30C.GL_DEPTH_TEST),
                    GL30C.glIsEnabled(GL30C.GL_BLEND)
            );
        }

        void restore() {
            GL30C.glActiveTexture(activeTexture);
            GL30C.glUseProgram(program);
            GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, texture);
            GL30C.glBindVertexArray(vao);
            GL30C.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            setCapability(GL30C.GL_DEPTH_TEST, depthTest);
            setCapability(GL30C.GL_BLEND, blend);
        }

        private static void setCapability(int capability, boolean enabled) {
            if (enabled) GL30C.glEnable(capability);
            else GL30C.glDisable(capability);
        }
    }
}