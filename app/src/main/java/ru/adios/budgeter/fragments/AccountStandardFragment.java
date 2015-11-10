package ru.adios.budgeter.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.UiThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Supplier;
import ru.adios.budgeter.AccountsElementCore;
import ru.adios.budgeter.BalancesUiThreadState;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.Constants;
import ru.adios.budgeter.FundsAdditionElementCore;
import ru.adios.budgeter.R;
import ru.adios.budgeter.Submitter;
import ru.adios.budgeter.adapters.BalanceAccountContainer;
import ru.adios.budgeter.adapters.HintedArrayAdapter;
import ru.adios.budgeter.api.TransactionalSupport;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.core.AbstractCollectibleFeedbacker;
import ru.adios.budgeter.core.CollectibleFragmentInfoProvider;
import ru.adios.budgeter.core.CoreElementActivity;
import ru.adios.budgeter.core.CoreErrorHighlighter;
import ru.adios.budgeter.core.CoreFragment;
import ru.adios.budgeter.core.CoreNotifier;
import ru.adios.budgeter.core.Feedbacking;
import ru.adios.budgeter.util.UiUtils;

import static com.google.common.base.Preconditions.checkState;


/**
 * Fragment for accounts spinner with a button to add a new one.
 */
@UiThread
public class AccountStandardFragment extends CoreFragment {

    public static final String FIELD_ACCOUNT = "account";
    public static final String FIELD_NEW_ACCOUNT_NAME = "new_account_name";
    public static final String FIELD_NEW_ACCOUNT_CURRENCY = "new_account_currency";
    public static final String FIELD_NEW_ACCOUNT_AMOUNT = "new_account_amount";
    public static final String FIELD_NEW_ACCOUNT_DESC = "new_account_desc";
    public static final String BUTTON_NEW_ACCOUNT_SUBMIT = "new_account_submit";

    public static final String KEY_HIGHLIGHTER = "acc_frag_highlighter";
    public static final String KEY_OPT_AMOUNT = "acc_frag_opt_amount";

    public static InfoProviderBuilder getInfoProviderBuilder(@IdRes int fragmentId, Context context, Supplier<BalanceAccount> accountSupplier) {
        return new InfoProviderBuilder(fragmentId, context, accountSupplier);
    }

    public static final class InfoProviderBuilder {

        private final Mutable<Optional<BigDecimal>> newAccountOptionalAmount = new Mutable<>(Optional.of(BigDecimal.ZERO));
        private final AccountsElementCore accountsElement = new AccountsElementCore(BundleProvider.getBundle().treasury());
        private final CoreErrorHighlighter accountsErrorHighlighter = new CoreErrorHighlighter(KEY_HIGHLIGHTER);
        private final HybridAccountCore hybridAccountCore = new HybridAccountCore(accountsElement, newAccountOptionalAmount);

        private final Context context;
        private final CollectibleFragmentInfoProvider.Builder<BalanceAccount, HybridAccountCore> mainBuilder;

        @Nullable
        private Consumer<BalanceAccount> callback;
        private CoreElementActivity.CoreElementFieldInfo accountFieldInfo;

        public InfoProviderBuilder(@IdRes int fragmentId, Context context, final Supplier<BalanceAccount> accountSupplier) {
            this.context = context;
            mainBuilder = new CollectibleFragmentInfoProvider.Builder<>(
                    fragmentId,
                    new Feedbacker(fragmentId, accountsElement, newAccountOptionalAmount, accountSupplier),
                    accountsErrorHighlighter,
                    new CoreElementActivity.Retainer() {
                        @Override
                        public void onSaveInstanceState(Bundle outState) {
                            final Optional<BigDecimal> o = newAccountOptionalAmount.object;
                            if (o.isPresent()) {
                                outState.putString(KEY_OPT_AMOUNT, o.get().toPlainString());
                            } else {
                                outState.putString(KEY_OPT_AMOUNT, null);
                            }
                        }

                        @Override
                        public void onRestoreInstanceState(Bundle savedInstanceState) {
                            final String oaVal = savedInstanceState.getString(KEY_OPT_AMOUNT);
                            newAccountOptionalAmount.object = oaVal != null
                                    ? Optional.of(new BigDecimal(oaVal))
                                    : Optional.<BigDecimal>empty();
                        }
                    }
            );
        }

