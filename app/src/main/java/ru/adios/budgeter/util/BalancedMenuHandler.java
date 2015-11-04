package ru.adios.budgeter.util;

import android.app.Activity;
import android.content.res.Resources;
import android.view.Menu;

import org.joda.money.Money;

import java8.util.function.Consumer;

/**
 * Created by Michail Kulikov
 * 10/14/15
 */
public final class BalancedMenuHandler {

    private Money tbSnap;
    private Consumer<BalancesUiThreadState.Pair> balancesListener;

    private final Resources resources;

    private final Consumer<BalancesUiThreadState.Pair> innerListener;

    public BalancedMenuHandler(Resources resources, Consumer<BalancesUiThreadState.Pair> innerListener) {
        this.innerListener = innerListener;
        this.resources = resources;
    }

    public BalancedMenuHandler(Resources resources) {
        this(resources, null);
    }

    public void init(final Activity activity) {
        balancesListener = new Consumer<BalancesUiThreadState.Pair>() {
            @Override
            public void accept(BalancesUiThreadState.Pair pair) {
                if (innerListener != null) {
                    innerListener.accept(pair);
                }

                tbSnap = pair.totalBalance;
                activity.invalidateOptionsMenu();
            }
        };
        BalancesUiThreadState.registerListener(balancesListener);
    }

    public void onCreateMenu(Menu menu) {
        if (tbSnap == null) {
            tbSnap = BalancesUiThreadState.totalBalance;
        }
        UiUtils.fillStandardMenu(menu, tbSnap, resources);
    }

    public void destroy() {
        BalancesUiThreadState.removeListener(balancesListener);
    }

}
