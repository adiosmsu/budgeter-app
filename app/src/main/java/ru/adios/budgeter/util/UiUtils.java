package ru.adios.budgeter.util;

import android.view.MenuItem;
import android.widget.Spinner;

import org.joda.money.Money;

import ru.adios.budgeter.api.Treasury;

/**
 * Created by Michail Kulikov
 * 9/29/15
 */
public final class UiUtils {

    public static final int FUNDS_ID = ElementsIdProvider.getNextId();

    public static void fillStandardMenu(android.view.Menu menu, Money totalBalance) {
        // Add funds info
        final int settingsOrder = menu.getItem(0).getOrder();
        final MenuItem fundsInfo = menu.add(android.view.Menu.NONE, FUNDS_ID, settingsOrder - 1, Formatting.toStringMoneyUsingText(totalBalance));
        fundsInfo.setTitle(Formatting.toStringMoneyUsingText(totalBalance));
        fundsInfo.setTitleCondensed(Formatting.toStringMoneyUsingSign(totalBalance));
        fundsInfo.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    public static void addAccountToSpinner(Treasury.BalanceAccount account, Spinner accountsSpinner) {
        @SuppressWarnings("unchecked")
        final HintedArrayAdapter<Treasury.BalanceAccount> adapter = (HintedArrayAdapter<Treasury.BalanceAccount>) accountsSpinner.getAdapter();
        adapter.add(new BalanceAccountContainer(account));
        accountsSpinner.setSelection(adapter.getCount(), true);
    }

    public static void replaceAccountInSpinner(Treasury.BalanceAccount account, Spinner accountsSpinner) {
        @SuppressWarnings("unchecked")
        final HintedArrayAdapter<Treasury.BalanceAccount> adapter = (HintedArrayAdapter<Treasury.BalanceAccount>) accountsSpinner.getAdapter();

        for (int i = 0; i < adapter.getCount(); i++) {
            final HintedArrayAdapter.ObjectContainer<Treasury.BalanceAccount> item = adapter.getItem(i);
            final Treasury.BalanceAccount someAccount = item.getObject();

            if (someAccount.name.equals(account.name) && someAccount.getUnit().equals(account.getUnit())) {
                adapter.insert(new BalanceAccountContainer(account), i);
                adapter.remove(item);
                return;
            }
        }
    }

    private UiUtils() {}

}
