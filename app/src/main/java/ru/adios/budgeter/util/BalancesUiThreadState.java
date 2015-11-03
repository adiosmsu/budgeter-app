package ru.adios.budgeter.util;

import android.content.Context;
import android.os.AsyncTask;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import java8.util.function.Consumer;
import java8.util.stream.Collectors;
import ru.adios.budgeter.BalanceElementCore;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.Constants;
import ru.adios.budgeter.api.Units;

/**
 * Designed for UI thread.
 *
 * Created by Michail Kulikov
 * 10/14/15
 */
@NotThreadSafe
public final class BalancesUiThreadState {

    private static final ArrayList<Money> balances = new ArrayList<>(10);
    public static Money totalBalance = Money.zero(Units.RUB);
    private static final BalanceElementCore balanceElement =
            new BalanceElementCore(BundleProvider.getBundle().treasury(), Constants.CURRENCIES_EXCHANGE_SERVICE.getExchangeService()); // with service for background thread only

    static {
        balanceElement.setTotalUnit(Units.RUB);
    }

    public static final class Pair {
        public final ArrayList<Money> balances;
        public final Money totalBalance;

        public Pair(ArrayList<Money> balances, Money totalBalance) {
            this.balances = balances;
            this.totalBalance = totalBalance;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(BalancesUiThreadState.class);

    private static final HashSet<Consumer<Pair>> listenersSet = new HashSet<>();

    public static Pair getSnapshot() {
        return new Pair(balances, totalBalance);
    }

    public static void registerListener(Consumer<Pair> listener) {
        listenersSet.add(listener);
    }

    public static void removeListener(Consumer<Pair> listener) {
        listenersSet.remove(listener);
    }

    public static void setTotalUnit(CurrencyUnit unit) {
        balanceElement.setTotalUnit(unit);
    }

    public static void addMoney(final Money money, final Context context) {
        new AsyncTask<Pair, Void, Pair>() {
            @Override
            protected Pair doInBackground(Pair[] params) {
                final Pair our = params[0];

                boolean intact = true;
                for (int i = 0; i < our.balances.size(); i++) {
                    final Money bal = our.balances.get(i);
                    if (bal.getCurrencyUnit().equals(money.getCurrencyUnit())) {
                        our.balances.remove(i);
                        our.balances.add(i, money.plus(bal));
                        intact = false;
                        break;
                    }
                }
                if (intact) {
                    our.balances.add(money);
                }

                final Money nextTotal = CoreUtils.addToTotalBalance(our.totalBalance, money, context, false);

                return new Pair(our.balances, nextTotal);
            }

            @Override
            protected void onPostExecute(Pair pair) {
                balances.clear();
                balances.addAll(pair.balances);
                totalBalance = pair.totalBalance;
                notifyListeners();
            }
        }.execute(new Pair(new ArrayList<>(balances), totalBalance));
    }

    public static void instantiate() {
        new AsyncTask<BalanceElementCore, Void, Pair>() {
            @Override
            protected Pair doInBackground(BalanceElementCore[] params) {
                final BalanceElementCore balanceElement = params[0];
                final List<Money> collect = balanceElement.streamIndividualBalances().collect(Collectors.<Money>toList());
                final ArrayList<Money> balances = collect instanceof ArrayList
                        ? (ArrayList<Money>) collect
                        : new ArrayList<>(collect);
                final Money totalBalance = CoreUtils.getTotalBalance(balanceElement, logger);
                return new Pair(balances, totalBalance);
            }

            @Override
            protected void onPostExecute(Pair balancesState) {
                balances.clear();
                balances.addAll(balancesState.balances);
                totalBalance = balancesState.totalBalance;
                notifyListeners();
            }
        }.execute(balanceElement);
    }

    private static void notifyListeners() {
        final Pair pair = new Pair(balances, totalBalance);
        for (final Consumer<Pair> listener : listenersSet) {
            listener.accept(pair);
        }
    }

    private BalancesUiThreadState() {}

}
