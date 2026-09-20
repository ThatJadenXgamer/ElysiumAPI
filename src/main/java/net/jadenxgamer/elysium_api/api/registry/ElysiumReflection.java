package net.jadenxgamer.elysium_api.api.registry;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
public final class ElysiumReflection {
    private static final Map<String, ConstructorInvoker> CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Constructor<?>[]> CONSTRUCTORS_CACHE = new ConcurrentHashMap<>();

    /**
     * Creates a builder for dynamically instantiating a {@link Block} subclass using reflection.
     * <p>
     * This method starts a builder pattern that allows you to specify the fully qualified class name
     * and constructor arguments. Reflection is used to find and invoke the appropriate constructor,
     * which is particularly useful when you need to register blocks from different mods without direct
     * compile-time dependencies.
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * Block lesionBlock = ElysiumReflection.createBlock()
     *         .className("net.jadenxgamer.netherexp.core.block.LesionBlock")
     *         .constructorValues(() -> Items.ROTTEN_FLESH, BlockBehaviour.Properties.of().strength(2.0f));
     * }</pre>
     *
     * @param <T> the type of block to create (must extend Block)
     * @return a new ReflectionBuilder instance for building the block
     */
    public static <T extends Block> ReflectionBuilder<T> createBlock() {
        return new ReflectionBuilder<>(Block.class);
    }

    /**
     * Creates a builder for dynamically instantiating an {@link Item} subclass using reflection.
     * <p>
     * This method starts a builder pattern that allows you to specify the fully qualified class name
     * and constructor arguments. See {@link #createBlock()} for a usage example and explanation
     * of the reflection purpose.
     *
     * @param <T> the type of item to create (must extend Item)
     * @return a new ReflectionBuilder instance for building the item
     */
    public static <T extends Item> ReflectionBuilder<T> createItem() {
        return new ReflectionBuilder<>(Item.class);
    }

    /**
     * Creates a builder for dynamically instantiating a {@link MobEffect} subclass using reflection.
     * <p>
     * This method starts a builder pattern that allows you to specify the fully qualified class name
     * and constructor arguments. See {@link #createBlock()} for a usage example and explanation
     * of the reflection purpose.
     *
     * @param <T> the type of mob effect to create (must extend MobEffect)
     * @return a new ReflectionBuilder instance for building the mob effect
     */
    public static <T extends MobEffect> ReflectionBuilder<T> createMobEffect() {
        return new ReflectionBuilder<>(MobEffect.class);
    }

    /**
     * @deprecated Use {@link #createBlock()} builder pattern instead.
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public static <T extends Block> T createBlock(String className, Object... args) {
        return createInstance(Block.class, className, args);
    }

    /**
     * @deprecated Use {@link #createItem()} builder pattern instead.
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public static <T extends Item> T createItem(String className, Object... args) {
        return createInstance(Item.class, className, args);
    }

    /**
     * @deprecated Use {@link #createMobEffect()} builder pattern instead.
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public static <T extends MobEffect> T createMobEffect(String className, Object... args) {
        return createInstance(MobEffect.class, className, args);
    }

    /**
     * Generic method to create an instance of any class using reflection
     *
     * @param superType the super class type that the target class must extend/implement
     * @param className the fully qualified class name of the class to instantiate
     * @param args the constructor arguments to pass into the reflected class's constructor
     * @return a new instance of the specified class
     * @param <T> the type of object to create
     * @param <S> the super type that T must extend/implement
     */
    private static <T, S> T createInstance(Class<S> superType, String className, Object... args) {
        Class<?>[] providedTypes = buildTypesArray(args);
        String cacheKey = buildCacheKey(className, providedTypes);

        ConstructorInvoker invoker = CACHE.computeIfAbsent(cacheKey, key -> createInvoker(superType, className, providedTypes));

        return returnInstance(className, invoker, args);
    }

