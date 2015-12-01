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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatSpinner;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import java8.util.Optional;
import java8.util.OptionalInt;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.ElementsIdProvider;
import ru.adios.budgeter.FundsAwareMenuActivity;
import ru.adios.budgeter.R;
import ru.adios.budgeter.adapters.NullableDecoratingAdapter;
import ru.adios.budgeter.adapters.Presenters;
import ru.adios.budgeter.adapters.StringPresenter;
import ru.adios.budgeter.api.Order;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.api.OrderedField;
import ru.adios.budgeter.api.RepoOption;
import ru.adios.budgeter.api.SubjectPriceRepository;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.api.data.SubjectPrice;
import ru.adios.budgeter.util.EmptyOnItemSelectedListener;
import ru.adios.budgeter.util.Formatting;
import ru.adios.budgeter.widgets.AbstractDataStore;
import ru.adios.budgeter.widgets.DataTableLayout;

@UiThread
public class PricesActivity extends FundsAwareMenuActivity {

    public static final String KEY_SUBJECTS_SEL = "prices_act_subj_key1";
    public static final String KEY_SUBJECTS_ID = "prices_act_subj_key2";
    public static final String KEY_AGENTS_SEL = "prices_act_agents_key1";
    public static final String KEY_AGENTS_ID = "prices_act_agents_key2";
    public static final String KEY_DT = "prices_act_dt_key";
    public static final String KEY_TABLE_NAME = "prices_act_table_name_key";

    private static final int TABLE_ID = ElementsIdProvider.getNextId();

    private static final Logger logger = LoggerFactory.getLogger(PricesActivity.class);


    // transient state
    private SpinnerState subjectsSelection = new SpinnerState(-1, -1);
    private SpinnerState agentsSelection = new SpinnerState(-1, -1);
    private boolean dataTableConstructed = false;
    private String tableName;
    // end of transient state

    private AppCompatSpinner subjectsSpinner;
    private RelativeLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mainLayout = (RelativeLayout) findViewById(R.id.activity_prices);
        subjectsSpinner = (AppCompatSpinner) mainLayout.findViewById(R.id.price_subjects_spinner);
        final AppCompatSpinner agentsSpinner = (AppCompatSpinner) mainLayout.findViewById(R.id.price_agents_spinner);

        if (savedInstanceState != null) {
            subjectsSelection = new SpinnerState(savedInstanceState.getInt(KEY_SUBJECTS_SEL, -1), savedInstanceState.getLong(KEY_SUBJECTS_ID, -1));
            agentsSelection = new SpinnerState(savedInstanceState.getInt(KEY_AGENTS_SEL, -1), savedInstanceState.getLong(KEY_AGENTS_ID, -1));
            dataTableConstructed = savedInstanceState.getBoolean(KEY_DT, false);
            tableName = savedInstanceState.getString(KEY_TABLE_NAME);
            if (dataTableConstructed) {
                constructDataTable();
            }
        }

