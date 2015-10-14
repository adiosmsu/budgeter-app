package ru.adios.budgeter.util;

import org.joda.money.Money;

/**
 * Created by Michail Kulikov
 * 9/25/15
 */
public final class Formatting {

    public static String toStringMoneyUsingText(Money money) {
        return money.toString();
    }

    public static String toStringMoneyUsingSign(Money money) {
        return money.getAmount().toPlainString();
    }

    private Formatting() {}

}
