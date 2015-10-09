package ru.adios.budgeter;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java8.util.function.Function;
import java8.util.stream.Collectors;
import ru.adios.budgeter.api.Treasury;


/**
 * A simple {@link Fragment} subclass.
 */
public class AccountStandardFragment extends Fragment {

    public static final String FIELD_ACCOUNT = "account";
    public static final String FIELD_NEW_ACCOUNT_NAME = "new_account_name";
    public static final String FIELD_NEW_ACCOUNT_CURRENCY = "new_account_currency";
    public static final String FIELD_NEW_ACCOUNT_AMOUNT = "new_account_amount";
    public static final String BUTTON_NEW_ACCOUNT_SUBMIT = "new_account_submit";


    public AccountStandardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View inflated = inflater.inflate(R.layout.fragment_account_standard, container, false);

        final CoreElementActivity activity = (CoreElementActivity) getActivity();
        final AccountsElementCore accountsCore = ((AccountsElementCoreProvider) activity).getAccountsElementCore();
        final int id = getId();

        // main spinner init
        final Spinner accountsSpinner = (Spinner) inflated.findViewById(R.id.accounts_spinner);
        HintedArrayAdapter.adaptArbitraryContainedSpinner(
                accountsSpinner,
                activity,
                accountsCore
                        .streamAccountBalances()
                        .map(new Function<Treasury.BalanceAccount, HintedArrayAdapter.ObjectContainer<Treasury.BalanceAccount>>() {
                            @Override
                            public HintedArrayAdapter.ObjectContainer<Treasury.BalanceAccount> apply(Treasury.BalanceAccount balanceAccount) {
                                return new AccountContainer(balanceAccount);
                            }
                        })
                        .collect(Collectors.<HintedArrayAdapter.ObjectContainer<Treasury.BalanceAccount>>toList())
        );
        activity.addFieldFragmentInfo(id, FIELD_ACCOUNT, accountsSpinner, inflated.findViewById(R.id.accounts_spinner_info));

        // hidden parts
        final EditText nameInput = (EditText) inflated.findViewById(R.id.accounts_name_input);
        final TextView nameInputInfo = (TextView) inflated.findViewById(R.id.accounts_name_input_info);
        activity.addFieldFragmentInfo(id, FIELD_NEW_ACCOUNT_NAME, nameInput, nameInputInfo);
        final Spinner currencyInput = (Spinner) inflated.findViewById(R.id.accounts_currency_input);
        HintedArrayAdapter.adaptStringSpinner(currencyInput, activity, Constants.CURRENCIES_DROPDOWN);
        final TextView currencyInputInfo = (TextView) inflated.findViewById(R.id.accounts_currency_input_info);
        activity.addFieldFragmentInfo(id, FIELD_NEW_ACCOUNT_CURRENCY, currencyInput, currencyInputInfo);
        final EditText amountInput = (EditText) inflated.findViewById(R.id.accounts_amount_optional_input);
        final TextView amountInputHint = (TextView) inflated.findViewById(R.id.accounts_amount_optional_input_hint);
        final TextView amountInputInfo = (TextView) inflated.findViewById(R.id.accounts_amount_optional_input_info);
        activity.addFieldFragmentInfo(id, FIELD_NEW_ACCOUNT_AMOUNT, amountInput, amountInputInfo);

        final Button submitButton = (Button) inflated.findViewById(R.id.accounts_submit_button);

        // button roundness and listener to show hidden interface
        final Button addButton = (Button) inflated.findViewById(R.id.accounts_add_button);
        final int diameter = addButton.getLayoutParams().height;
        final RelativeLayout.LayoutParams abParams = new RelativeLayout.LayoutParams(diameter, diameter);
        abParams.addRule(RelativeLayout.RIGHT_OF, R.id.accounts_spinner);
        addButton.setLayoutParams(abParams);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getVisibility() == View.VISIBLE) {
                    nameInput.setVisibility(View.VISIBLE);
                    nameInputInfo.setVisibility(View.INVISIBLE);
                    currencyInput.setVisibility(View.VISIBLE);
                    currencyInputInfo.setVisibility(View.INVISIBLE);
                    amountInput.setVisibility(View.VISIBLE);
                    amountInputHint.setVisibility(View.VISIBLE);
                    amountInputInfo.setVisibility(View.INVISIBLE);
                    submitButton.setVisibility(View.VISIBLE);
                    v.setVisibility(View.INVISIBLE);
                    inflated.invalidate();
                }
            }
        });

        // add submit button logic
        activity.addButtonFragmentInfo(id, BUTTON_NEW_ACCOUNT_SUBMIT, submitButton, new Runnable() {
            @Override
            public void run() {
                nameInput.setVisibility(View.GONE);
                nameInputInfo.setVisibility(View.GONE);
                currencyInput.setVisibility(View.GONE);
                currencyInputInfo.setVisibility(View.GONE);
                amountInput.setVisibility(View.GONE);
                amountInputHint.setVisibility(View.GONE);
                amountInputInfo.setVisibility(View.GONE);
                submitButton.setVisibility(View.GONE);
                addButton.setVisibility(View.VISIBLE);
                @SuppressWarnings("unchecked")
                final HintedArrayAdapter<Treasury.BalanceAccount> adapter = (HintedArrayAdapter<Treasury.BalanceAccount>) accountsSpinner.getAdapter();
                //noinspection ConstantConditions
                adapter.add(new HintedArrayAdapter.ToStringObjectContainer<>(new Treasury.BalanceAccount(accountsCore.getName(), accountsCore.getUnit())));
                accountsSpinner.setSelection(adapter.getCount(), true);
                inflated.invalidate();
            }
        });

        return inflated;
    }

    private static final class AccountContainer implements HintedArrayAdapter.ObjectContainer<Treasury.BalanceAccount> {

        private final Treasury.BalanceAccount account;

        private AccountContainer(Treasury.BalanceAccount account) {
            this.account = account;
        }

        @Override
        public Treasury.BalanceAccount getObject() {
            return account;
        }

        @Override
        public String toString() {
            return account.name;
        }

    }

}
