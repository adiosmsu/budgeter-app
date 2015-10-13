package ru.adios.budgeter;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import org.joda.money.CurrencyUnit;

import java.math.BigDecimal;

import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.HintedArrayAdapter;


/**
 * Common fields for entering monetary value.
 */
public class EnterAmountFragment extends Fragment {

    public static final String FIELD_AMOUNT_DECIMAL = "amount_decimal";
    public static final String FIELD_AMOUNT_CURRENCY = "amount_currency";

    public static CollectibleFragmentInfoProvider<Treasury.BalanceAccount> getInfoProvider(@IdRes int fragmentId,
                                                                                           final FundsAdditionElementCore additionElement,
                                                                                           final CoreErrorHighlighter addFundsErrorHighlighter) {
        return new CollectibleFragmentInfoProvider.Builder<>(fragmentId, new CollectibleFragmentInfoProvider.Feedbacker<Treasury.BalanceAccount>() {
            @Override
            public void performFeedback(CoreElementActivity<Treasury.BalanceAccount> activity) {
                activity.accountSpinnerFeedback(additionElement.getAccount(), R.id.accounts_spinner);
                activity.decimalTextViewFeedback(additionElement.getAmountDecimal(), R.id.amount_decimal);
                activity.currenciesSpinnerFeedback(additionElement.getAmountUnit(), R.id.amount_currency);
            }
        })
                .addFieldInfo(FIELD_AMOUNT_DECIMAL, new CoreElementActivity.CoreElementFieldInfo(FundsAdditionElementCore.FIELD_AMOUNT_DECIMAL, new CoreNotifier.DecimalLinker() {
                    @Override
                    public void link(BigDecimal data) {
                        additionElement.setAmountDecimal(data);
                    }
                }, addFundsErrorHighlighter))
                .addFieldInfo(EnterAmountFragment.FIELD_AMOUNT_CURRENCY, new CoreElementActivity.CoreElementFieldInfo(FundsAdditionElementCore.FIELD_AMOUNT_UNIT, new CoreNotifier.CurrencyLinker() {
                    @Override
                    public void link(CurrencyUnit data) {
                        additionElement.setAmountUnit(data);
                    }
                }, addFundsErrorHighlighter))
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

}