        public InfoProviderBuilder setNewAccountSubmitButtonCallback(@Nullable Consumer<BalanceAccount> callback) {
            this.callback = callback;
            return this;
        }

        public InfoProviderBuilder provideAccountFieldInfo(String coreFieldName, CoreErrorHighlighter highlighter, CoreNotifier.HintedLinker linker) {
            accountFieldInfo = new CoreElementActivity.CoreElementFieldInfo(coreFieldName, linker, highlighter);
            return this;
        }

        public CollectibleFragmentInfoProvider<BalanceAccount, HybridAccountCore> build() {
            checkState(accountFieldInfo != null, "Account field info not provided");

            return mainBuilder
                    .addButtonInfo(BUTTON_NEW_ACCOUNT_SUBMIT, new CoreElementActivity.CoreElementSubmitInfo<>(hybridAccountCore, new Consumer<BalanceAccount>() {
                        @Override
                        public void accept(BalanceAccount account) {
                            final Optional<Money> balance = account.getBalance();
                            final Money bal;

                            if (balance.isPresent() && (bal = balance.get()).isPositive()) {
                                BalancesUiThreadState.addMoney(bal, context);
                            }

                            if (callback != null) {
                                callback.accept(account);
                            }
                        }
                    }, accountsErrorHighlighter))
                    .addFieldInfo(FIELD_ACCOUNT, accountFieldInfo)
                    .addFieldInfo(FIELD_NEW_ACCOUNT_NAME, new CoreElementActivity.CoreElementFieldInfo(AccountsElementCore.FIELD_NAME, new CoreNotifier.TextLinker() {
                        @Override
                        public void link(String data) {
                            accountsElement.setName(data);
                        }
                    }, accountsErrorHighlighter))
                    .addFieldInfo(FIELD_NEW_ACCOUNT_DESC, new CoreElementActivity.CoreElementFieldInfo(AccountsElementCore.FIELD_DESCRIPTION, new CoreNotifier.TextLinker() {
                        @Override
                        public void link(String data) {
                            accountsElement.setDescription(data);
                        }
                    }, accountsErrorHighlighter))
                    .addFieldInfo(FIELD_NEW_ACCOUNT_CURRENCY, new CoreElementActivity.CoreElementFieldInfo(AccountsElementCore.FIELD_UNIT, new CoreNotifier.CurrencyLinker() {
                        @Override
                        public void link(CurrencyUnit data) {
                            accountsElement.setUnit(data);
                        }
                    }, accountsErrorHighlighter))
                    .addFieldInfo(FIELD_NEW_ACCOUNT_AMOUNT, new CoreElementActivity.CoreElementFieldInfo(FundsAdditionElementCore.FIELD_AMOUNT_DECIMAL, new CoreNotifier.DecimalLinker() {
                        @Override
                        public void link(BigDecimal data) {
                            if (!newAccountOptionalAmount.lockOn) {
                                newAccountOptionalAmount.object = Optional.of(data);
                            }
                        }
                    }, accountsErrorHighlighter))
                    .build();
        }
    }

    public static final class Feedbacker extends AbstractCollectibleFeedbacker {

        @IdRes
        private final int fragmentId;
        private final AccountsElementCore accountsElement;
        private final Mutable<Optional<BigDecimal>> newAccountOptionalAmount;
        private final Supplier<BalanceAccount> accountSupplier;

        private TextView accountsNameInput;
        private TextView accountsDescInput;
        private Spinner accountsCurrencyInput;
        private TextView accountsAmountOptionalInput;
        private Spinner accountsSpinner;

        private Feedbacker(int fragmentId, AccountsElementCore accountsElement, Mutable<Optional<BigDecimal>> newAccountOptionalAmount, Supplier<BalanceAccount> accountSupplier) {
            this.fragmentId = fragmentId;
            this.accountsElement = accountsElement;
            this.newAccountOptionalAmount = newAccountOptionalAmount;
            this.accountSupplier = accountSupplier;
        }

