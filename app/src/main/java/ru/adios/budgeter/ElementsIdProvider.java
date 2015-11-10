package ru.adios.budgeter;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Created by Michail Kulikov
 * 9/25/15
 */
@ThreadSafe
public final class ElementsIdProvider {

    private static final AtomicInteger COUNTER = new AtomicInteger(Integer.MAX_VALUE - 1);

    public static int getNextId() {
        return COUNTER.getAndDecrement();
    }

    private ElementsIdProvider() {}

}
