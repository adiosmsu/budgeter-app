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

package ru.adios.budgeter.fragments;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.UiThread;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import org.joda.money.CurrencyUnit;

import java.math.BigDecimal;

import java8.util.Optional;
import ru.adios.budgeter.Constants;
import ru.adios.budgeter.MoneySettable;
import ru.adios.budgeter.R;
import ru.adios.budgeter.adapters.NullableDecoratingAdapter;
import ru.adios.budgeter.adapters.StringPresenter;
import ru.adios.budgeter.core.AbstractCollectibleFeedbacker;
import ru.adios.budgeter.core.CollectibleFragmentInfoProvider;
import ru.adios.budgeter.core.CoreElementActivity;
import ru.adios.budgeter.core.CoreErrorHighlighter;
import ru.adios.budgeter.core.CoreFragment;
import ru.adios.budgeter.core.CoreNotifier;
import ru.adios.budgeter.core.Feedbacking;
import ru.adios.budgeter.util.EmptyOnItemSelectedListener;
import ru.adios.budgeter.util.EmptyTextWatcher;
import ru.adios.budgeter.util.Formatting;


/**
 * Common fields for entering monetary value.
 */
@UiThread
public class EnterAmountFragment extends CoreFragment {

    public static final String FIELD_AMOUNT_DECIMAL = "amount_decimal";
    public static final String FIELD_AMOUNT_CURRENCY = "amount_currency";

    public static CollectibleFragmentInfoProvider getInfoProvider(@IdRes final int fragmentId,
                                                                  final MoneySettable monSet,
                                                                  final CoreErrorHighlighter highlighter,
                                                                  String decCoreName,
                                                                  String unitCoreName) {
        return new CollectibleFragmentInfoProvider.Builder(fragmentId, new Feedbacker(fragmentId, monSet), highlighter)
                .addFieldInfo(FIELD_AMOUNT_DECIMAL, new CoreElementActivity.CoreElementFieldInfo(decCoreName, new CoreNotifier.DecimalLinker() {
                    @Override
                    public boolean link(BigDecimal data) {
                        final BigDecimal prev = monSet.getAmountDecimal();
                        if (!prev.equals(data != null ? data : BigDecimal.ZERO)) {
                            monSet.setAmountDecimal(data);
                            return true;
                        }
                        return false;
                    }
                }, highlighter))
                .addFieldInfo(FIELD_AMOUNT_CURRENCY, new CoreElementActivity.CoreElementFieldInfo(unitCoreName, new CoreNotifier.CurrencyLinker() {
                    @Override
                    public boolean link(CurrencyUnit data) {
                        final CurrencyUnit prev = monSet.getAmountUnit();
                        if ((prev == null && data != null) || (prev != null && !prev.equals(data))) {
                            monSet.setAmountUnit(data);
                            return true;
                        }
                        return false;
                    }
                }, highlighter))
                .build();
    }


    private BigDecimal decimalVal;
    private int curSelection = -1;

    public EnterAmountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View inflated = inflater.inflate(R.layout.fragment_enter_amount, container, false);

        final CoreElementActivity activity = (CoreElementActivity) getActivity();

        // Init currencies choice
        final Spinner currencySpinner = (Spinner) inflated.findViewById(R.id.amount_currency);
        final TextView amountTextView = (TextView) inflated.findViewById(R.id.amount_decimal);
        NullableDecoratingAdapter.adaptSpinnerWithArrayWrapper(currencySpinner, Optional.<StringPresenter<String>>empty(), Constants.currenciesDropdownCopy());

        final int id = getId();

        amountTextView.addTextChangedListener(new EmptyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                final String val = s.toString();
                if (Formatting.isDecimal(val)) {
                    decimalVal = new BigDecimal(val);
                }
            }
        });
        if (decimalVal != null) {
            amountTextView.setText(decimalVal.toPlainString());
        }
        final CoreNotifier.Linker decLinker = activity.addFieldFragmentInfo(id, FIELD_AMOUNT_DECIMAL, amountTextView, inflated.findViewById(R.id.amount_decimal_info));
        if (decimalVal != null) {
            CoreNotifier.linkViewValueWithCore(decimalVal, decLinker, activity);
        }

        currencySpinner.setOnItemSelectedListener(new EmptyOnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                curSelection = parent.getAdapter().getCount() > position ? position : -1;
            }
        });
        if (curSelection >= 0) {
            currencySpinner.setSelection(curSelection);
        }
        final CoreNotifier.Linker curLinker = activity.addFieldFragmentInfo(id, FIELD_AMOUNT_CURRENCY, currencySpinner, inflated.findViewById(R.id.amount_currency_info));
        if (curSelection >= 0) {
            CoreNotifier.linkViewValueWithCore(currencySpinner.getSelectedItem(), curLinker, activity);
        }

        return inflated;
    }


    public static final class Feedbacker extends AbstractCollectibleFeedbacker {

        @IdRes
        private final int fragmentId;
        private final MoneySettable monSet;

        private TextView amountDecimal;
        private Spinner amountCurrency;

        private Feedbacker(@IdRes int fragmentId, MoneySettable monSet) {
            this.fragmentId = fragmentId;
            this.monSet = monSet;
        }

        @Override
        protected void clearViewReferencesOptimal() {
            amountDecimal = null;
            amountCurrency = null;
        }

        @Override
        protected void performFeedbackSafe() {
            Feedbacking.decimalTextViewFeedback(monSet.getAmountDecimal(), amountDecimal);
            Feedbacking.currenciesSpinnerFeedback(monSet.getAmountUnit(), amountCurrency);
        }

        @Override
        protected void collectEssentialViewsOptimal(CoreElementActivity activity) {
            final View fragmentLayout = activity.findViewById(fragmentId);
            amountDecimal = (TextView) fragmentLayout.findViewById(R.id.amount_decimal);
            amountCurrency = (Spinner) fragmentLayout.findViewById(R.id.amount_currency);
        }

    }

}