        @Override
        public void performFeedbackSafe() {
            Feedbacking.textViewFeedback(accountsElement.getName(), accountsNameInput);
            Feedbacking.textViewFeedback(accountsElement.getDescription(), accountsDescInput);
            Feedbacking.currenciesSpinnerFeedback(accountsElement.getUnit(), accountsCurrencyInput);
            Feedbacking.decimalTextViewFeedback(newAccountOptionalAmount.object.get(), accountsAmountOptionalInput);
            Feedbacking.hintedArraySpinnerFeedback(accountSupplier.get(), accountsSpinner);
        }

        @Override
        public void clearViewReferencesOptimal() {
            accountsNameInput = null;
            accountsDescInput = null;
            accountsCurrencyInput = null;
            accountsAmountOptionalInput = null;
            accountsSpinner = null;
        }

        @Override
        public void collectEssentialViewsOptimal(CoreElementActivity activity) {
            final View fragmentLayout = activity.findViewById(fragmentId);
            accountsNameInput = (TextView) fragmentLayout.findViewById(R.id.accounts_name_input);
            accountsDescInput = (TextView) fragmentLayout.findViewById(R.id.accounts_desc_input);
            accountsCurrencyInput = (Spinner) fragmentLayout.findViewById(R.id.accounts_currency_input);
            accountsAmountOptionalInput = (TextView) fragmentLayout.findViewById(R.id.accounts_amount_optional_input);
            accountsSpinner = (Spinner) fragmentLayout.findViewById(R.id.accounts_spinner);
        }

    }

    public static final class HybridAccountCore implements Submitter<BalanceAccount> {

        public final AccountsElementCore accountsElement;
        private final Mutable<Optional<BigDecimal>> newAccountOptionalAmount;
        private Result<BalanceAccount> storedResult;

        public HybridAccountCore(AccountsElementCore accountsElement, Mutable<Optional<BigDecimal>> newAccountOptionalAmount) {
            this.accountsElement = accountsElement;
            this.newAccountOptionalAmount = newAccountOptionalAmount;
        }

        @Override
        public Result<BalanceAccount> submit() {
            final Result<BalanceAccount> accountResult = accountsElement.submit();

            if (!accountResult.isSuccessful()) {
                return accountResult;
            }

            final BigDecimal dec = newAccountOptionalAmount.object.get();
            if (!dec.equals(BigDecimal.ZERO)) {
                final FundsAdditionElementCore core = new FundsAdditionElementCore(BundleProvider.getBundle().treasury());
                core.setAccount(accountResult.submitResult);
                core.setAmountDecimal(dec);
                return core.submit();
            }

            return accountResult;
        }

        @Override
        public TransactionalSupport getTransactional() {
            return accountsElement.getTransactional();
        }

        @Override
        public void setTransactional(TransactionalSupport transactional) {
            accountsElement.setTransactional(transactional);
        }

        @Override
        public void lock() {
            accountsElement.lock();
            newAccountOptionalAmount.lockOn = true;
        }

        @Override
        public void unlock() {
            accountsElement.unlock();
            newAccountOptionalAmount.lockOn = false;
        }

        @Override
        public Result<BalanceAccount> getStoredResult() {
            return storedResult;
        }

        @Override
        public void submitAndStoreResult() {
            storedResult = submit();
        }

    }


    private boolean editOpen = false;

    public AccountStandardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View inflated = inflater.inflate(R.layout.fragment_account_standard, container, false);

        final CoreElementActivity activity = (CoreElementActivity) getActivity();
        final int id = getId();

        // main spinner init
        final Spinner accountsSpinner = (Spinner) inflated.findViewById(R.id.accounts_spinner);
        UiUtils.prepareHintedSpinnerAsync(accountsSpinner, activity, id, FIELD_ACCOUNT, inflated, R.id.accounts_spinner_info, BundleProvider.getBundle().treasury().streamRegisteredAccounts(),
                new Function<BalanceAccount, HintedArrayAdapter.ObjectContainer<BalanceAccount>>() {
                    @Override
                    public HintedArrayAdapter.ObjectContainer<BalanceAccount> apply(BalanceAccount balanceAccount) {
                        return new BalanceAccountContainer(balanceAccount, getResources());
                    }
                }
        );

