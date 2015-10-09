package ru.adios.budgeter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.common.collect.ImmutableMap;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import java8.util.Optional;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.CoreUtils;
import ru.adios.budgeter.util.MenuUtils;

public class AddFundsActivity extends CoreElementActivity implements AccountsElementCoreProvider {

    private static final Logger logger = LoggerFactory.getLogger(AddFundsActivity.class);

    private static final int[] ALLOWED_FRAGMENT = new int[] {R.id.add_funds_fragment, R.id.add_funds_account_fragment};


    private final FundsAdditionElementCore additionElement = new FundsAdditionElementCore(Schema.TREASURY);
    private final CoreErrorHighlighter addFundsErrorHighlighter = new CoreErrorHighlighter();
    private final ImmutableMap<String, CoreElementFieldInfo> addFundsFieldInfoMap = ImmutableMap.<String, CoreElementFieldInfo>builder()
            .put(AccountStandardFragment.FIELD_ACCOUNT, new CoreElementFieldInfo(FundsAdditionElementCore.FIELD_ACCOUNT, new CoreNotifier.ArbitraryLinker() {
                @Override
                public void link(Object data) {
                    final Treasury.BalanceAccount account = (Treasury.BalanceAccount) data;
                    additionElement.setAccount(account);
                }
            }))
            .put(EnterAmountFragment.FIELD_AMOUNT_DECIMAL, new CoreElementFieldInfo(FundsAdditionElementCore.FIELD_AMOUNT_DECIMAL, new CoreNotifier.DecimalLinker() {
                @Override
                public void link(BigDecimal data) {
                    additionElement.setAmountDecimal(data);
                }
            }))
            .put(EnterAmountFragment.FIELD_AMOUNT_CURRENCY, new CoreElementFieldInfo(FundsAdditionElementCore.FIELD_AMOUNT_UNIT, new CoreNotifier.CurrencyLinker() {
                @Override
                public void link(CurrencyUnit data) {
                    additionElement.setAmountUnit(data);
                }
            }))
            .build();

    private Optional<BigDecimal> newAccountOptionalAmount = Optional.of(BigDecimal.ZERO);
    private final AccountsElementCore accountsElement = new AccountsElementCore(Schema.TREASURY);
    private final CoreErrorHighlighter accountsErrorHighlighter = new CoreErrorHighlighter();
    private final ImmutableMap<String, CoreElementFieldInfo> accountsFieldInfoMap = ImmutableMap.<String, CoreElementFieldInfo>builder()
            .put(AccountStandardFragment.FIELD_NEW_ACCOUNT_NAME, new CoreElementFieldInfo(AccountsElementCore.FIELD_NAME, new CoreNotifier.TextLinker() {
                @Override
                public void link(String data) {
                    accountsElement.setName(data);
                }
            }))
            .put(AccountStandardFragment.FIELD_NEW_ACCOUNT_CURRENCY, new CoreElementFieldInfo(AccountsElementCore.FIELD_UNIT, new CoreNotifier.CurrencyLinker() {
                @Override
                public void link(CurrencyUnit data) {
                    accountsElement.setUnit(data);
                }
            }))
            .put(AccountStandardFragment.FIELD_NEW_ACCOUNT_AMOUNT, new CoreElementFieldInfo(FundsAdditionElementCore.FIELD_AMOUNT_DECIMAL, new CoreNotifier.DecimalLinker() {
                @Override
                public void link(BigDecimal data) {
                    setNewAccountOptionalAmount(data);
                }
            }))
            .build();
    private final HybridAccountCore hybridAccountCore = new HybridAccountCore();

    private Money totalBalance;

    private void setNewAccountOptionalAmount(BigDecimal amount) {
        newAccountOptionalAmount = Optional.of(amount);
    }

    @Override
    public AccountsElementCore getAccountsElementCore() {
        return accountsElement;
    }

    @Override
    protected final int getLayoutId() {
        return R.layout.activity_add_funds;
    }

    @Override
    protected final CoreErrorHighlighter getErrorHighlighter(@IdRes int fragmentId) {
        switch (fragmentId) {
            case R.id.add_funds_fragment:
                return addFundsErrorHighlighter;
            case R.id.add_funds_account_fragment:
                return accountsErrorHighlighter;
            default:
                throw unsupportedFragmentError(fragmentId);
        }
    }

