package ru.adios.budgeter.util;

import org.joda.money.Money;

import javax.annotation.Nonnull;

import java8.util.Optional;
import ru.adios.budgeter.api.data.BalanceAccount;

/**
 * Created by Michail Kulikov
 * 10/13/15
 */
public final class BalanceAccountContainer extends CachingHintedContainer<BalanceAccount> {

    public static final Factory FACTORY = new Factory();

    public BalanceAccountContainer(BalanceAccount account) {
        super(account);
    }

    @Nonnull
    protected String calculateToString() {
        final BalanceAccount account = getObject();
        final Optional<Money> balance = account.getBalance();
        return account.name
                + " ("
                + (balance.isPresent() ? Formatting.toStringMoneyUsingSign(balance.get()) : account.getUnit().toString())
                + ')';
    }

    public static final class Factory implements HintedArrayAdapter.ContainerFactory<BalanceAccount> {

        @Override
        public HintedArrayAdapter.ObjectContainer<BalanceAccount> create(BalanceAccount account) {
            return new BalanceAccountContainer(account);
        }

    }

}
