/*
 *
 *  *
 *  *  * Copyright 2015 Michael Kulikov
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

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
import ru.adios.budgeter.ElementsIdProvider;
import ru.adios.budgeter.FundsAwareMenuActivity;
import ru.adios.budgeter.R;
import ru.adios.budgeter.api.FundsMutationEventRepository;
import ru.adios.budgeter.api.Order;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.api.OrderedField;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.core.CoreUtils;
import ru.adios.budgeter.util.Formatting;
import ru.adios.budgeter.widgets.DataTableLayout;
import ru.adios.budgeter.widgets.MutationEventsDataStore;

@UiThread
public class MutationsTableActivity extends FundsAwareMenuActivity {

    private static final int TABLE_ID = ElementsIdProvider.getNextId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

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
        table.setId(TABLE_ID);

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
        return R.layout.activity_mutations_table;
    }

    @Override
    @MenuRes
    protected int getMenuResource() {
        return R.menu.menu_mutations_table;
    }

}