        // hidden parts
        final EditText nameInput = (EditText) inflated.findViewById(R.id.accounts_name_input);
        final TextView nameInputInfo = (TextView) inflated.findViewById(R.id.accounts_name_input_info);
        final EditText descInput = (EditText) inflated.findViewById(R.id.accounts_desc_input);
        final TextView descInputInfo = (TextView) inflated.findViewById(R.id.accounts_desc_input_info);
        final TextView descInputOpt = (TextView) inflated.findViewById(R.id.accounts_desc_optional);
        activity.addFieldFragmentInfo(id, FIELD_NEW_ACCOUNT_NAME, nameInput, nameInputInfo);
        activity.addFieldFragmentInfo(id, FIELD_NEW_ACCOUNT_DESC, descInput, descInputInfo);
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
        UiUtils.makeButtonSquaredByHeight(addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getVisibility() == View.VISIBLE) {
                    editOpen = true;
                    showEdit(v, nameInput, nameInputInfo, descInput, descInputInfo, descInputOpt,
                            currencyInput, currencyInputInfo, amountInput, amountInputHint, amountInputInfo, submitButton);
                    inflated.invalidate();
                }
            }
        });
        addButton.invalidate();

        // add submit button logic
        activity.addSubmitFragmentInfo(id, submitButton, BUTTON_NEW_ACCOUNT_SUBMIT, new Consumer<BalanceAccount>() {
            @Override
            public void accept(BalanceAccount account) {
                editOpen = false;
                hideEdit(nameInput, nameInputInfo, descInput, descInputInfo, descInputOpt,
                        currencyInput, currencyInputInfo, amountInput, amountInputHint, amountInputInfo, submitButton, addButton);
                UiUtils.addToHintedSpinner(account, accountsSpinner, BalanceAccountContainer.getFactory(getResources()));
                inflated.invalidate();
            }
        });

        if (editOpen) {
            showEdit(addButton, nameInput, nameInputInfo, descInput, descInputInfo, descInputOpt,
                    currencyInput, currencyInputInfo, amountInput, amountInputHint, amountInputInfo, submitButton);
        } else {
            hideEdit(nameInput, nameInputInfo, descInput, descInputInfo, descInputOpt,
                    currencyInput, currencyInputInfo, amountInput, amountInputHint, amountInputInfo, submitButton, addButton);
        }

        return inflated;
    }

    private void hideEdit(EditText nameInput, TextView nameInputInfo, EditText descInput, TextView descInputInfo,
                          TextView descInputOpt, Spinner currencyInput, TextView currencyInputInfo, EditText amountInput,
                          TextView amountInputHint, TextView amountInputInfo, Button submitButton, Button addButton) {
        nameInput.setVisibility(View.GONE);
        nameInputInfo.setVisibility(View.GONE);
        descInput.setVisibility(View.GONE);
        descInputInfo.setVisibility(View.GONE);
        descInputOpt.setVisibility(View.GONE);
        currencyInput.setVisibility(View.GONE);
        currencyInputInfo.setVisibility(View.GONE);
        amountInput.setVisibility(View.GONE);
        amountInputHint.setVisibility(View.GONE);
        amountInputInfo.setVisibility(View.GONE);
        submitButton.setVisibility(View.GONE);
        addButton.setVisibility(View.VISIBLE);
    }

    private void showEdit(View v, EditText nameInput, TextView nameInputInfo, EditText descInput,
                          TextView descInputInfo, TextView descInputOpt, Spinner currencyInput, TextView currencyInputInfo,
                          EditText amountInput, TextView amountInputHint, TextView amountInputInfo, Button submitButton) {
        nameInput.setVisibility(View.VISIBLE);
        nameInputInfo.setVisibility(View.INVISIBLE);
        descInput.setVisibility(View.VISIBLE);
        descInputInfo.setVisibility(View.INVISIBLE);
        descInputOpt.setVisibility(View.VISIBLE);
        currencyInput.setVisibility(View.VISIBLE);
        currencyInputInfo.setVisibility(View.INVISIBLE);
        amountInput.setVisibility(View.VISIBLE);
        amountInputHint.setVisibility(View.VISIBLE);
        amountInputInfo.setVisibility(View.INVISIBLE);
        submitButton.setVisibility(View.VISIBLE);
        v.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);
        super.onCreate(savedInstanceState);
    }

    private static final class Mutable<T> {

        private T object;

        private boolean lockOn = false;

        private Mutable(T object) {
            this.object = object;
        }

    }

}
