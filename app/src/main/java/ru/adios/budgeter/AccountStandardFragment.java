package ru.adios.budgeter;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.joda.money.CurrencyUnit;

import java.math.BigDecimal;

import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.util.BalanceAccountContainer;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.HintedArrayAdapter;
import ru.adios.budgeter.util.UiUtils;


/**
 * Fragment for accounts spinner with a button to add a new one.
 */
public class AccountStandardFragment extends Fragment {

    public static final String FIELD_ACCOUNT = "account";
    public static final String FIELD_NEW_ACCOUNT_NAME = "new_account_name";
    public static final String FIELD_NEW_ACCOUNT_CURRENCY = "new_account_currency";
    public static final String FIELD_NEW_ACCOUNT_AMOUNT = "new_account_amount";
    public static final String BUTTON_NEW_ACCOUNT_SUBMIT = "new_account_submit";

    public static InfoProvider getInfoProvider(@IdRes int fragmentId, final FundsAdditionElementCore additionElement, final CoreErrorHighlighter addFundsErrorHighlighter) {
        final Mutable<Optional<BigDecimal>> newAccountOptionalAmount = new Mutable<>(Optional.of(BigDecimal.ZERO));
        final AccountsElementCore accountsElement = new AccountsElementCore(Schema.TREASURY);
        final CoreErrorHighlighter accountsErrorHighlighter = new CoreErrorHighlighter();
        final Submitter<Treasury.BalanceAccount> hybridAccountCore = new Submitter<Treasury.BalanceAccount>() {
            @Override
            public Result<Treasury.BalanceAccount> submit() {
                final Result<Treasury.BalanceAccount> accountResult = accountsElement.submit();

                if (!accountResult.isSuccessful()) {
                    return accountResult;
                }

                final BigDecimal dec = newAccountOptionalAmount.object.get();
                if (!dec.equals(BigDecimal.ZERO)) {
                    final FundsAdditionElementCore core = new FundsAdditionElementCore(Schema.TREASURY);
                    core.setAccount(accountsElement.getName());
                    core.setAmountDecimal(dec);
                    //noinspection ConstantConditions
                    core.setAmountUnit(accountsElement.getUnit());
                    return core.submit();
                }

                return accountResult;
            }
        };

        CollectibleFragmentInfoProvider<Treasury.BalanceAccount> delegate = new CollectibleFragmentInfoProvider.Builder<>(fragmentId, new CollectibleFragmentInfoProvider.Feedbacker<Treasury.BalanceAccount>() {
            @Override
            public void performFeedback(CoreElementActivity<Treasury.BalanceAccount> activity) {
                activity.textViewFeedback(accountsElement.getName(), R.id.accounts_name_input);
                activity.currenciesSpinnerFeedback(accountsElement.getUnit(), R.id.accounts_currency_input);
                activity.decimalTextViewFeedback(newAccountOptionalAmount.object.get(), R.id.accounts_amount_optional_input);
            }
        })
                .addButtonInfo(BUTTON_NEW_ACCOUNT_SUBMIT, new CoreElementActivity.CoreElementSubmitInfo<>(hybridAccountCore, null, accountsErrorHighlighter))
                .addFieldInfo(AccountStandardFragment.FIELD_ACCOUNT, new CoreElementActivity.CoreElementFieldInfo(FundsAdditionElementCore.FIELD_ACCOUNT, new CoreNotifier.ArbitraryLinker() {
                    @Override
                    public void link(HintedArrayAdapter.ObjectContainer data) {
                        final Treasury.BalanceAccount account = (Treasury.BalanceAccount) data.getObject();
                        additionElement.setAccount(account);
                    }
                }, addFundsErrorHighlighter))
                .addFieldInfo(FIELD_NEW_ACCOUNT_NAME, new CoreElementActivity.CoreElementFieldInfo(AccountsElementCore.FIELD_NAME, new CoreNotifier.TextLinker() {
                    @Override
                    public void link(String data) {
                        accountsElement.setName(data);
                    }
                }, accountsErrorHighlighter))
                .addFieldInfo(AccountStandardFragment.FIELD_NEW_ACCOUNT_CURRENCY, new CoreElementActivity.CoreElementFieldInfo(AccountsElementCore.FIELD_UNIT, new CoreNotifier.CurrencyLinker() {
                    @Override
                    public void link(CurrencyUnit data) {
                        accountsElement.setUnit(data);
                    }
                }, accountsErrorHighlighter))
                .addFieldInfo(AccountStandardFragment.FIELD_NEW_ACCOUNT_AMOUNT, new CoreElementActivity.CoreElementFieldInfo(FundsAdditionElementCore.FIELD_AMOUNT_DECIMAL, new CoreNotifier.DecimalLinker() {
                    @Override
                    public void link(BigDecimal data) {
                        newAccountOptionalAmount.object = Optional.of(data);
                    }
                }, accountsErrorHighlighter))
                .build();

        return new InfoProvider(delegate, accountsElement, accountsErrorHighlighter);
    }

