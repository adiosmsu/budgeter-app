package ru.adios.budgeter.util;

import org.joda.money.Money;

import javax.annotation.Nonnull;

import ru.adios.budgeter.api.Treasury;

/**
 * Created by Michail Kulikov
 * 10/13/15
 */
public final class BalanceAccountContainer implements HintedArrayAdapter.ObjectContainer<Treasury.BalanceAccount> {

    private final Treasury.BalanceAccount account;

    private String cache;

    public BalanceAccountContainer(Treasury.BalanceAccount account) {
        this.account = account;
    }

    @Override
    public Treasury.BalanceAccount getObject() {
        return account;
    }

    @Override
    public String toString() {
        if (cache == null) {
            cache = calculateToString();
        }
        return cache;
    }

    @Nonnull
    private String calculateToString() {
        final Money balance = account.getBalance();
        return account.name
                + " ("
                + (balance != null ? Formatting.toStringMoneyUsingSign(balance) : account.getUnit().toString())
                + ')';
    }

}
