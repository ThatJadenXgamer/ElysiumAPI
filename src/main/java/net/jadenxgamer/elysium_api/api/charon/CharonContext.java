package net.jadenxgamer.elysium_api.api.charon;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CharonContext {
    private final Class<?> targetClass;
    private final Object self;
    private final boolean hasReturn;

    private Object returnValue;
    private boolean cancelled;
    private boolean cancelVanilla;

    private static final ClassValue<Map<String, VarHandle>> VAR_HANDLE_CACHE = new ClassValue<>() {
        @Override
        protected Map<String, VarHandle> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    public CharonContext(Class<?> targetClass, Object self, boolean hasReturn, Object initialReturn) {
        this.targetClass = targetClass;
        this.self = self;
        this.hasReturn = hasReturn;
        this.returnValue = initialReturn;
    }

    @SuppressWarnings("unchecked")
    public <T> T getSelf() { return (T) self; }
    public Class<?> getTargetClass() { return targetClass; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public void cancelExecution() { this.cancelVanilla = true; }
    public boolean isVanillaCancelled() { return cancelVanilla; }
    public boolean hasReturn() { return hasReturn; }

    @SuppressWarnings("unchecked")
    public <T> T getReturn() {
        if (!hasReturn) throw new IllegalStateException("This CharonContext does not expose a return value");
        return (T) returnValue;
    }

    public void setReturn(Object value) {
        if (!hasReturn) throw new IllegalStateException("This CharonContext does not expose a return value");
        this.returnValue = value;
        this.cancelVanilla = true;
    }

    @SuppressWarnings("unchecked")
    public <T> T getField(String fieldName) {
        VarHandle handle = getCachedVarHandle(fieldName);
        return (T) (isStaticField(fieldName) ? handle.get() : handle.get(self));
    }

    public void setField(String fieldName, Object value) {
        VarHandle handle = getCachedVarHandle(fieldName);
        if (isStaticField(fieldName)) handle.set(value);
        else handle.set(self, value);
    }

    private VarHandle getCachedVarHandle(String fieldName) {
        return VAR_HANDLE_CACHE.get(targetClass).computeIfAbsent(fieldName, k -> {
            Class<?> current = targetClass;
            while (current != null) {
                try {
                    Field field = current.getDeclaredField(k);
                    field.setAccessible(true);
                    return MethodHandles.privateLookupIn(current, MethodHandles.lookup()).unreflectVarHandle(field);
                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                    current = current.getSuperclass();
                }
            }
            throw new RuntimeException("Field '" + k + "' not found in " + targetClass.getName());
        });
    }

    private boolean isStaticField(String fieldName) {
        Class<?> current = targetClass;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                return Modifier.isStatic(field.getModifiers());
            } catch (NoSuchFieldException ignored) { current = current.getSuperclass(); }
        }
        return false;
    }
}