        fillSpinner(
                subjectsSpinner,
                subjectsSelection,
                BundleProvider.getBundle().fundsMutationSubjects().streamAll(),
                Presenters.getSubjectParentLoadingPresenter(),
                R.string.subjects_spinner_null_val,
                new Function<FundsMutationSubject, Long>() {
                    @Override
                    public Long apply(FundsMutationSubject fundsMutationSubject) {
                        return fundsMutationSubject.id.getAsLong();
                    }
                }
        );
        fillSpinner(
                agentsSpinner,
                agentsSelection,
                BundleProvider.getBundle().fundsMutationAgents().streamAll(),
                Presenters.getAgentDefaultPresenter(),
                R.string.agents_spinner_null_val,
                new Function<FundsMutationAgent, Long>() {
                    @Override
                    public Long apply(FundsMutationAgent agent) {
                        return agent.id.getAsLong();
                    }
                }
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SUBJECTS_SEL, subjectsSelection.selection);
        outState.putLong(KEY_SUBJECTS_ID, subjectsSelection.id);
        outState.putInt(KEY_AGENTS_SEL, agentsSelection.selection);
        outState.putLong(KEY_AGENTS_ID, agentsSelection.id);
        outState.putBoolean(KEY_DT, dataTableConstructed);
        outState.putString(KEY_TABLE_NAME, tableName);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_prices;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.menu_prices;
    }

    public void show(View view) {
        if (!dataTableConstructed) {
            constructDataTable();
        }
    }

    private void constructDataTable() {
        if (subjectsSelection.selection >= 0) {
            final String dayColName = getString(R.string.prices_table_col_day);
            final String priceColName = getString(R.string.prices_table_col_price);
            final DataTableLayout table = new DataTableLayout(
                    this,
                    new AbstractDataStore<SubjectPrice>(
                            ImmutableList.of(
                                    dayColName,
                                    getString(R.string.prices_table_col_agent),
                                    priceColName
                            ),
                            new Function<SubjectPrice, Iterable<String>>() {
                                @Override
                                public Iterable<String> apply(SubjectPrice sp) {
                                    return ImmutableList.of(Formatting.toStringRusDay(sp.day), sp.agent.name, Formatting.toStringMoneyUsingSign(sp.price, getResources()));
                                }
                            }
                    ) {
                        @Override
                        protected Stream<SubjectPrice> getLoadingStream(RepoOption[] options) {
                            return agentsSelection.id >= 0
                                    ? BundleProvider.getBundle().subjectPrices().streamByAgent(subjectsSelection.id, agentsSelection.id, options)
                                    : BundleProvider.getBundle().subjectPrices().stream(subjectsSelection.id, options);
                        }

                        @Override
                        public int count() {
                            return agentsSelection.id >= 0
                                    ? BundleProvider.getBundle().subjectPrices().countByAgent(subjectsSelection.id, agentsSelection.id)
                                    : BundleProvider.getBundle().subjectPrices().count(subjectsSelection.id);
                        }
                    }
            );
            final RelativeLayout.LayoutParams exchangesParams =
                    new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            exchangesParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            exchangesParams.addRule(RelativeLayout.BELOW, R.id.prices_show_button);
            table.setLayoutParams(exchangesParams);
            final Object selectedItem = subjectsSpinner.getSelectedItem();
            if (tableName == null) {
                //noinspection unchecked
                final String sn = (selectedItem != null) ? ((FundsMutationSubject) selectedItem).name : null;
                tableName = getResources().getString(R.string.common_table_col_subj) + ": " + sn;
            }
            table.setTableName(tableName);
            table.setPageSize(5);
            table.setOrderBy(new OrderBy<>(SubjectPriceRepository.Field.PRICE, Order.ASC));
            table.enableUserOrdering(new DataTableLayout.OrderResolver() {
                @Override
                public Optional<OrderBy> byColumnName(String columnName, Order order) {
                    if (columnName.equals(dayColName)) {
                        return Optional.<OrderBy>of(new OrderBy<>(SubjectPriceRepository.Field.DAY, order));
                    } else if (columnName.equals(priceColName)) {
                        return Optional.<OrderBy>of(new OrderBy<>(SubjectPriceRepository.Field.PRICE, order));
                    }
                    return Optional.empty();
                }

                @Override
                public Optional<String> byField(OrderedField field) {
                    if (field.equals(SubjectPriceRepository.Field.DAY)) {
                        return Optional.of(dayColName);
                    } else if (field.equals(SubjectPriceRepository.Field.PRICE)) {
                        return Optional.of(priceColName);
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
            mainLayout.invalidate();
            dataTableConstructed = true;
        }
    }

    private <T> void fillSpinner(final Spinner spinner,
                                 final SpinnerState selection,
                                 final Stream<T> stream,
                                 final StringPresenter<T> presenter,
                                 final @StringRes int nullPresentation,
                                 final Function<T, Long> idExtractor
    ) {
        NullableDecoratingAdapter.adaptSpinnerWithArrayWrapper(spinner, Optional.<StringPresenter<String>>empty(), new String[] {});
        new AsyncTask<Void, Void, List<T>>() {
            @Override
            protected List<T> doInBackground(Void[] params) {
                // get values from db
                try {
                    return stream.collect(Collectors.<T>toList());
                } catch (RuntimeException e) {
                    logger.warn("Exception while querying for spinner contents with stream", e);
                    return new ArrayList<>(1);
                }
            }

            @Override
            protected void onPostExecute(List<T> res) {
                NullableDecoratingAdapter.adaptSpinnerWithArrayWrapper(spinner, Optional.of(presenter), res, OptionalInt.of(nullPresentation));
                if (selection.selection >= 0) {
                    spinner.setSelection(selection.selection);
                }
                spinner.setOnItemSelectedListener(new EmptyOnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        final boolean actual = position < parent.getAdapter().getCount();
                        selection.selection = actual ? position : -1;
                        //noinspection unchecked
                        selection.id = actual ? idExtractor.apply((T) parent.getSelectedItem()) : -1;
                    }
                });
                spinner.invalidate();
            }
        }.execute();
    }

    private static final class SpinnerState {

        int selection;
        long id;

        SpinnerState(int selection, long id) {
            this.selection = selection;
            this.id = id;
        }

    }

}
