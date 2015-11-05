package ru.adios.budgeter.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.annotation.IdRes;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import ru.adios.budgeter.Constants;
import ru.adios.budgeter.CoreElementActivity;
import ru.adios.budgeter.R;
import ru.adios.budgeter.api.data.BalanceAccount;

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
    private static final Logger logger = LoggerFactory.getLogger(UiUtils.class);

    public static int dpAsPixels(Context context, int sizeInDp) {
        Resources r = context.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeInDp, r.getDisplayMetrics());
    }

    public static <T> void prepareHintedSpinnerAsync(final Spinner spinner,
                                                     final CoreElementActivity activity,
                                                     final @IdRes int fragmentId,
                                                     final String fieldName,
                                                     final View mainFragmentView,
                                                     final @IdRes int spinnerInfoId,
                                                     Stream<T> stream,
                                                     Function<T, HintedArrayAdapter.ObjectContainer<T>> converter) {
        HintedArrayAdapter.adaptStringSpinner(spinner, activity, new String[] {}); // empty it first 'cause we need to do our task in background
        new AsyncTask<Object, Void, List<HintedArrayAdapter.ObjectContainer<T>>>() {
            @Override
            protected List<HintedArrayAdapter.ObjectContainer<T>> doInBackground(Object[] params) {
                // get values from db
                try {
                    //noinspection unchecked
                    return ((Stream<T>) params[0])
                            .map((Function<T, HintedArrayAdapter.ObjectContainer<T>>) params[1])
                            .collect(Collectors.<HintedArrayAdapter.ObjectContainer<T>>toList());
                } catch (RuntimeException e) {
                    logger.warn("Exception while querying for spinner contents with stream", e);
                    return new ArrayList<>(1);
                }
            }

            @Override
            protected void onPostExecute(List<HintedArrayAdapter.ObjectContainer<T>> res) {
                // fill spinner with data and schedule it for redrawing
                HintedArrayAdapter.adaptArbitraryContainedSpinner(spinner, activity, res);
                activity.addFieldFragmentInfo(fragmentId, fieldName, spinner, mainFragmentView.findViewById(spinnerInfoId));
                spinner.invalidate();
            }
        }.execute(stream, converter);
    }

    public static void refillLinearLayoutWithBalances(LinearLayout fundsLayout, List<Money> balances, Money totalBalance, Context context) {
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

    public static MenuItem fillStandardMenu(Menu menu, Money totalBalance, Resources resources) {
        // Add funds info
        final int settingsOrder = menu.getItem(0).getOrder();
        final String text = Formatting.toStringMoneyUsingText(totalBalance);
        final MenuItem fundsInfo = menu.add(Menu.NONE, FUNDS_ID, settingsOrder - 1, text);
        fundsInfo.setTitle(text);
        fundsInfo.setTitleCondensed(Formatting.toStringMoneyUsingSign(totalBalance, resources));
        fundsInfo.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return fundsInfo;
    }

    public static <T> void addToHintedSpinner(T obj, Spinner hintedSpinner, HintedArrayAdapter.ContainerFactory<T> factory) {
        @SuppressWarnings("unchecked")
        final HintedArrayAdapter<T> adapter = (HintedArrayAdapter<T>) hintedSpinner.getAdapter();

        boolean hintSelected = adapter.getCount() == hintedSpinner.getSelectedItemPosition();
        Boolean backup = null;

        adapter.add(factory.create(obj));

        // repair situation when what selected is a hint and we add something to the end of a real items list hence position will not change and no event will fire
        if (hintedSpinner instanceof FlexibleNotifyingSpinner && hintSelected) {
            final FlexibleNotifyingSpinner as = (FlexibleNotifyingSpinner) hintedSpinner;
            backup = as.willNotifyEvenIfSameSelection();
            as.setNotifyEvenIfSameSelection(true);
        }

        hintedSpinner.setSelection(adapter.getCount() - 1, true);

        // change back
        if (backup != null && backup == Boolean.FALSE) {
            ((FlexibleNotifyingSpinner) hintedSpinner).setNotifyEvenIfSameSelection(false);
        }
    }

    public static void replaceAccountInSpinner(BalanceAccount account, Spinner accountsSpinner, Resources resources) {
        @SuppressWarnings("unchecked")
        final HintedArrayAdapter<BalanceAccount> adapter = (HintedArrayAdapter<BalanceAccount>) accountsSpinner.getAdapter();

        for (int i = 0; i < adapter.getCount(); i++) {
            final HintedArrayAdapter.ObjectContainer<BalanceAccount> item = adapter.getItem(i);
            final BalanceAccount someAccount = item.getObject();

            if (someAccount.name.equals(account.name) && someAccount.getUnit().equals(account.getUnit())) {
                adapter.insert(new BalanceAccountContainer(account, resources), i);
                adapter.remove(item);
                return;
            }
        }
    }

    public static void makeButtonSquaredByHeight(final Button button) {
        button.post(new Runnable() {
            @Override
            public void run() {
                final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) button.getLayoutParams();
                params.width = button.getHeight();
                button.setLayoutParams(params);
                button.invalidate();
            }
        });
    }

    private UiUtils() {}

}
