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

package ru.adios.budgeter.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import java8.util.Optional;
import java8.util.OptionalInt;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import ru.adios.budgeter.Constants;
import ru.adios.budgeter.ElementsIdProvider;
import ru.adios.budgeter.R;
import ru.adios.budgeter.adapters.MutableAdapter;
import ru.adios.budgeter.adapters.NullableDecoratingAdapter;
import ru.adios.budgeter.adapters.StringPresenter;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.core.CoreElementActivity;
import ru.adios.budgeter.core.CoreNotifier;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by Michail Kulikov
 * 9/29/15
 */
@Immutable
public final class UiUtils {

    public static final int RED_COLOR = 0xffaf0b0b;
    public static final int GREEN_COLOR = 0xff2ace1f;

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

    public static int pixelsAsDp(Context context, int sizeInPixels) {
        return (int) (sizeInPixels / context.getResources().getDisplayMetrics().density);
    }

    public static String resolveResourceId(Context context, int id) {
        return resolveResourceId(context.getResources(), id);
    }

    public static String resolveResourceId(Resources resources, int id) {
        String fieldValue;
        if (id >= 0) {
            try {
                fieldValue = resources.getResourceTypeName(id) + '/' +
                        resources.getResourceEntryName(id);
            } catch (Resources.NotFoundException e) {
                fieldValue = "id/" + id;
            }
        } else {
            fieldValue = "NO_ID";
        }
        return fieldValue;
    }

    public static <T> void prepareNullableSpinnerAsync(final Spinner spinner,
                                                       final CoreElementActivity activity,
                                                       final @IdRes int fragmentId,
                                                       final String fieldName,
                                                       final View mainFragmentView,
                                                       final @IdRes int spinnerInfoId,
                                                       Stream<T> stream,
                                                       final StringPresenter<T> presenter,
                                                       final @StringRes int nullPresentation,
                                                       final OptionalInt selection,
                                                       final Optional<AdapterView.OnItemSelectedListener> listenerOptional) {
        // empty it first 'cause we need to do our task in background
        NullableDecoratingAdapter.adaptSpinnerWithArrayWrapper(spinner, Optional.<StringPresenter<String>>empty(), new String[] {});
        new AsyncTask<Stream, Void, List<T>>() {
            @Override
            protected List<T> doInBackground(Stream[] params) {
                // get values from db
                try {
                    //noinspection unchecked
                    return ((Stream<T>) params[0]).collect(Collectors.<T>toList());
                } catch (RuntimeException e) {
                    logger.warn("Exception while querying for spinner contents with stream", e);
                    return new ArrayList<>(1);
                }
            }

            @Override
            protected void onPostExecute(List<T> res) {
                prepareNullableSpinner(spinner, activity, fragmentId, fieldName, mainFragmentView, spinnerInfoId, res, presenter, nullPresentation, selection, listenerOptional);
            }
        }.execute(stream);
    }

    public static <T> void prepareNullableSpinner(Spinner spinner,
                                                  CoreElementActivity activity,
                                                  @IdRes int fragmentId,
                                                  String fieldName,
                                                  View mainFragmentView,
                                                  @IdRes int spinnerInfoId,
                                                  List<T> res,
                                                  StringPresenter<T> presenter,
                                                  @StringRes int nullPresentation,
                                                  OptionalInt selection,
                                                  Optional<AdapterView.OnItemSelectedListener> listenerOptional) {
        // fill spinner with data and schedule it for redrawing
        NullableDecoratingAdapter.adaptSpinnerWithArrayWrapper(spinner, Optional.of(presenter), res, OptionalInt.of(nullPresentation));
        if (selection.isPresent()) {
            spinner.setSelection(selection.getAsInt()); // if fragment saved spinner state, apply it
        }
        if (listenerOptional.isPresent()) {
            spinner.setOnItemSelectedListener(listenerOptional.get()); // apply fragment's spinner state listener if it was supplied
        }
        final CoreNotifier.Linker linker =
                activity.addFieldFragmentInfo(fragmentId, fieldName, spinner, mainFragmentView.findViewById(spinnerInfoId)); // bind spinner with activity structure

        // if state saved by fragment was applied, link it with core to prevent feedback nullifying it
        if (selection.isPresent()) {
            CoreNotifier.linkViewValueWithCore(spinner.getSelectedItem(), linker, activity);
        }

        spinner.invalidate();
    }

    public static <T> void prepareNullableSpinner(Spinner spinner,
                                                  CoreElementActivity activity,
                                                  String fieldName,
                                                  View mainFragmentView,
                                                  @IdRes int spinnerInfoId,
                                                  List<T> res,
                                                  StringPresenter<T> presenter,
                                                  @StringRes int nullPresentation,
                                                  OptionalInt selection,
                                                  Optional<AdapterView.OnItemSelectedListener> listenerOptional) {
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
        final int settingsOrder = menu.size() > 0 ? menu.getItem(0).getOrder() : 1;
        final String text = Formatting.toStringMoneyUsingText(totalBalance);
        final MenuItem fundsInfo = menu.add(Menu.NONE, FUNDS_ID, settingsOrder - 1, text);
        fundsInfo.setTitle(text);
        fundsInfo.setTitleCondensed(Formatting.toStringMoneyUsingSign(totalBalance, resources));
        fundsInfo.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return fundsInfo;
    }

    public static <T> void addToMutableSpinner(T obj, Spinner spinner) {
        final MutableAdapter<T> adapter = extractMutableAdapter(spinner);
        final int indexNewVal = adapter.getCount();
        adapter.add(obj);
        spinner.setSelection(indexNewVal);
    }

    @SuppressWarnings("unchecked")
    private static <T> MutableAdapter<T> extractMutableAdapter(Spinner spinner) {
        final SpinnerAdapter spAd = spinner.getAdapter();
        checkArgument(spAd instanceof MutableAdapter, "Spinner must be adapted by MutableAdapter");
        return (MutableAdapter<T>) spAd;
    }

    public static void replaceAccountInSpinner(BalanceAccount account, Spinner accountsSpinner) {
        final MutableAdapter<BalanceAccount> adapter = extractMutableAdapter(accountsSpinner);

        for (int i = 0; i < adapter.getCount(); i++) {
            final BalanceAccount item = adapter.getItem(i);

            if (item != null && item.name.equals(account.name) && item.getUnit().equals(account.getUnit())) {
                adapter.insert(account, i);
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
