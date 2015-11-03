package ru.adios.budgeter;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
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
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.util.BalanceAccountContainer;
import ru.adios.budgeter.util.BalancesUiThreadState;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.HintedArrayAdapter;
import ru.adios.budgeter.util.UiUtils;

import static com.google.common.base.Preconditions.checkState;


/**
 * Fragment for accounts spinner with a button to add a new one.
 */
public class AccountStandardFragment extends CoreFragment {

    public static final String FIELD_ACCOUNT = "account";
    public static final String FIELD_NEW_ACCOUNT_NAME = "new_account_name";
    public static final String FIELD_NEW_ACCOUNT_CURRENCY = "new_account_currency";
    public static final String FIELD_NEW_ACCOUNT_AMOUNT = "new_account_amount";
    public static final String BUTTON_NEW_ACCOUNT_SUBMIT = "new_account_submit";

    public static InfoProviderBuilder getInfoProviderBuilder(@IdRes int fragmentId, Context context, Supplier<BalanceAccount> accountSupplier) {
        return new InfoProviderBuilder(fragmentId, context, accountSupplier);
    }

    public static final class InfoProviderBuilder {

        private final Mutable<Optional<BigDecimal>> newAccountOptionalAmount = new Mutable<>(Optional.of(BigDecimal.ZERO));
        private final AccountsElementCore accountsElement = new AccountsElementCore(BundleProvider.getBundle().treasury());
        private final CoreErrorHighlighter accountsErrorHighlighter = new CoreErrorHighlighter();
        private final HybridAccountCore hybridAccountCore = new HybridAccountCore(accountsElement, newAccountOptionalAmount);

        private final Context context;
        private final CollectibleFragmentInfoProvider.Builder<BalanceAccount, HybridAccountCore> mainBuilder;

        @Nullable
        private Consumer<BalanceAccount> callback;
        private CoreElementActivity.CoreElementFieldInfo accountFieldInfo;

        public InfoProviderBuilder(@IdRes int fragmentId, Context context, Supplier<BalanceAccount> accountSupplier) {
            this.context = context;
            mainBuilder = new CollectibleFragmentInfoProvider.Builder<>(fragmentId, new Feedbacker(fragmentId, accountsElement, newAccountOptionalAmount, accountSupplier));
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

    public static final class Feedbacker implements CollectibleFragmentInfoProvider.Feedbacker {

        @IdRes
        private final int fragmentId;
        private final AccountsElementCore accountsElement;
        private final Mutable<Optional<BigDecimal>> newAccountOptionalAmount;
        private final Supplier<BalanceAccount> accountSupplier;

        private Feedbacker(int fragmentId, AccountsElementCore accountsElement, Mutable<Optional<BigDecimal>> newAccountOptionalAmount, Supplier<BalanceAccount> accountSupplier) {
            this.fragmentId = fragmentId;
            this.accountsElement = accountsElement;
            this.newAccountOptionalAmount = newAccountOptionalAmount;
            this.accountSupplier = accountSupplier;
        }

        @Override
        public void performFeedback(CoreElementActivity activity) {
            activity.textViewFeedback(accountsElement.getName(), fragmentId, R.id.accounts_name_input);
            activity.currenciesSpinnerFeedback(accountsElement.getUnit(), fragmentId, R.id.accounts_currency_input);
            activity.decimalTextViewFeedback(newAccountOptionalAmount.object.get(), fragmentId, R.id.accounts_amount_optional_input);
            activity.hintedArraySpinnerFeedback(accountSupplier.get(), fragmentId, R.id.accounts_spinner);
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
                core.setAccount(accountsElement.getName());
                core.setAmountDecimal(dec);
                //noinspection ConstantConditions
                core.setAmountUnit(accountsElement.getUnit());
                return core.submit();
            }

            return accountResult;
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
                        return new BalanceAccountContainer(balanceAccount);
                    }
                }
        );

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
        UiUtils.makeButtonSquaredByHeight(addButton);
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
        activity.addSubmitFragmentInfo(id, submitButton, BUTTON_NEW_ACCOUNT_SUBMIT, new Consumer<BalanceAccount>() {
            @Override
            public void accept(BalanceAccount account) {
                nameInput.setVisibility(View.GONE);
                nameInputInfo.setVisibility(View.GONE);
                currencyInput.setVisibility(View.GONE);
                currencyInputInfo.setVisibility(View.GONE);
                amountInput.setVisibility(View.GONE);
                amountInputHint.setVisibility(View.GONE);
                amountInputInfo.setVisibility(View.GONE);
                submitButton.setVisibility(View.GONE);
                addButton.setVisibility(View.VISIBLE);
                UiUtils.addToHintedSpinner(account, accountsSpinner, BalanceAccountContainer.FACTORY);
                inflated.invalidate();
            }
        });

        return inflated;
    }

    private static final class Mutable<T> {

        private T object;

        private boolean lockOn = false;

        private Mutable(T object) {
            this.object = object;
        }

    }

}