    private static Class<?>[] buildTypesArray(Object[] args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] != null ? args[i].getClass() : null;
        }
        return types;
    }

    private static String buildCacheKey(String className, Class<?>[] types) {
        StringBuilder keyBuilder = new StringBuilder(className).append('(');
        for (int i = 0; i < types.length; i++) {
            if (i > 0) keyBuilder.append(',');
            keyBuilder.append(types[i] != null ? types[i].getName() : "null");
        }
        return keyBuilder.append(')').toString();
    }

    private static <S> ConstructorInvoker createInvoker(Class<S> superType, String className, Class<?>[] providedTypes) {
        try {
            Class<? extends S> targetClass = Class.forName(className).asSubclass(superType);
            Constructor<? extends S> constructor = findCompatibleConstructor(targetClass, providedTypes);
            constructor.setAccessible(true);

            try {
                MethodHandle methodHandle = MethodHandles.lookup().unreflectConstructor(constructor);
                return new ConstructorInvoker(methodHandle);
            } catch (IllegalAccessException e) {
                return new ConstructorInvoker(constructor);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(superType.getSimpleName() + " class not found: " + className, e);
        }
    }

    private static <S> Constructor<? extends S> findCompatibleConstructor(Class<? extends S> targetClass, Class<?>[] providedTypes) {
        Constructor<?>[] constructors = CONSTRUCTORS_CACHE.computeIfAbsent(targetClass, Class::getDeclaredConstructors);

        return (Constructor<? extends S>) Arrays.stream(constructors)
                .filter(constructor -> constructor.getParameterCount() == providedTypes.length)
                .min(Comparator.comparingInt(c -> calculateCompatibilityScore(c.getParameterTypes(), providedTypes)))
                .orElseThrow(() -> createConstructorNotFoundException(targetClass, providedTypes, constructors));
    }

    private static <S> RuntimeException createConstructorNotFoundException(Class<? extends S> targetClass, Class<?>[] providedTypes, Constructor<?>[] availableConstructors) {
        StringBuilder errorMessage = new StringBuilder()
                .append("No compatible constructor found for ")
                .append(targetClass.getName())
                .append(" with parameter types: ")
                .append(Arrays.toString(providedTypes))
                .append("\nAvailable constructors:\n");

        for (Constructor<?> constructor : availableConstructors) {
            errorMessage.append("  -")
                    .append(Arrays.toString(constructor.getParameterTypes()))
                    .append("\n");
        }

        return new RuntimeException(errorMessage.toString());
    }

    private static int calculateCompatibilityScore(Class<?>[] paramTypes, Class<?>[] providedTypes) {
        int score = 0;
        for (int i = 0; i < paramTypes.length; i++) {
            int paramScore = getTypeCompatibility(paramTypes[i], providedTypes[i]);
            if (paramScore < 0) return Integer.MAX_VALUE;
            score += paramScore;
        }
        return score;
    }

    private static int getTypeCompatibility(Class<?> expected, Class<?> actual) {
        if (actual == null) return expected.isPrimitive() ? -1 : 0;
        if (expected.equals(actual)) return 0;
        if (expected.isAssignableFrom(actual)) return 2;

        // Handle primitive/boxed type compatibility
        if (PrimitiveTypes.isWrapper(expected, actual)) return 1;

        return -1;
    }

    private static <T> T returnInstance(String className, ConstructorInvoker invoker, Object[] args) {
        try {
            return (T) invoker.invoke(args);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to instantiate class: " + className, t);
        }
    }

    private static final class ConstructorInvoker {
        private final MethodHandle methodHandle;
        private final Constructor<?> constructor;

        ConstructorInvoker(MethodHandle methodHandle) {
            this.methodHandle = methodHandle;
            this.constructor = null;
        }

        ConstructorInvoker(Constructor<?> constructor) {
            this.constructor = constructor;
            this.methodHandle = null;
        }

        Object invoke(Object[] args) throws Throwable {
            if (methodHandle != null) {
                return methodHandle.invokeWithArguments(args);
            }
            return constructor.newInstance(args);
        }
    }

    private static final class PrimitiveTypes {
        private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = Map.of(
                Boolean.class, boolean.class,
                Byte.class, byte.class,
                Character.class, char.class,
                Short.class, short.class,
                Integer.class, int.class,
                Long.class, long.class,
                Float.class, float.class,
                Double.class, double.class
        );

        private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = Map.of(
                boolean.class, Boolean.class,
                byte.class, Byte.class,
                char.class, Character.class,
                short.class, Short.class,
                int.class, Integer.class,
                long.class, Long.class,
                float.class, Float.class,
                double.class, Double.class
        );

        static boolean isWrapper(Class<?> type1, Class<?> type2) {
            return WRAPPER_TO_PRIMITIVE.get(type1) == type2 || PRIMITIVE_TO_WRAPPER.get(type1) == type2;
        }
    }

    public static class ReflectionBuilder<T> {
        private final Class<?> superType;
        private String targetClassName;

        protected ReflectionBuilder(Class<?> superType) {
            this.superType = superType;
        }

        public ReflectionBuilder<T> className(String className) {
            this.targetClassName = className;
            return this;
        }

        public T constructorValues(Object... args) {
            if (targetClassName == null) {
                throw new IllegalStateException("className must be defined before calling constructorValues!");
            }
            return (T) createInstance(superType, targetClassName, args);
        }
    }
}