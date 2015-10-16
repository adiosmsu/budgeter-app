package ru.adios.budgeter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import java8.util.function.Consumer;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.util.BalancedMenuHandler;
import ru.adios.budgeter.util.BalancesUiThreadState;
import ru.adios.budgeter.util.UiUtils;

public class HomeActivity extends AppCompatActivity {

    static {
        UiUtils.onApplicationStart();
        Schema.TREASURY.registerBalanceAccount(new Treasury.BalanceAccount("Тест", Units.RUB));
    }

    private BalancedMenuHandler menuHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home); //draw

        // Retrieve funds layout
        final LinearLayout fundsLayout = (LinearLayout) findViewById(R.id.ah_funds_list);

        // Register balances state listener
        initMenuHandler(fundsLayout);

        BalancesUiThreadState.instantiate(); // this is the first activity so...
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        resumeMenuHandler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeMenuHandler();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        menuHandler.onCreateMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        menuHandler.destroy();
        menuHandler = null;
    }

    /** Called when user clicks on Add Funds button */
    public void startAddFundsActivity(View view) {
        startActivity(new Intent(this, AddFundsActivity.class));
    }

    /** Called when user clicks on Mutate Funds button */
    public void startFundsMutationActivity(View view) {
        startActivity(new Intent(this, FundsMutationActivity.class));
    }

    private boolean initMenuHandler(final LinearLayout fundsLayout) {
        if (menuHandler == null) {
            menuHandler = new BalancedMenuHandler(new Consumer<BalancesUiThreadState.Pair>() {
                @Override
                public void accept(BalancesUiThreadState.Pair pair) {
                    UiUtils.refillLinearLayout(fundsLayout, pair.balances, pair.totalBalance, HomeActivity.this);
                }
            });
            menuHandler.init(this);
            return true;
        }
        return false;
    }

    private void resumeMenuHandler() {
        final LinearLayout fundsLayout = (LinearLayout) findViewById(R.id.ah_funds_list);
        if (initMenuHandler(fundsLayout)) {
            final BalancesUiThreadState.Pair pair = BalancesUiThreadState.getSnapshot();
            UiUtils.refillLinearLayout(fundsLayout, pair.balances, pair.totalBalance, this);
        }
    }

}
