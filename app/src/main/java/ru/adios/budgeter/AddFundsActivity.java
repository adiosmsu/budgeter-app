package ru.adios.budgeter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.math.BigDecimal;

import java8.util.function.Supplier;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.util.BalancesUiThreadState;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.HintedArrayAdapter;
import ru.adios.budgeter.util.UiUtils;

public class AddFundsActivity extends CoreElementActivity<Treasury.BalanceAccount> implements AccountsElementCoreProvider {

    private final FundsAdditionElementCore additionElement = new FundsAdditionElementCore(Schema.TREASURY);
    private final CoreErrorHighlighter addFundsErrorHighlighter = new CoreErrorHighlighter();
    private final AccountStandardFragment.InfoProvider accountsInfoProvider =
            AccountStandardFragment.getInfoProviderBuilder(R.id.add_funds_account_fragment, this, new Supplier<Treasury.BalanceAccount>() {
                @Override
                public Treasury.BalanceAccount get() {
                    return additionElement.getAccount();
                }
            })
                    .provideAccountFieldInfo(FundsAdditionElementCore.FIELD_ACCOUNT, addFundsErrorHighlighter, new CoreNotifier.HintedLinker() {
                        @Override
                        public void link(HintedArrayAdapter.ObjectContainer data) {
                            additionElement.setAccount((Treasury.BalanceAccount) data.getObject());
                        }
                    })
                    .build();

    private final CollectedFragmentsInfoProvider<Treasury.BalanceAccount> infoProvider =
            new CollectedFragmentsInfoProvider.Builder<>(this)
                    .addProvider(EnterAmountFragment.getInfoProvider(
                            R.id.add_funds_amount_fragment,
                            additionElement,
                            addFundsErrorHighlighter,
                            FundsAdditionElementCore.FIELD_AMOUNT_DECIMAL,
                            FundsAdditionElementCore.FIELD_AMOUNT_UNIT))
                    .addProvider(accountsInfoProvider)
                    .build();

    @Override
    public final AccountsElementCore getAccountsElementCore() {
        return accountsInfoProvider.accountsElement;
    }

    @Override
    public final Class<Treasury.BalanceAccount> provideClassForChecking() {
        return Treasury.BalanceAccount.class;
    }

    @Override
    protected final FragmentsInfoProvider<Treasury.BalanceAccount> getInfoProvider() {
        return infoProvider;
    }

    @Override
    @LayoutRes
    protected final int getLayoutId() {
        return R.layout.activity_add_funds;
    }

    @Override
    @MenuRes
    protected final int getMenuId() {
        return R.menu.menu_add_funds;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final View infoView = findViewById(R.id.add_funds_info);
        setGlobalViewToHighlighter(accountsInfoProvider.accountsErrorHighlighter, infoView);
        setGlobalViewToHighlighter(addFundsErrorHighlighter, infoView);
    }

    private static void setGlobalViewToHighlighter(CoreErrorHighlighter highlighter, View infoView) {
        highlighter.setGlobalInfoView(infoView);
        highlighter.setWorker(infoView, new CoreErrorHighlighter.ViewWorker() {
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
                pars.addRule(RelativeLayout.BELOW, R.id.add_funds_amount_fragment);
                view.setLayoutParams(pars);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void addFunds(View view) {
        final BigDecimal decimal = additionElement.getAmountDecimal();
        final CurrencyUnit amountUnit = additionElement.getAmountUnit();

        new AsyncTask<FundsAdditionElementCore, Void, Submitter.Result<Treasury.BalanceAccount>>() {
            @Override
            protected Submitter.Result<Treasury.BalanceAccount> doInBackground(FundsAdditionElementCore[] params) {
                return params[0].submit();
            }

            @Override
            protected void onPostExecute(Submitter.Result<Treasury.BalanceAccount> result) {
                addFundsErrorHighlighter.processSubmitResult(result);
                if (result.isSuccessful()) {
                    final Spinner accountsSpinner = (Spinner) findViewById(R.id.accounts_spinner);
                    UiUtils.replaceAccountInSpinner(result.submitResult, accountsSpinner);
                    //noinspection ConstantConditions
                    BalancesUiThreadState.addMoney(Money.of(amountUnit, decimal), AddFundsActivity.this);
                }
                findViewById(R.id.activity_add_funds).invalidate();
            }
        }.execute(additionElement);
    }

}