    @Override
    protected CoreElementSubmitInfo getSubmitInfo(@IdRes int fragmentId, String buttonName) {
        switch (fragmentId) {
            case R.id.add_funds_fragment:
                return new CoreElementSubmitInfo(additionElement, null);
            case R.id.add_funds_account_fragment:
                return new CoreElementSubmitInfo(hybridAccountCore, null);
            default:
                throw unsupportedFragmentError(fragmentId);
        }
    }

    @Override
    protected final int[] allowedFragments() {
        return ALLOWED_FRAGMENT;
    }

    @Nullable
    @Override
    protected final CoreElementFieldInfo getCoreElementFieldInfo(@IdRes int fragmentId, String fragmentFieldName) {
        switch (fragmentId) {
            case R.id.add_funds_fragment:
                return addFundsFieldInfoMap.get(fragmentFieldName);
            case R.id.add_funds_account_fragment:
                return accountsFieldInfoMap.get(fragmentFieldName);
            default:
                throw unsupportedFragmentError(fragmentId);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BalanceElementCore balanceElement = new BalanceElementCore(Schema.TREASURY, Constants.CURRENCIES_EXCHANGE_SERVICE);
        balanceElement.setTotalUnit(Units.RUB);
        totalBalance = CoreUtils.getTotalBalance(balanceElement, logger);

        final View infoView = findViewById(R.id.add_funds_info);
        addFundsErrorHighlighter.setGlobalInfoView(infoView);
        addFundsErrorHighlighter.setWorker(infoView, new CoreErrorHighlighter.ViewWorker() {
            @Override
            public void successWork(View view) {
                setLayoutParams(view, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 0));
            }

            @Override
            public void failureWork(View view) {
                setLayoutParams(view, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
            }

            private void setLayoutParams(View view, RelativeLayout.LayoutParams pars) {
                pars.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                pars.addRule(RelativeLayout.BELOW, R.id.add_funds_fragment);
                view.setLayoutParams(pars);
            }
        });
    }

    @Override
    protected void coreFeedbackInternal() {
        accountSpinnerFeedback(additionElement.getAccount(), R.id.accounts_spinner);
        decimalTextViewFeedback(additionElement.getAmountDecimal(), R.id.amount_decimal);
        currenciesSpinnerFeedback(additionElement.getAmountUnit(), R.id.amount_currency);
        textViewFeedback(accountsElement.getName(), R.id.accounts_name_input);
        currenciesSpinnerFeedback(accountsElement.getUnit(), R.id.accounts_currency_input);
        decimalTextViewFeedback(newAccountOptionalAmount.get(), R.id.accounts_amount_optional_input);
    }

    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_funds, menu);

        MenuUtils.fillStandardMenu(menu, totalBalance);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void addFunds(View view) {
        new AsyncTask<Void, Void, Submitter.Result>() {
            @Override
            protected Submitter.Result doInBackground(Void... params) {
                return additionElement.submit();
            }

            @Override
            protected void onPostExecute(Submitter.Result result) {
                addFundsErrorHighlighter.processSubmitResult(result);
                findViewById(R.id.activity_add_funds).invalidate();
            }
        }.doInBackground();
    }

    private IllegalArgumentException unsupportedFragmentError(@IdRes int fragmentId) {
        return new IllegalArgumentException("Unsupported fragment: " + getResources().getResourceName(fragmentId));
    }

    private final class HybridAccountCore implements Submitter {

        @Override
        public Result submit() {
            final Result accountResult = accountsElement.submit();

            if (!accountResult.isSuccessful()) {
                return accountResult;
            }

            if (!newAccountOptionalAmount.get().equals(BigDecimal.ZERO)) {
                final FundsAdditionElementCore core = new FundsAdditionElementCore(Schema.TREASURY);
                core.setAccount(accountsElement.getName());
                core.setAmountDecimal(newAccountOptionalAmount.get());
                //noinspection ConstantConditions
                core.setAmountUnit(accountsElement.getUnit());
                return core.submit();
            }

            return Result.SUCCESS;
        }

    }

}
