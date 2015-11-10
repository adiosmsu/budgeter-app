package ru.adios.budgeter.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.annotation.UiThread;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.common.collect.ImmutableList;

import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.function.Function;
import ru.adios.budgeter.FundsAwareMenuActivity;
import ru.adios.budgeter.R;
import ru.adios.budgeter.api.CurrencyExchangeEventRepository;
import ru.adios.budgeter.api.Order;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.api.OrderedField;
import ru.adios.budgeter.api.data.CurrencyExchangeEvent;
import ru.adios.budgeter.util.Formatting;
import ru.adios.budgeter.widgets.DataTableLayout;
import ru.adios.budgeter.widgets.ExchangesDataStore;

@UiThread
public class ExchangesTableActivity extends FundsAwareMenuActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final RelativeLayout mainLayout = (RelativeLayout) findViewById(R.id.activity_exchanges_table);

        final Resources resources = getResources();
        final String timeColName = resources.getString(R.string.ah_ops_table_col_time);
        final DataTableLayout table = new DataTableLayout(this, 2, new ExchangesDataStore(
                ImmutableList.of(
                        timeColName,
                        resources.getString(R.string.ah_exchanges_table_col_bought),
                        resources.getString(R.string.ah_exchanges_table_col_sold),
                        resources.getString(R.string.ah_ops_table_col_agent),
                        resources.getString(R.string.ah_exchanges_table_col_b_acc),
                        resources.getString(R.string.ah_exchanges_table_col_s_acc),
                        resources.getString(R.string.ah_exchanges_table_col_rate)
                ),
                new Function<CurrencyExchangeEvent, Iterable<String>>() {
                    @Override
                    public Iterable<String> apply(CurrencyExchangeEvent event) {
                        return ImmutableList.of(
                                Formatting.toStringRusDateTimeShort(event.timestamp),
                                Formatting.toStringMoneyUsingSign(event.bought, resources),
                                Formatting.toStringMoneyUsingSign(event.sold, resources),
                                event.agent.name,
                                event.boughtAccount.name,
                                event.soldAccount.name,
                                Formatting.toStringExchangeRate(event.rate)
                        );
                    }
                }
        ));
        final RelativeLayout.LayoutParams exchangesParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        exchangesParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        exchangesParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        table.setLayoutParams(exchangesParams);
        table.setTableName(resources.getString(R.string.exchanges_table_header));
        table.setPageSize(10);
        table.setOrderBy(new OrderBy<>(CurrencyExchangeEventRepository.Field.TIMESTAMP, Order.DESC));
        table.enableUserOrdering(new DataTableLayout.OrderResolver() {
            @Override
            public Optional<OrderBy> byColumnName(String columnName, Order order) {
                if (timeColName.equals(columnName)) {
                    return Optional.<OrderBy>of(new OrderBy<>(CurrencyExchangeEventRepository.Field.TIMESTAMP, order));
                }
                return Optional.empty();
            }

            @Override
            public Optional<String> byField(OrderedField field) {
                if (CurrencyExchangeEventRepository.Field.TIMESTAMP.equals(field)) {
                    return Optional.of(timeColName);
                }
                return Optional.empty();
            }
        });
        table.setVisibility(View.GONE);
        table.setOnDataLoadedListener(new Consumer<DataTableLayout>() {
            @Override
            public void accept(DataTableLayout dataTableLayout) {
                dataTableLayout.setVisibility(View.VISIBLE);
            }
        });
        table.setSaveEnabled(false);

        mainLayout.addView(table);
        table.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    @LayoutRes
    protected int getLayoutId() {
        return R.layout.activity_exchanges_table;
    }

    @Override
    @MenuRes
    protected int getMenuResource() {
        return R.menu.menu_exchanges_table;
    }

}
