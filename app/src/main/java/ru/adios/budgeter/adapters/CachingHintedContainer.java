package ru.adios.budgeter.adapters;

import android.support.annotation.UiThread;

/**
 * Created by Michail Kulikov
 * 10/15/15
 */
@UiThread
public abstract class CachingHintedContainer<T> implements HintedArrayAdapter.ObjectContainer<T> {

    private final T obj;

    private String cache;

    public CachingHintedContainer(T obj) {
        this.obj = obj;
    }

    @Override
    public final T getObject() {
        return obj;
    }

    @Override
    public final String toString() {
        if (cache == null) {
            cache = calculateToString();
        }
        return cache;
    }

    protected abstract String calculateToString();

    protected final void invalidateCache() {
        cache = null;
    }

}
