package ru.adios.budgeter.util;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.joda.money.Money;

import java.util.List;

import ru.adios.budgeter.Constants;
import ru.adios.budgeter.R;
import ru.adios.budgeter.api.Treasury;

/**
 * Created by Michail Kulikov
 * 9/29/15
 */
public final class UiUtils {

    public static void onApplicationStart() {
        //noinspection ResultOfMethodCallIgnored
        Constants.MAIN_HANDLER.toString();
    }

    public static final int FUNDS_ID = ElementsIdProvider.getNextId();

    public static void refillLinearLayout(LinearLayout fundsLayout, List<Money> balances, Money totalBalance, Context context) {
        fundsLayout.removeAllViews();

        int maxLine = 0;
        for (final Money money : balances) {
            final String str = '\t' + Formatting.toStringMoneyUsingText(money);

            final int l = str.length();
            if (l > maxLine) {
                maxLine = l;
            }

            fundsLayout.addView(getNewLineView(str, context));
        }

        if (maxLine != 0) {
            fundsLayout.addView(getStrSeparatorLine(maxLine, context));
        }

        fundsLayout.addView(getNewLineView(context.getResources().getText(R.string.total).toString() + ' ' + Formatting.toStringMoneyUsingText(totalBalance), context));

        fundsLayout.invalidate();
    }

    private static TextView getStrSeparatorLine(int ml, Context context) {
        final StringBuilder sb = new StringBuilder(ml + 1);
        for (int i = 0; i < ml; i++) {
            sb.append('_');
        }
        final String s = sb.toString();
        return getNewLineView(s, context);
    }

    private static TextView getNewLineView(String str, Context context) {
        final TextView line = new TextView(context);
        line.setText(str);
        line.setId(ElementsIdProvider.getNextId());
        line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return line;
    }

    public static MenuItem fillStandardMenu(Menu menu, Money totalBalance) {
        // Add funds info
        final int settingsOrder = menu.getItem(0).getOrder();
        final String text = Formatting.toStringMoneyUsingText(totalBalance);
        final MenuItem fundsInfo = menu.add(Menu.NONE, FUNDS_ID, settingsOrder - 1, text);
        fundsInfo.setTitle(text);
        fundsInfo.setTitleCondensed(Formatting.toStringMoneyUsingSign(totalBalance));
        fundsInfo.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return fundsInfo;
    }

    public static void addAccountToSpinner(Treasury.BalanceAccount account, Spinner accountsSpinner) {
        @SuppressWarnings("unchecked")
        final HintedArrayAdapter<Treasury.BalanceAccount> adapter = (HintedArrayAdapter<Treasury.BalanceAccount>) accountsSpinner.getAdapter();

        boolean hintSelected = adapter.getCount() == accountsSpinner.getSelectedItemPosition();
        Boolean backup = null;

        adapter.add(new BalanceAccountContainer(account));

        // repair situation when what selected is a hint and we add something to the end of a real items list hence position will not change and no event will fire
        if (accountsSpinner instanceof FlexibleNotifyingSpinner && hintSelected) {
            final FlexibleNotifyingSpinner as = (FlexibleNotifyingSpinner) accountsSpinner;
            backup = as.willNotifyEvenIfSameSelection();
            as.setNotifyEvenIfSameSelection(true);
        }

        accountsSpinner.setSelection(adapter.getCount() - 1, true);

        // change back
        if (backup != null && backup == Boolean.FALSE) {
            ((FlexibleNotifyingSpinner) accountsSpinner).setNotifyEvenIfSameSelection(false);
        }
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
