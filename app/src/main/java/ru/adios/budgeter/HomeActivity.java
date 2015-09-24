package ru.adios.budgeter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.joda.money.Money;

import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.inmemrepo.Schema;

public class HomeActivity extends AppCompatActivity {

    public static final int FUNDS_ID = Integer.MAX_VALUE - 1;

    static {
        Schema.TREASURY.registerBalanceAccount(new Treasury.BalanceAccount("Тест", Units.RUB));
    }

    private final BalanceElementCore balanceElement = new BalanceElementCore(Schema.TREASURY, Constants.CURRENCIES_EXCHANGE_SERVICE);
    {
        balanceElement.setTotalUnit(Units.RUB);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);

        // Get funds
        final Money totalBalance = balanceElement.getTotalBalance();

        // Add funds info
        final int settingsOrder = menu.getItem(0).getOrder();
        final MenuItem fundsInfo = menu.add(Menu.NONE, FUNDS_ID, settingsOrder - 1, totalBalance.toString());
        fundsInfo.setTitleCondensed(totalBalance.getAmount().toString());
        fundsInfo.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
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

}
