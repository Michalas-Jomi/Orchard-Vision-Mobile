package me.jomi.orchardvision.interfaces;

public interface ConsumerUnSafe<T> {
    void accept(T t) throws Exception;
}
