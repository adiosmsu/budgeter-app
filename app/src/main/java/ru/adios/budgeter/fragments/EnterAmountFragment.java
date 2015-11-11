package ru.adios.budgeter.fragments;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.UiThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import org.joda.money.CurrencyUnit;

import java.math.BigDecimal;

import ru.adios.budgeter.Constants;
import ru.adios.budgeter.MoneySettable;
import ru.adios.budgeter.R;
import ru.adios.budgeter.adapters.HintedArrayAdapter;
import ru.adios.budgeter.core.AbstractCollectibleFeedbacker;
import ru.adios.budgeter.core.CollectibleFragmentInfoProvider;
import ru.adios.budgeter.core.CoreElementActivity;
import ru.adios.budgeter.core.CoreErrorHighlighter;
import ru.adios.budgeter.core.CoreFragment;
import ru.adios.budgeter.core.CoreNotifier;
import ru.adios.budgeter.core.Feedbacking;


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
                    public void link(BigDecimal data) {
                        monSet.setAmountDecimal(data);
                    }
                }, highlighter))
                .addFieldInfo(FIELD_AMOUNT_CURRENCY, new CoreElementActivity.CoreElementFieldInfo(unitCoreName, new CoreNotifier.CurrencyLinker() {
                    @Override
                    public void link(CurrencyUnit data) {
                        monSet.setAmountUnit(data);
                    }
                }, highlighter))
                .build();
    }


    public EnterAmountFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View inflated = inflater.inflate(R.layout.fragment_enter_amount, container, false);

        final CoreElementActivity activity = (CoreElementActivity) getActivity();

        // Init currencies choice
        final Spinner currencySpinner = (Spinner) inflated.findViewById(R.id.amount_currency);
        HintedArrayAdapter.adaptStringSpinner(currencySpinner, activity, Constants.CURRENCIES_DROPDOWN);

        // Register listeners in parent activity
        final int id = getId();
        activity.addFieldFragmentInfo(id, FIELD_AMOUNT_DECIMAL, inflated.findViewById(R.id.amount_decimal), inflated.findViewById(R.id.amount_decimal_info));
        activity.addFieldFragmentInfo(id, FIELD_AMOUNT_CURRENCY, currencySpinner, inflated.findViewById(R.id.amount_currency_info));

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
