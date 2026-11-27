package enviromine_revived.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

import enviromine_revived.model.Doc;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Turns every component of a tuning record into a config entry (comment and range from its
 * {@link Doc} annotation, default from the given defaults) and builds the record back from the
 * config. This keeps the config and the model in step: a new tuning value only needs a
 * {@code @Doc} to become configurable.
 */
final class RecordConfig<R extends Record> {
    private final Constructor<R> constructor;
    private final List<ModConfigSpec.ConfigValue<?>> values = new ArrayList<>();

    RecordConfig(Class<R> type, R defaults, ModConfigSpec.Builder builder) {
        RecordComponent[] components = type.getRecordComponents();
        Class<?>[] parameterTypes = new Class<?>[components.length];
        try {
            for (int i = 0; i < components.length; i++) {
                RecordComponent component = components[i];
                parameterTypes[i] = component.getType();
                Doc doc = component.getAnnotation(Doc.class);
                if (doc == null) {
                    throw new IllegalStateException(type.getSimpleName() + "." + component.getName() + " has no @Doc");
                }
                Object defaultValue = component.getAccessor().invoke(defaults);
                String key = ConfigNames.key(component.getName());
                builder.comment(doc.value());
                if (component.getType() == double.class) {
                    values.add(builder.defineInRange(key, (double) defaultValue, doc.min(), doc.max()));
                } else if (component.getType() == int.class) {
                    values.add(builder.defineInRange(key, (int) defaultValue, (int) doc.min(), (int) doc.max()));
                } else {
                    throw new IllegalStateException("Unsupported config type " + component.getType());
                }
            }
            constructor = type.getDeclaredConstructor(parameterTypes);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot build config for " + type.getName(), e);
        }
    }

    R read() {
        Object[] args = new Object[values.size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = values.get(i).get();
        }
        try {
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read config for " + constructor.getDeclaringClass().getName(), e);
        }
    }
}
