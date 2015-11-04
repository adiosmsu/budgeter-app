package ru.adios.budgeter.util;

import android.content.res.Resources;

import org.joda.money.Money;

import java8.util.Optional;

/**
 * Created by Michail Kulikov
 * 9/25/15
 */
public final class Formatting {

    public static String toStringMoneyUsingText(Money money) {
        return money.toString();
    }

    public static String toStringMoneyUsingSign(Money money, Resources resources) {
        final String formatted;
        final String amountStr = money.getAmount().toPlainString();

        final Optional<CurrencySymbols.SymInfo> infoRef = CurrencySymbols.getSymbolicInfo(money.getCurrencyUnit(), resources);

        if (infoRef.isPresent()) {
            final CurrencySymbols.SymInfo symInfo = infoRef.get();
            final StringBuilder sb = new StringBuilder(amountStr.length() + symInfo.sym.length() + 1);
            if (symInfo.afterAmount) {
                sb.append(amountStr).append(symInfo.sym);
            } else {
                sb.append(symInfo.sym).append(amountStr);
            }
            formatted = sb.toString();
        } else {
            formatted = amountStr;
        }

        return formatted;
    }

    private Formatting() {}

}
