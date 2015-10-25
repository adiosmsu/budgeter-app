package ru.adios.budgeter;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.threeten.bp.format.DateTimeFormatter;

import java.text.DecimalFormat;
import java.util.List;

import javax.annotation.Nonnull;

import java8.util.function.Consumer;
import java8.util.stream.Collectors;
import ru.adios.budgeter.api.CurrencyExchangeEvent;
import ru.adios.budgeter.api.CurrencyExchangeEventRepository;
import ru.adios.budgeter.api.FundsMutationEvent;
import ru.adios.budgeter.api.FundsMutationEventRepository;
import ru.adios.budgeter.api.OptLimit;
import ru.adios.budgeter.api.Order;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.util.BalancedMenuHandler;
import ru.adios.budgeter.util.BalancesUiThreadState;
import ru.adios.budgeter.util.ElementsIdProvider;
import ru.adios.budgeter.util.Formatting;
import ru.adios.budgeter.util.SpyingTextView;
import ru.adios.budgeter.util.UiUtils;

public class HomeActivity extends AppCompatActivity {

    static {
        UiUtils.onApplicationStart();
        Schema.TREASURY.registerBalanceAccount(new Treasury.BalanceAccount("Тест", Units.RUB));
    }

    private static final int TABLE_ROWS = 5;
    private static final OptLimit TABLE_REQUEST_LIMIT = OptLimit.createLimit(TABLE_ROWS);
    private static final OrderBy<FundsMutationEventRepository.Field> MUTATIONS_ORDER_BY_TIMESTAMP = new OrderBy<>(FundsMutationEventRepository.Field.TIMESTAMP, Order.DESC);
    private static final OrderBy<CurrencyExchangeEventRepository.Field> EXCHANGES_ORDER_BY_TIMESTAMP = new OrderBy<>(CurrencyExchangeEventRepository.Field.TIMESTAMP, Order.DESC);
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.####");

