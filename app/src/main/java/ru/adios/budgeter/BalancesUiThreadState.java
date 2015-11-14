/*
 *
 *  *
 *  *  * Copyright 2015 Michael Kulikov
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package ru.adios.budgeter;

import android.content.Context;
import android.support.annotation.UiThread;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

import java8.util.function.Consumer;
import java8.util.stream.Collectors;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.core.CoreUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Designed for UI thread.
 *
 * Created by Michail Kulikov
 * 10/14/15
 */
@UiThread
@ThreadSafe
public final class BalancesUiThreadState {

    private static final ArrayList<Money> innerBalances = new ArrayList<>(10);
    public static Money innerTotalBalance = Money.zero(Units.RUB);
    private static CurrencyUnit totalUnit = Units.RUB;
    private static boolean initialized = false;

    public static final class Pair {
        public final ArrayList<Money> balances;
        public final Money totalBalance;

        public Pair(ArrayList<Money> balances, Money totalBalance) {
            this.balances = balances;
            this.totalBalance = totalBalance;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Pair pair = (Pair) o;
            return balances.equals(pair.balances)
                    && totalBalance.equals(pair.totalBalance);
        }

        @Override
        public int hashCode() {
            return 31 * balances.hashCode() + totalBalance.hashCode();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(BalancesUiThreadState.class);
    private static final HashSet<Consumer<Pair>> listenersSet = new HashSet<>();


    public static Pair getSnapshot() {
        return new Pair(innerBalances, innerTotalBalance);
    }

    public static void registerListener(Consumer<Pair> listener) {
        listenersSet.add(listener);
    }

    public static void removeListener(Consumer<Pair> listener) {
        listenersSet.remove(listener);
    }

    public static void setTotalUnit(CurrencyUnit unit) {
        checkNotNull(unit, "unit is null");
        totalUnit = unit;
    }

    public static void addMoney(final Money money, final Context context) {
        if (!initialized) {
            instantiate(context);
        }

        boolean intact = true;
        for (int i = 0; i < innerBalances.size(); i++) {
            final Money bal = innerBalances.get(i);
            if (bal.getCurrencyUnit().equals(money.getCurrencyUnit())) {
                innerBalances.remove(i);
                innerBalances.add(i, money.plus(bal));
                intact = false;
                break;
            }
        }
        if (intact) {
            innerBalances.add(money);
        }

        innerTotalBalance = CoreUtils.addToTotalBalance(innerTotalBalance, money, context, true);

        notifyListeners();
    }

    public static void instantiate(Context application) {
        initialized = false;
        try {
            final BalanceElementCore balanceElement =
                    new BalanceElementCore(BundleProvider.getBundle().treasury(), Constants.CURRENCIES_EXCHANGE_SERVICE);
            balanceElement.setTotalUnit(totalUnit);

            final List<Money> collect = balanceElement.streamIndividualBalances().collect(Collectors.<Money>toList());
            final ArrayList<Money> balances = collect instanceof ArrayList
                    ? (ArrayList<Money>) collect
                    : new ArrayList<>(collect);

            innerBalances.clear();
            innerBalances.addAll(balances);
            innerTotalBalance = CoreUtils.getTotalBalance(balanceElement, application, logger);

            initialized = true;

            notifyListeners();
        } catch (RuntimeException ex) {
            logger.warn("Exception while instantiating balances state", ex);
            innerBalances.clear();
            innerTotalBalance = Money.zero(totalUnit);
        }
    }

    private static void notifyListeners() {
        final Pair pair = new Pair(innerBalances, innerTotalBalance);
        for (final Consumer<Pair> listener : listenersSet) {
            listener.accept(pair);
        }
    }

    private BalancesUiThreadState() {}

}
