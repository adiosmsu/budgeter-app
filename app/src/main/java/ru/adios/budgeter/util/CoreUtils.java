package ru.adios.budgeter.util;

import org.joda.money.Money;
import org.slf4j.Logger;

import javax.annotation.Nullable;

import ru.adios.budgeter.BalanceElementCore;
import ru.adios.budgeter.api.BudgeterApiException;
import ru.adios.budgeter.api.Units;

/**
 * Created by Michail Kulikov
 * 9/29/15
 */
public final class CoreUtils {

    public static Money getTotalBalance(BalanceElementCore balanceElement, @Nullable Logger logger) {
        Money totalBalance;
        try {
            totalBalance = balanceElement.getTotalBalance();
        } catch (BudgeterApiException ex) {
            if (logger != null) {
                logger.error("Error fetching total balance", ex);
            }
            totalBalance = Money.zero(Units.RUB);
        }
        return totalBalance;
    }

    private CoreUtils() {}

}
