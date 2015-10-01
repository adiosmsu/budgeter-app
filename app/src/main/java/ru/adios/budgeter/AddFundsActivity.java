package ru.adios.budgeter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.CoreUtils;
import ru.adios.budgeter.util.MenuUtils;

public class AddFundsActivity extends AppCompatActivity {

    private static final Logger logger = LoggerFactory.getLogger(AddFundsActivity.class);

    private final FundsAdditionElementCore additionElement;
    private final CoreErrorHighlighter errorHighlighter;
    private Money totalBalance;

    {
        additionElement = new FundsAdditionElementCore(Schema.TREASURY);
        final BalanceElementCore balanceElement = new BalanceElementCore(Schema.TREASURY, Constants.CURRENCIES_EXCHANGE_SERVICE);
        balanceElement.setTotalUnit(Units.RUB);
        totalBalance = CoreUtils.getTotalBalance(balanceElement, logger);

        final CoreErrorHighlighter.Builder ehBuilder = new CoreErrorHighlighter.Builder();
        errorHighlighter = ehBuilder.addElementInfo("amountDecimal", R.id.amount_decimal_info)
                .addElementInfo("amountUnit", R.id.amount_currency_info)
                .setGlobalInfoViewId(R.id.add_funds_info)
                .setWorker(R.id.add_funds_info, new CoreErrorHighlighter.ViewWorker() {
                    @Override
                    public void successWork(View view) {
                        final RelativeLayout.LayoutParams pars = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 0);
                        pars.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        pars.addRule(RelativeLayout.BELOW, R.id.amount_decimal_info);
                        view.setLayoutParams(pars);
                    }

                    @Override
                    public void failureWork(View view) {
                        final RelativeLayout.LayoutParams pars = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        pars.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        pars.addRule(RelativeLayout.BELOW, R.id.amount_decimal_info);
                        view.setLayoutParams(pars);
                    }
                })
                .build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_funds);
        coreFeedback();
        CoreNotifier.addLink(this, R.id.amount_decimal, new CoreNotifier.DecimalLinker() {
            @Override
            public void link(BigDecimal data) {
                additionElement.setAmountDecimal(data);
                coreFeedback();
            }
        });
        CoreNotifier.addLink(this, R.id.amount_currency, new CoreNotifier.CurrencyLinker() {
            @Override
            public void link(CurrencyUnit data) {
                additionElement.setAmountUnit(data);
                coreFeedback();
            }
        });
    }

    private void coreFeedback() {
        final BigDecimal decimal = additionElement.getAmountDecimal();
        getAmountDecimalView().setText(decimal.toPlainString());
        final CurrencyUnit amountUnit = additionElement.getAmountUnit();
        getAmountCurrencyView().setText(amountUnit != null ? amountUnit.getCode() : "");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
                errorHighlighter.processSubmitResult(result, AddFundsActivity.this);
            }
        }.doInBackground();
    }

    private TextView getAmountCurrencyView() {
        return (TextView) findViewById(R.id.amount_currency);
    }

    private TextView getAmountDecimalView() {
        return (TextView) findViewById(R.id.amount_decimal);
    }

}
