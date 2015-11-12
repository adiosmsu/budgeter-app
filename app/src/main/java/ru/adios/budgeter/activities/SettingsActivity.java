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

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.annotation.UiThread;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java8.util.Optional;
import java8.util.function.Consumer;
import ru.adios.budgeter.BalancesUiThreadState;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.Constants;
import ru.adios.budgeter.CurrenciesExchangeService;
import ru.adios.budgeter.FundsAwareMenuActivity;
import ru.adios.budgeter.R;
import ru.adios.budgeter.api.data.ConversionRate;
import ru.adios.budgeter.util.Formatting;
import ru.adios.budgeter.util.UiUtils;

@UiThread
public class SettingsActivity extends FundsAwareMenuActivity {

    private static final Logger logger = LoggerFactory.getLogger(SettingsActivity.class);

    private final DialogInterface.OnClickListener dbDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        String r = null;
                        try {
                            BundleProvider.getBundle().clearSchema();
                        } catch (RuntimeException ex) {
                            r = "Error: " + ex.getMessage();
                        }
                        return r;
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        final TextView info = (TextView) findViewById(R.id.settings_reset_db_button_info);
                        if (s == null) {
                            info.setTextColor(UiUtils.GREEN_COLOR);
                            info.setText(R.string.button_success);
                            BalancesUiThreadState.instantiate(); // invalidate balances cache
                        } else {
                            info.setTextColor(UiUtils.RED_COLOR);
                            info.setText(s);
                        }
                        info.setVisibility(View.VISIBLE);
                    }
                }.execute();
            }
            dialog.dismiss();
        }
    };
    private final DialogInterface.OnClickListener dismissListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    @LayoutRes
    protected int getLayoutId() {
        return R.layout.activity_settings;
    }

    @Override
    @MenuRes
    protected int getMenuResource() {
        return R.menu.menu_settings;
    }

    public void resetDb(View view) {
        findViewById(R.id.settings_reset_db_button_info).setVisibility(View.INVISIBLE);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.attention_exclamation))
                .setMessage(getString(R.string.settings_db_reset_dialog_text))
                .setPositiveButton(getString(R.string.common_yes), dbDialogClickListener)
                .setNegativeButton(getString(R.string.common_no), dbDialogClickListener)
                .show();
    }

    public void processPostponed(View view) {
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.settings_postponed_progress);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.invalidate();

        new AsyncTask<Void, Integer, CurrenciesExchangeService.ProcessPostponedResult>() {
            @Override
            protected CurrenciesExchangeService.ProcessPostponedResult doInBackground(Void[] params) {
                try {
                    return Constants.CURRENCIES_EXCHANGE_SERVICE.getExchangeService().processAllPostponedEvents(
                            Optional.<Consumer<Integer>>of(
                                    new Consumer<Integer>() {
                                        @Override
                                        public void accept(Integer percent) {
                                            publishProgress(percent);
                                        }
                                    }
                            ),
                            Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                    );
                } catch (RuntimeException ex) {
                    logger.error("Exception while processing all postponed tasks", ex);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(CurrenciesExchangeService.ProcessPostponedResult processPostponedResult) {
                progressBar.setVisibility(View.INVISIBLE);
                progressBar.invalidate();

                if (processPostponedResult == null) {
                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle(getString(R.string.error_occurred_phraze))
                            .setMessage("An error occurred while doing processing of all postponed tasks")
                            .setNegativeButton(getString(R.string.common_done), dismissListener)
                            .show();
                } else {
                    final StringBuilder mesBuilder = new StringBuilder(50 + processPostponedResult.succeeded.size() * 20 + processPostponedResult.failed.size() * 20);
                    mesBuilder.append(getString(R.string.settings_succeeded_postponed_message_start)).append(": ");
                    appendPostponed(processPostponedResult.succeeded, mesBuilder, true);
                    mesBuilder.append('\n').append(getString(R.string.settings_failed_postponed_message_start)).append(": ");
                    appendPostponed(processPostponedResult.failed, mesBuilder, false);
                    mesBuilder.append('.');

                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle(getString(R.string.work_finished_phraze))
                            .setMessage(mesBuilder.toString())
                            .setPositiveButton(getString(R.string.common_done), dismissListener)
                            .show();
                }
            }

            @Override
            protected void onProgressUpdate(Integer... percents) {
                progressBar.setProgress(percents[0]);
            }
        }.execute();
    }

    private static void appendPostponed(ImmutableList<ConversionRate> list, StringBuilder mesBuilder, boolean appendRate) {
        boolean first = true;
        for (final ConversionRate rate : list) {
            if (first) {
                first = false;
            } else {
                mesBuilder.append(", ");
            }
            mesBuilder.append(Formatting.toStringRusDay(rate.day))
                    .append(':');
            rate.pair.appendTo(mesBuilder);
            if (appendRate) {
                mesBuilder.append(':')
                        .append(rate.rate.toPlainString());
            }
        }
        if (first) {
            mesBuilder.append("None");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
