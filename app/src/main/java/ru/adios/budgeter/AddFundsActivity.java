package ru.adios.budgeter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.collect.ImmutableMap;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.CoreUtils;
import ru.adios.budgeter.util.MenuUtils;

public class AddFundsActivity extends CoreElementActivity {

    private static final Logger logger = LoggerFactory.getLogger(AddFundsActivity.class);
    private static final int[] ALLOWED_FRAGMENT = new int[] {R.id.add_funds_fragment};


    private final FundsAdditionElementCore additionElement = new FundsAdditionElementCore(Schema.TREASURY);
    private final CoreErrorHighlighter errorHighlighter = new CoreErrorHighlighter();
    private final ImmutableMap<String, CoreElementFieldInfo> fieldInfoMap = ImmutableMap.<String, CoreElementFieldInfo>builder()
            .put(EnterAmountFragment.FIELD_AMOUNT_DECIMAL, new CoreElementFieldInfo("amountDecimal", new CoreNotifier.DecimalLinker() {
                @Override
                public void link(BigDecimal data) {
                    additionElement.setAmountDecimal(data);
                }
            }))
            .put(EnterAmountFragment.FIELD_AMOUNT_CURRENCY, new CoreElementFieldInfo("amountUnit", new CoreNotifier.CurrencyLinker() {
                @Override
                public void link(CurrencyUnit data) {
                    additionElement.setAmountUnit(data);
                }
            }))
            .build();

    private Money totalBalance;

    @Override
    protected final int getLayoutId() {
        return R.layout.activity_add_funds;
    }

    @Override
    protected final CoreErrorHighlighter getErrorHighlighter() {
        return errorHighlighter;
    }

    @Override
    protected final int[] allowedFragments() {
        return ALLOWED_FRAGMENT;
    }

    @Nullable
    @Override
    protected final CoreElementFieldInfo getCoreElementFieldInfo(@IdRes int fragmentId, String fragmentFieldName) {
        return fieldInfoMap.get(fragmentFieldName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BalanceElementCore balanceElement = new BalanceElementCore(Schema.TREASURY, Constants.CURRENCIES_EXCHANGE_SERVICE);
        balanceElement.setTotalUnit(Units.RUB);
        totalBalance = CoreUtils.getTotalBalance(balanceElement, logger);

        final View infoView = findViewById(R.id.add_funds_info);
        errorHighlighter.setGlobalInfoView(infoView);
        errorHighlighter.setWorker(infoView, new CoreErrorHighlighter.ViewWorker() {
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
        final BigDecimal decimal = additionElement.getAmountDecimal();
        getAmountDecimalView().setText(decimal.toPlainString());
        final CurrencyUnit amountUnit = additionElement.getAmountUnit();
        if (amountUnit != null) {
            getAmountCurrencyView().setSelection(Constants.getCurrencyDropdownPosition(amountUnit), true);
        }
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
                errorHighlighter.processSubmitResult(result);
            }
        }.doInBackground();
    }

    private Spinner getAmountCurrencyView() {
        return (Spinner) findViewById(R.id.amount_currency);
    }

    private TextView getAmountDecimalView() {
        return (TextView) findViewById(R.id.amount_decimal);
    }

}