    public static final class InfoProvider implements CollectedFragmentsInfoProvider.InfoProvider<Treasury.BalanceAccount> {

        private final CollectibleFragmentInfoProvider<Treasury.BalanceAccount> delegate;

        public final AccountsElementCore accountsElement;
        public final CoreErrorHighlighter accountsErrorHighlighter;

        private InfoProvider(CollectibleFragmentInfoProvider<Treasury.BalanceAccount> delegate, AccountsElementCore accountsElement, CoreErrorHighlighter accountsErrorHighlighter) {
            this.delegate = delegate;
            this.accountsElement = accountsElement;
            this.accountsErrorHighlighter = accountsErrorHighlighter;
        }

        @Override
        public int getFragmentId() {
            return delegate.getFragmentId();
        }

        @Override
        public CoreElementActivity.CoreElementSubmitInfo<Treasury.BalanceAccount> getSubmitInfo(String buttonName) {
            return delegate.getSubmitInfo(buttonName);
        }

        @Override
        public CoreElementActivity.CoreElementFieldInfo getCoreElementFieldInfo(String fragmentFieldName) {
            return delegate.getCoreElementFieldInfo(fragmentFieldName);
        }

        @Override
        public void performFeedback(CoreElementActivity<Treasury.BalanceAccount> activity) {
            delegate.performFeedback(activity);
        }

    }

    public AccountStandardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View inflated = inflater.inflate(R.layout.fragment_account_standard, container, false);

        @SuppressWarnings("unchecked")
        final CoreElementActivity<Treasury.BalanceAccount> activity = (CoreElementActivity<Treasury.BalanceAccount>) getActivity();
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
                                return new BalanceAccountContainer(balanceAccount);
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
        addButton.post(new Runnable() {
            @Override
            public void run() {
                final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) addButton.getLayoutParams();
                params.width = addButton.getHeight();
                addButton.setLayoutParams(params);
                addButton.invalidate();
            }
        });
        //final int diameter = addButton.getLayoutParams().height;
        //final RelativeLayout.LayoutParams abParams = new RelativeLayout.LayoutParams(diameter, diameter);
        //abParams.addRule(RelativeLayout.RIGHT_OF, R.id.accounts_spinner);
        //addButton.setLayoutParams(abParams);
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
        addButton.invalidate();

        // add submit button logic
        activity.addSubmitFragmentInfo(id, submitButton, BUTTON_NEW_ACCOUNT_SUBMIT, new Consumer<Treasury.BalanceAccount>() {
            @Override
            public void accept(Treasury.BalanceAccount account) {
                nameInput.setVisibility(View.GONE);
                nameInputInfo.setVisibility(View.GONE);
                currencyInput.setVisibility(View.GONE);
                currencyInputInfo.setVisibility(View.GONE);
                amountInput.setVisibility(View.GONE);
                amountInputHint.setVisibility(View.GONE);
                amountInputInfo.setVisibility(View.GONE);
                submitButton.setVisibility(View.GONE);
                addButton.setVisibility(View.VISIBLE);
                UiUtils.addAccountToSpinner(account, accountsSpinner);
                inflated.invalidate();
            }
        });

        return inflated;
    }

    private static final class Mutable<T> {

        private T object;

        private Mutable(T object) {
            this.object = object;
        }

    }

}
