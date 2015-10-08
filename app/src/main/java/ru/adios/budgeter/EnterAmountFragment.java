package ru.adios.budgeter;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;


/**
 * Common fields for entering monetary value.
 */
public class EnterAmountFragment extends Fragment {

    public static final String FIELD_AMOUNT_DECIMAL = "amount_decimal";
    public static final String FIELD_AMOUNT_CURRENCY = "amount_currency";

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
        final ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, Constants.CURRENCIES_DROPDOWN);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(dataAdapter);

        // Register listeners in parent activity
        final int id = getId();
        activity.addFieldFragmentInfo(id, FIELD_AMOUNT_DECIMAL, inflated.findViewById(R.id.amount_decimal), inflated.findViewById(R.id.amount_decimal_info));
        activity.addFieldFragmentInfo(id, FIELD_AMOUNT_CURRENCY, currencySpinner, inflated.findViewById(R.id.amount_currency_info));

        return inflated;
    }

}
