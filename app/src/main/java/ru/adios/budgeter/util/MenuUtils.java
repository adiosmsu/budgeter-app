package ru.adios.budgeter.util;

import android.view.MenuItem;

import org.joda.money.Money;

/**
 * Created by Michail Kulikov
 * 9/29/15
 */
public final class MenuUtils {

    public static final int FUNDS_ID = ElementsIdProvider.getNextId();

    public static void fillStandardMenu(android.view.Menu menu, Money totalBalance) {
        // Add funds info
        final int settingsOrder = menu.getItem(0).getOrder();
        final MenuItem fundsInfo = menu.add(android.view.Menu.NONE, FUNDS_ID, settingsOrder - 1, Formatting.toStringMoneyUsingText(totalBalance));
        fundsInfo.setTitle(Formatting.toStringMoneyUsingText(totalBalance));
        fundsInfo.setTitleCondensed(Formatting.toStringMoneyUsingSign(totalBalance));
        fundsInfo.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }


    private MenuUtils() {}

}
