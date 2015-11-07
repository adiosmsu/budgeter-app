package ru.adios.budgeter;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.common.collect.ImmutableList;

import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.function.Function;
import ru.adios.budgeter.api.FundsMutationEventRepository;
import ru.adios.budgeter.api.Order;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.api.OrderedField;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.util.CoreUtils;
import ru.adios.budgeter.util.DataTableLayout;
import ru.adios.budgeter.util.Formatting;
import ru.adios.budgeter.util.MutationEventsDataStore;

public class MutationsTableActivity extends FundsAwareMenuActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final RelativeLayout mainLayout = (RelativeLayout) findViewById(R.id.activity_mutations_table);

        final Resources resources = getResources();
        final String timeColName = resources.getString(R.string.ah_ops_table_col_time);
        final String amountColName = resources.getString(R.string.ah_ops_table_col_money);
        final DataTableLayout table = new DataTableLayout(this, 2, new MutationEventsDataStore(
                ImmutableList.of(
                        timeColName,
                        resources.getString(R.string.ah_ops_table_col_subj),
                        resources.getString(R.string.ah_ops_table_col_agent),
                        amountColName,
                        resources.getString(R.string.ah_ops_table_col_account)
                ),
                new Function<FundsMutationEvent, Iterable<String>>() {
                    @Override
                    public Iterable<String> apply(FundsMutationEvent event) {
                        return ImmutableList.of(
                                Formatting.toStringRusDateTimeShort(event.timestamp),
                                event.subject.name,
                                event.agent.name,
                                Formatting.toStringMoneyUsingSign(event.amount.multipliedBy(event.quantity), resources),
                                CoreUtils.getExtendedAccountString(event.relevantBalance, resources)
                        );
                    }
                }
        ));
        final RelativeLayout.LayoutParams mutationsParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        mutationsParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mutationsParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        table.setLayoutParams(mutationsParams);
        table.setTableName(resources.getString(R.string.mutations_table_header));
        table.setPageSize(10);
        table.setOrderBy(new OrderBy<>(FundsMutationEventRepository.Field.TIMESTAMP, Order.DESC));
        table.enableUserOrdering(new DataTableLayout.OrderResolver() {
            @Override
            public Optional<OrderBy> byColumnName(String columnName, Order order) {
                if (timeColName.equals(columnName)) {
                    return Optional.<OrderBy>of(new OrderBy<>(FundsMutationEventRepository.Field.TIMESTAMP, order));
                } else if (amountColName.equals(columnName)) {
                    return Optional.<OrderBy>of(new OrderBy<>(FundsMutationEventRepository.Field.AMOUNT, order));
                }
                return Optional.empty();
            }

            @Override
            public Optional<String> byField(OrderedField field) {
                if (FundsMutationEventRepository.Field.TIMESTAMP.equals(field)) {
                    return Optional.of(timeColName);
                } else if (FundsMutationEventRepository.Field.AMOUNT.equals(field)) {
                    return Optional.of(amountColName);
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

        mainLayout.addView(table);
        table.start();
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
    @LayoutRes
    protected int getLayoutId() {
        return R.layout.activity_mutations_table;
    }

    @Override
    @MenuRes
    protected int getMenuResource() {
        return R.menu.menu_mutations_table;
    }

}