    private BalancedMenuHandler menuHandler;
    private int fiveDp = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home); //draw

        initMenuHandler((LinearLayout) findViewById(R.id.ah_funds_list)); // Register balances state listener
        populateTables();

        BalancesUiThreadState.instantiate(); // this is the first activity so...
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
        fiveDp = -1;
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
        startActivity(new Intent(this, FundsMutationActivity.class)); //TODO: change
    }

    private boolean initMenuHandler(final LinearLayout fundsLayout) {
        if (menuHandler == null) {
            menuHandler = new BalancedMenuHandler(new Consumer<BalancesUiThreadState.Pair>() {
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
        populateTables();
        resumeMenuHandler();
    }

    private void populateTables() {
        if (fiveDp < 0) {
            fiveDp = UiUtils.dpAsPixels(HomeActivity.this, 5);
            // Populate tables
            new AsyncTask<Void, Void, List<FundsMutationEvent>>() {
                @Override
                protected List<FundsMutationEvent> doInBackground(Void... params) {
                    return Schema.FUNDS_MUTATION_EVENTS
                            .streamMutationEvents(TABLE_REQUEST_LIMIT, MUTATIONS_ORDER_BY_TIMESTAMP)
                            .collect(Collectors.<FundsMutationEvent>toList());
                }

                @Override
                protected void onPostExecute(List<FundsMutationEvent> events) {
                    if (events.isEmpty()) {
                        return;
                    }

                    final TableLayout tableLayout = (TableLayout) findViewById(R.id.ah_ops_table);
                    for (int i = 0; i < events.size(); i++) {
                        final FundsMutationEvent fme = events.get(i);
                        final TableRow row = new TableRow(HomeActivity.this);
                        row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                        row.setWeightSum(8.f);
                        row.setId(ElementsIdProvider.getNextId());
                        final int rowId = i + 3;
                        row.addView(createSpyingColumnForTableRow(fme.timestamp.format(TS_FORMATTER), R.id.ah_ops_table, rowId));
                        final int seventy = fiveDp * 14;
                        row.addView(createColumnForTableRow(fme.subject.name, seventy));
                        row.addView(createColumnForTableRow(Formatting.toStringMoneyUsingText(fme.amount)));
                        row.addView(createColumnForTableRow(fme.relevantBalance.name, seventy));
                        tableLayout.addView(row, rowId);
                    }

                    tableLayout.setVisibility(View.VISIBLE);
                    tableLayout.invalidate();
                }

            }.execute();
            new AsyncTask<Void, Void, List<CurrencyExchangeEvent>>() {
                @Override
                protected List<CurrencyExchangeEvent> doInBackground(Void... params) {
                    return Schema.CURRENCY_EXCHANGE_EVENTS
                            .streamExchangeEvents(TABLE_REQUEST_LIMIT, EXCHANGES_ORDER_BY_TIMESTAMP)
                            .collect(Collectors.<CurrencyExchangeEvent>toList());
                }

                @Override
                protected void onPostExecute(List<CurrencyExchangeEvent> events) {
                    if (events.isEmpty()) {
                        return;
                    }

                    final TableLayout tableLayout = (TableLayout) findViewById(R.id.ah_exchanges_table);
                    for (int i = 0; i < events.size(); i++) {
                        final CurrencyExchangeEvent cee = events.get(i);
                        final TableRow row = new TableRow(HomeActivity.this);
                        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        row.setWeightSum(12.f);
                        row.setId(ElementsIdProvider.getNextId());
                        final int rowId = i + 3;
                        row.addView(createSpyingColumnForTableRow(cee.timestamp.format(TS_FORMATTER), R.id.ah_exchanges_table, rowId));
                        row.addView(createColumnForTableRow(Formatting.toStringMoneyUsingText(cee.bought)));
                        row.addView(createColumnForTableRow(Formatting.toStringMoneyUsingText(cee.sold)));
                        final int fiftyDp = fiveDp * 10;
                        row.addView(createColumnForTableRow(cee.boughtAccount.name, fiftyDp));
                        row.addView(createColumnForTableRow(cee.soldAccount.name, fiftyDp));
                        row.addView(createColumnForTableRow(DECIMAL_FORMAT.format(cee.rate)));
                        tableLayout.addView(row, rowId);
                    }

                    tableLayout.setVisibility(View.VISIBLE);
                    tableLayout.invalidate();
                }
            }.execute();
        }
    }

    private void resumeMenuHandler() {
        final LinearLayout fundsLayout = (LinearLayout) findViewById(R.id.ah_funds_list);
        if (initMenuHandler(fundsLayout)) {
            final BalancesUiThreadState.Pair pair = BalancesUiThreadState.getSnapshot();
            UiUtils.refillLinearLayoutWithBalances(fundsLayout, pair.balances, pair.totalBalance, this);
        }
    }

    @Nonnull
    private TextView createColumnForTableRow(String text) {
        final TextView view = new TextView(HomeActivity.this);
        populateColumn(view);
        view.setText(text);
        return view;
    }

    @Nonnull
    private TextView createColumnForTableRow(String text, int maxWidth) {
        final TextView view = new SpyingTextView(HomeActivity.this);
        populateColumn(view);
        view.setMaxWidth(maxWidth);
        view.setText(text);
        return view;
    }

    @Nonnull
    private TextView createSpyingColumnForTableRow(String text, final int tableId, final int rowId) {
        final TextView view = new SpyingTextView(HomeActivity.this);
        populateColumn(view);
        final SpyingTextView spyingTextView = (SpyingTextView) view;
        spyingTextView.heightCatch = true;
        spyingTextView.setHeightRunnable(new Runnable() {
            @Override
            public void run() {
                final TableLayout tableLayout = (TableLayout) findViewById(tableId);
                final TableRow row = (TableRow) tableLayout.getChildAt(rowId);
                int maxHeight = 0;
                for (int i = 0; i < row.getChildCount(); i++) {
                    final TextView childAt = (TextView) row.getChildAt(i);
                    maxHeight = Math.max(maxHeight, childAt.getHeight());
                }
                for (int i = 0; i < row.getChildCount(); i++) {
                    final TextView childAt = (TextView) row.getChildAt(i);
                    childAt.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, maxHeight, 2.f));
                }

                tableLayout.invalidate();
            }
        });
        view.setText(text);
        return view;
    }

    private void populateColumn(TextView view) {
        view.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 2.f));
        view.setId(ElementsIdProvider.getNextId());
        view.setBackground(ContextCompat.getDrawable(HomeActivity.this, R.drawable.cell_shape));
        view.setPadding(fiveDp, fiveDp, fiveDp, fiveDp);
        view.setTextAppearance(HomeActivity.this, android.R.style.TextAppearance_Small);
    }

}
