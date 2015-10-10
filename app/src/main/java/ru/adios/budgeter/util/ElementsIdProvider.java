package ru.adios.budgeter.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Michail Kulikov
 * 9/25/15
 */
public final class ElementsIdProvider {

    private static final AtomicInteger COUNTER = new AtomicInteger(Integer.MAX_VALUE - 1);

    public static int getNextId() {
        return COUNTER.getAndDecrement();
    }

    private ElementsIdProvider() {}

}
