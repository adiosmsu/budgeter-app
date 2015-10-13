package ru.adios.budgeter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreUtils;
import ru.adios.budgeter.util.UiUtils;

public class AddFundsActivity extends CoreElementActivity<Treasury.BalanceAccount> implements AccountsElementCoreProvider {

    private static final Logger logger = LoggerFactory.getLogger(AddFundsActivity.class);

    private final FundsAdditionElementCore additionElement = new FundsAdditionElementCore(Schema.TREASURY);
    private final CoreErrorHighlighter addFundsErrorHighlighter = new CoreErrorHighlighter();
    private final AccountStandardFragment.InfoProvider accountsInfoProvider = AccountStandardFragment.getInfoProvider(R.id.add_funds_account_fragment, additionElement, addFundsErrorHighlighter);

    private final CollectedFragmentsInfoProvider<Treasury.BalanceAccount> infoProvider =
            new CollectedFragmentsInfoProvider.Builder<>(this)
                    .addProvider(EnterAmountFragment.getInfoProvider(R.id.add_funds_amount_fragment, additionElement, addFundsErrorHighlighter))
                    .addProvider(accountsInfoProvider)
                    .build();

    private Money totalBalance;

    @Override
    public AccountsElementCore getAccountsElementCore() {
        return accountsInfoProvider.accountsElement;
    }

    @Override
    protected FragmentsInfoProvider<Treasury.BalanceAccount> getInfoProvider() {
        return infoProvider;
    }

    @Override
    protected final int getLayoutId() {
        return R.layout.activity_add_funds;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BalanceElementCore balanceElement = new BalanceElementCore(Schema.TREASURY, Constants.CURRENCIES_EXCHANGE_SERVICE);
        balanceElement.setTotalUnit(Units.RUB);
        totalBalance = CoreUtils.getTotalBalance(balanceElement, logger);

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
    public final boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_funds, menu);

        UiUtils.fillStandardMenu(menu, totalBalance);

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
        new AsyncTask<Void, Void, Submitter.Result<Treasury.BalanceAccount>>() {
            @Override
            protected Submitter.Result<Treasury.BalanceAccount> doInBackground(Void... params) {
                return additionElement.submit();
            }

            @Override
            protected void onPostExecute(Submitter.Result<Treasury.BalanceAccount> result) {
                addFundsErrorHighlighter.processSubmitResult(result);
                if (result.isSuccessful()) {
                    final Spinner accountsSpinner = (Spinner) findViewById(R.id.accounts_spinner);
                    UiUtils.replaceAccountInSpinner(result.submitResult, accountsSpinner);
                }
                findViewById(R.id.activity_add_funds).invalidate();
            }
        }.execute();
    }


}
