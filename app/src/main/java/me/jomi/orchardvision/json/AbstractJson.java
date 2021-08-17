package me.jomi.orchardvision.json;

import me.jomi.orchardvision.Func;

abstract class AbstractJson<K, T> {
    interface Callable<K, V> {
        V call(K key) throws Exception;
    }
    static <K, V> V get(K key, Callable<K, V> callable) {
        try {
            return callable.call(key);
        } catch (Exception e) {
            throw Func.throwEx(e);
        }
    }


    final T json;

    AbstractJson(T json) {
        this.json = json;
    }

    public abstract int length();

    public abstract int       getInt    (K key);
    public abstract long      getLong   (K key);
    public abstract double    getDouble (K key);
    public abstract boolean   getBoolean(K key);
    public abstract String    getString (K key);
    public abstract Json      getJson   (K key);
    public abstract JsonArray getArray  (K key);
}
