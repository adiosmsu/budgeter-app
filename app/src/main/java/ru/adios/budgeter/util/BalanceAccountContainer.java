package ru.adios.budgeter.util;

import org.joda.money.Money;

import javax.annotation.Nonnull;

import ru.adios.budgeter.api.Treasury;

/**
 * Created by Michail Kulikov
 * 10/13/15
 */
public final class BalanceAccountContainer extends CachingHintedContainer<Treasury.BalanceAccount> {

    public static final Factory FACTORY = new Factory();

    public BalanceAccountContainer(Treasury.BalanceAccount account) {
        super(account);
    }

    @Nonnull
    protected String calculateToString() {
        final Treasury.BalanceAccount account = getObject();
        final Money balance = account.getBalance();
        return account.name
                + " ("
                + (balance != null ? Formatting.toStringMoneyUsingSign(balance) : account.getUnit().toString())
                + ')';
    }

    public static final class Factory implements HintedArrayAdapter.ContainerFactory<Treasury.BalanceAccount> {

        @Override
        public HintedArrayAdapter.ObjectContainer<Treasury.BalanceAccount> create(Treasury.BalanceAccount account) {
            return new BalanceAccountContainer(account);
        }

    }

}
