package ru.adios.budgeter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java8.util.function.Consumer;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.util.CoreUtils;
import ru.adios.budgeter.util.Formatting;
import ru.adios.budgeter.util.MenuUtils;

public class HomeActivity extends AppCompatActivity {

    private static final Logger logger = LoggerFactory.getLogger(HomeActivity.class);

    static {
        Schema.TREASURY.registerBalanceAccount(new Treasury.BalanceAccount("Тест", Units.RUB));
    }

    private final BalanceElementCore balanceElement = new BalanceElementCore(Schema.TREASURY, Constants.CURRENCIES_EXCHANGE_SERVICE);

    private Money totalBalance;

    {
        balanceElement.setTotalUnit(Units.RUB);
        totalBalance = CoreUtils.getTotalBalance(balanceElement, logger);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home); //draw

        // Retrieve funds layout
        final LinearLayout fundsLayout = (LinearLayout) findViewById(R.id.ah_funds_list);

        // Modify it so it will show balances
        final Integer[] maxLine = new Integer[1];
        maxLine[0] = 0;
        balanceElement.streamIndividualBalances().forEach(new Consumer<Money>() {
            @Override
            public void accept(Money money) {
                final String str = '\t' + Formatting.toStringMoneyUsingText(money);
                final int l = str.length();
                if (l > maxLine[0]) {
                    maxLine[0] = l;
                }
                fundsLayout.addView(getNewLineView(str));
            }
        });
        if (maxLine[0] != 0) {
            fundsLayout.addView(getStrSeparatorLine(maxLine[0]));
        }
        fundsLayout.addView(getNewLineView(getResources().getText(R.string.total).toString() + ' ' + Formatting.toStringMoneyUsingText(totalBalance)));
    }

    private TextView getStrSeparatorLine(int ml) {
        final StringBuilder sb = new StringBuilder(ml + 1);
        for (int i = 0; i < ml; i++) {
            sb.append('_');
        }
        final String s = sb.toString();
        return getNewLineView(s);
    }

    private TextView getNewLineView(String str) {
        final TextView line = new TextView(HomeActivity.this);
        line.setText(str);
        line.setId(ElementsIdProvider.getNextId());
        line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return line;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);

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

    /** Called when user clicks on Add Funds button */
    public void startAddFundsActivity(View view) {
        startActivity(new Intent(this, AddFundsActivity.class));
    }

}
