package ru.adios.budgeter;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.common.collect.ImmutableList;

import java.text.DecimalFormat;
import java.util.List;

import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import ru.adios.budgeter.api.CurrencyExchangeEventRepository;
import ru.adios.budgeter.api.FundsMutationEventRepository;
import ru.adios.budgeter.api.Order;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.api.RepoOption;
import ru.adios.budgeter.api.data.CurrencyExchangeEvent;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.util.BalancedMenuHandler;
import ru.adios.budgeter.util.BalancesUiThreadState;
import ru.adios.budgeter.util.DataTableLayout;
import ru.adios.budgeter.util.Formatting;
import ru.adios.budgeter.util.UiUtils;

public class HomeActivity extends AppCompatActivity {

    static {
        UiUtils.onApplicationStart();
    }

    private static final int TABLE_ROWS = 5;
    private static final OrderBy<FundsMutationEventRepository.Field> MUTATIONS_ORDER_BY_TIMESTAMP = new OrderBy<>(FundsMutationEventRepository.Field.TIMESTAMP, Order.DESC);
    private static final OrderBy<CurrencyExchangeEventRepository.Field> EXCHANGES_ORDER_BY_TIMESTAMP = new OrderBy<>(CurrencyExchangeEventRepository.Field.TIMESTAMP, Order.DESC);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.####");

    private BalancedMenuHandler menuHandler;
    private DataTableLayout mutationsTable;
    private DataTableLayout exchangesTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Resources resources = getResources();
        final ImmutableList<String> mutationsHeader = ImmutableList.of(
                resources.getString(R.string.ah_ops_table_col_time),
                resources.getString(R.string.ah_ops_table_col_subj),
                resources.getString(R.string.ah_ops_table_col_money),
                resources.getString(R.string.ah_ops_table_col_account)
        );
        final ImmutableList<String> exchangesHeader = ImmutableList.of(
                resources.getString(R.string.ah_ops_table_col_time),
                resources.getString(R.string.ah_exchanges_table_col_bought),
                resources.getString(R.string.ah_exchanges_table_col_sold),
                resources.getString(R.string.ah_exchanges_table_col_b_acc),
                resources.getString(R.string.ah_exchanges_table_col_s_acc),
                resources.getString(R.string.ah_exchanges_table_col_rate)
        );

        setContentView(R.layout.activity_home); //draw
        final RelativeLayout mainLayout = (RelativeLayout) findViewById(R.id.activity_home);

        initMenuHandler((LinearLayout) findViewById(R.id.ah_funds_list)); // Register balances state listener

        mutationsTable = new DataTableLayout(this, new DataTableLayout.DataStore() {
            @Override
            public List<Iterable<String>> loadData(RepoOption... options) {
                return BundleProvider.getBundle()
                        .fundsMutationEvents()
                        .streamMutationEvents(options)
                        .map(new Function<FundsMutationEvent, Iterable<String>>() {
                            @Override
                            public Iterable<String> apply(FundsMutationEvent event) {
                                return ImmutableList.of(
                                        Formatting.toStringRusDateTimeShort(event.timestamp),
                                        event.subject.name,
                                        Formatting.toStringMoneyUsingSign(event.amount, resources),
                                        event.relevantBalance.name
                                );
                            }
                        })
                        .collect(Collectors.<Iterable<String>>toList());
            }

            @Override
            public int count() {
                return TABLE_ROWS;
            }

            @Override
            public List<String> getDataHeaders() {
                return mutationsHeader;
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
        mainLayout.addView(mutationsTable);

        exchangesTable = new DataTableLayout(this, new DataTableLayout.DataStore() {
            @Override
            public List<Iterable<String>> loadData(RepoOption... options) {
                return BundleProvider.getBundle()
                        .currencyExchangeEvents()
                        .streamExchangeEvents(options)
                        .map(new Function<CurrencyExchangeEvent, Iterable<String>>() {
                            @Override
                            public Iterable<String> apply(CurrencyExchangeEvent event) {
                                return ImmutableList.of(
                                        Formatting.toStringRusDateTimeShort(event.timestamp),
                                        Formatting.toStringMoneyUsingSign(event.bought, resources),
                                        Formatting.toStringMoneyUsingSign(event.sold, resources),
                                        event.boughtAccount.name,
                                        event.soldAccount.name,
                                        DECIMAL_FORMAT.format(event.rate)
                                );
                            }
                        })
                        .collect(Collectors.<Iterable<String>>toList());
            }

            @Override
            public int count() {
                return TABLE_ROWS;
            }

            @Override
            public List<String> getDataHeaders() {
                return exchangesHeader;
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
        exchangesParams.addRule(RelativeLayout.BELOW, mutationsTable.getId());
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
        mainLayout.addView(exchangesTable);

        BalancesUiThreadState.instantiate(); // this is the first activity so...

        mutationsTable.start();
        exchangesTable.start();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        onResumeOrRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        onResumeOrRestart();
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

    /** Called when user clicks on Exchange button */
    public void startExchangeCurrenciesActivity(View view) {
        startActivity(new Intent(this, ExchangeCurrenciesActivity.class));
    }

    private boolean initMenuHandler(final LinearLayout fundsLayout) {
        if (menuHandler == null) {
            menuHandler = new BalancedMenuHandler(getResources(), new Consumer<BalancesUiThreadState.Pair>() {
                @Override
                public void accept(BalancesUiThreadState.Pair pair) {
                    UiUtils.refillLinearLayoutWithBalances(fundsLayout, pair.balances, pair.totalBalance, HomeActivity.this);
                }
            });
            menuHandler.init(this);
            return true;
        }
        return false;
    }

    private void onResumeOrRestart() {
        if (menuHandler == null) {
            mutationsTable.repopulate();
            exchangesTable.repopulate();
        }
        resumeMenuHandler();
    }

    private void resumeMenuHandler() {
        final LinearLayout fundsLayout = (LinearLayout) findViewById(R.id.ah_funds_list);
        if (initMenuHandler(fundsLayout)) {
            final BalancesUiThreadState.Pair pair = BalancesUiThreadState.getSnapshot();
            UiUtils.refillLinearLayoutWithBalances(fundsLayout, pair.balances, pair.totalBalance, this);
        }
    }

}
