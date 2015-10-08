package ru.adios.budgeter.util;

import java.lang.reflect.Array;

/**
 * Created by Michail Kulikov
 * 10/8/15
 */
public final class GeneralUtils {

    public static <T> T[] arrayPlusValue(T[] original, T newValue) {
        final int l = original.length;
        final T[] ts = newArrayInstance(newValue, l + 1);
        System.arraycopy(original, 0, ts, 0, l);
        ts[l] = newValue;
        return ts;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] newArrayInstance(Class<T> arrayClass, int length) {
        if (arrayClass.equals(String.class)) { //expand if needed
            return (T[]) new String[length];
        } else {
            return (T[]) Array.newInstance(arrayClass, length); // heavy JNI call (screw Dalvik!)
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] newArrayInstance(T object, int length) {
        return newArrayInstance((Class<T>) object.getClass(), length);
    }

    private GeneralUtils() {}

}