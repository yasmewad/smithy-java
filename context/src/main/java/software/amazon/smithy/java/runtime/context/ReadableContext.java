package software.amazon.smithy.java.runtime.context;

import java.util.Objects;

/**
 * A read-only typed context map.
 */
public interface ReadableContext {
    /**
     * Get a property.
     *
     * @param key   property key to get by exact reference identity.
     * @param <T>   Returns the value, or null if not present.
     * @return Returns the nullable property value.
     */
    <T> T getProperty(Constant<T> key);

    /**
     * Get a property or return a default if not found.
     *
     * @param key          property key to get by exact reference identity.
     * @param defaultValue Value to return if the property is null or non-existent.
     * @param <T>          Returns the value, or null if not present.
     * @return Returns the property value.
     */
    default <T> T getProperty(Constant<T> key, T defaultValue) {
        return Objects.requireNonNullElse(getProperty(key), defaultValue);
    }

    /**
     * Get a property and throw if it isn't present.
     *
     * @param key property key to get by exact reference identity.
     * @param <T> Returns the value.
     * @throws IllegalArgumentException if the property isn't found.
     */
    default <T> T expectProperty(Constant<T> key) {
        T value = getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown property: " + key);
        }
        return value;
    }

    /**
     * Get each property of the context object.
     *
     * @param consumer Receives each context key value pair.
     */
    void forEachProperty(PropertyConsumer consumer);

    /**
     * Interface for receiving all {@link Context} entries.
     */
    @FunctionalInterface
    interface PropertyConsumer {
        /**
         * A method to operate on a {@link Context} and it's values.
         *
         * @param key   The context key.
         * @param value The context value.
         * @param <T> The value type.
         */
        <T> void accept(Constant<T> key, T value);
    }
}
