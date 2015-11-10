package ru.adios.budgeter.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.annotation.UiThread;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java8.util.Optional;
import java8.util.function.Consumer;
import ru.adios.budgeter.BalancesUiThreadState;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.FundsAwareMenuActivity;
import ru.adios.budgeter.R;
import ru.adios.budgeter.api.CurrencyExchangeEventRepository;
import ru.adios.budgeter.api.FundsMutationEventRepository;
import ru.adios.budgeter.api.Order;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.util.UiUtils;
import ru.adios.budgeter.widgets.DataTableLayout;
import ru.adios.budgeter.widgets.ExchangesDataStore;
import ru.adios.budgeter.widgets.MutationEventsDataStore;

import static com.google.common.base.Preconditions.checkNotNull;

@UiThread
public class HomeActivity extends FundsAwareMenuActivity {

    static {
        UiUtils.onApplicationStart();
    }

    private static final int TABLE_ROWS = 5;
    private static final OrderBy<FundsMutationEventRepository.Field> MUTATIONS_ORDER_BY_TIMESTAMP = new OrderBy<>(FundsMutationEventRepository.Field.TIMESTAMP, Order.DESC);
    private static final OrderBy<CurrencyExchangeEventRepository.Field> EXCHANGES_ORDER_BY_TIMESTAMP = new OrderBy<>(CurrencyExchangeEventRepository.Field.TIMESTAMP, Order.DESC);

    private DataTableLayout mutationsTable;
    private DataTableLayout exchangesTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final RelativeLayout mainLayout = (RelativeLayout) findViewById(R.id.activity_home);
        View btnMu = null, btnEx = null;
        int muBtnIx = 0, exBtnIx = 0;
        for (int i = 0; i < mainLayout.getChildCount(); i++) {
            final View childAt = mainLayout.getChildAt(i);
            final int id = childAt.getId();
            if (id == R.id.ah_mutations_table_button) {
                muBtnIx = i;
                btnMu = childAt;
            }
            if (id == R.id.ah_exchanges_table_button) {
                exBtnIx = i;
                btnEx = childAt;
            }
        }
        checkNotNull(btnMu);
        checkNotNull(btnEx);

        final Resources resources = getResources();
        mutationsTable = new DataTableLayout(this, new MutationEventsDataStore(resources) {
            @Override
            public int count() {
                return (BundleProvider.getBundle().fundsMutationEvents().countMutationEvents() == 0)
                        ? 0
                        : TABLE_ROWS;
            }

            @Override
            public Optional<Integer> getMaxWidthForData(int index) {
                if (index == 1) {
                    return Optional.of(50);
                }
                return Optional.empty();
            }
        });
        final RelativeLayout.LayoutParams mutationsParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        mutationsParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mutationsParams.addRule(RelativeLayout.BELOW, R.id.ah_exchange_button);
        mutationsTable.setLayoutParams(mutationsParams);
        mutationsTable.setTableName(resources.getString(R.string.ah_ops_table_header));
        mutationsTable.setPageSize(TABLE_ROWS);
        mutationsTable.setOrderBy(MUTATIONS_ORDER_BY_TIMESTAMP);
        mutationsTable.setVisibility(View.GONE);
        mutationsTable.setOnDataLoadedListener(new Consumer<DataTableLayout>() {
            @Override
            public void accept(DataTableLayout dataTableLayout) {
                dataTableLayout.setVisibility(View.VISIBLE);
            }
        });
        mainLayout.addView(mutationsTable, muBtnIx);
        final RelativeLayout.LayoutParams mbParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        mbParams.addRule(RelativeLayout.BELOW, mutationsTable.getId());
        mbParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        btnMu.setLayoutParams(mbParams);

        exchangesTable = new DataTableLayout(this, new ExchangesDataStore(resources) {
            @Override
            public int count() {
                return (BundleProvider.getBundle().currencyExchangeEvents().countExchangeEvents() == 0)
                        ? 0
                        : TABLE_ROWS;
            }

            @Override
            public Optional<Integer> getMaxWidthForData(int index) {
                if (index == 3 || index == 4) {
                    return Optional.of(100);
                }
                return Optional.empty();
            }
        });
        final RelativeLayout.LayoutParams exchangesParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        exchangesParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        exchangesParams.addRule(RelativeLayout.BELOW, R.id.ah_mutations_table_button);
        exchangesTable.setLayoutParams(exchangesParams);
        exchangesTable.setTableName(resources.getString(R.string.ah_exchanges_table_header));
        exchangesTable.setPageSize(TABLE_ROWS);
        exchangesTable.setOrderBy(EXCHANGES_ORDER_BY_TIMESTAMP);
        exchangesTable.setVisibility(View.GONE);
        exchangesTable.setOnDataLoadedListener(new Consumer<DataTableLayout>() {
            @Override
            public void accept(DataTableLayout dataTableLayout) {
                dataTableLayout.setVisibility(View.VISIBLE);
            }
        });
        mainLayout.addView(exchangesTable, exBtnIx + 1);
        final RelativeLayout.LayoutParams ebParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        ebParams.addRule(RelativeLayout.BELOW, exchangesTable.getId());
        ebParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        btnEx.setLayoutParams(ebParams);

        BalancesUiThreadState.instantiate(); // this is the first activity so...

        mutationsTable.start();
        exchangesTable.start();
    }

    @Override
    @MenuRes
    protected int getMenuResource() {
        return R.menu.menu_home;
    }

    @Override
    @LayoutRes
    protected int getLayoutId() {
        return R.layout.activity_home;
    }

    @Override
    protected Consumer<BalancesUiThreadState.Pair> getMenuHandlerListener() {
        return new Consumer<BalancesUiThreadState.Pair>() {
            @Override
            public void accept(BalancesUiThreadState.Pair pair) {
                UiUtils.refillLinearLayoutWithBalances((LinearLayout) findViewById(R.id.ah_funds_list), pair.balances, pair.totalBalance, HomeActivity.this);
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Called when user clicks on Add Funds button */
    public void startAddFundsActivity(View view) {
        startActivity(new Intent(this, AddFundsActivity.class));
    }

    /** Called when user clicks on Add Price button */
    public void startAddPriceActivity(View view) {
        startActivity(new Intent(this, AddPriceActivity.class));
    }

    /** Called when user clicks on Mutate Funds button */
    public void startFundsMutationActivity(View view) {
        startActivity(new Intent(this, FundsMutationActivity.class));
    }

    /** Called when user clicks on Exchange button */
    public void startExchangeCurrenciesActivity(View view) {
        startActivity(new Intent(this, ExchangeCurrenciesActivity.class));
    }

    /** Called when user clicks on View Mutations button */
    public void startMutationsTableActivity(View view) {
        startActivity(new Intent(this, MutationsTableActivity.class));
    }

    /** Called when user clicks on View exchanges button */
    public void startExchangesTableActivity(View view) {
        startActivity(new Intent(this, ExchangesTableActivity.class));
    }

    @Override
    protected void onResumeOrRestart() {
        mutationsTable.repopulate();
        exchangesTable.repopulate();
        final BalancesUiThreadState.Pair pair = BalancesUiThreadState.getSnapshot();
        UiUtils.refillLinearLayoutWithBalances((LinearLayout) findViewById(R.id.ah_funds_list), pair.balances, pair.totalBalance, this);
    }

}
