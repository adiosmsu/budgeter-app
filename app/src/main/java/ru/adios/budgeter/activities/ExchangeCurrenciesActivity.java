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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;

import java8.util.function.Supplier;
import ru.adios.budgeter.BalancesUiThreadState;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.Constants;
import ru.adios.budgeter.ExchangeCurrenciesElementCore;
import ru.adios.budgeter.R;
import ru.adios.budgeter.Submitter;
import ru.adios.budgeter.adapters.HintedArrayAdapter;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.core.CollectedFragmentsInfoProvider;
import ru.adios.budgeter.core.CoreElementActivity;
import ru.adios.budgeter.core.CoreErrorHighlighter;
import ru.adios.budgeter.core.CoreNotifier;
import ru.adios.budgeter.core.CoreUtils;
import ru.adios.budgeter.core.Feedbacking;
import ru.adios.budgeter.fragments.AccountStandardFragment;
import ru.adios.budgeter.fragments.DateTimeFragment;
import ru.adios.budgeter.fragments.EnterAmountFragment;
import ru.adios.budgeter.fragments.FundsAgentFragment;
import ru.adios.budgeter.util.UiUtils;

@UiThread
public class ExchangeCurrenciesActivity extends CoreElementActivity {

    public static final String KEY_HIGHLIGHTER = "ex_cur_act_high";
    public static final String KEY_NATURAL_RATE = "ex_cur_nr_key";
    public static final String KEY_CUSTOM_RATE = "ex_cur_cr_key";

    private final ExchangeCurrenciesElementCore exchangeElement =
            new ExchangeCurrenciesElementCore(Constants.ACCOUNTER, BundleProvider.getBundle().treasury(), Constants.CURRENCIES_EXCHANGE_SERVICE.getExchangeService());
    private final CoreErrorHighlighter exchangeHighlighter = new CoreErrorHighlighter(KEY_HIGHLIGHTER);

    private final CollectedFragmentsInfoProvider infoProvider =
            new CollectedFragmentsInfoProvider.Builder(this)
                    .addProvider(EnterAmountFragment.getInfoProvider(
                            R.id.exchange_currencies_buy_amount_fragment,
                            exchangeElement.getBuyMoneySettable(),
                            exchangeHighlighter,
                            ExchangeCurrenciesElementCore.FIELD_BUY_AMOUNT_DECIMAL,
                            ExchangeCurrenciesElementCore.FIELD_BUY_AMOUNT_UNIT
                    ))
                    .addProvider(EnterAmountFragment.getInfoProvider(
                            R.id.exchange_currencies_sell_amount_fragment,
                            exchangeElement.getSellMoneySettable(),
                            exchangeHighlighter,
                            ExchangeCurrenciesElementCore.FIELD_SELL_AMOUNT_DECIMAL,
                            ExchangeCurrenciesElementCore.FIELD_SELL_AMOUNT_UNIT
                    ))
                    .addProvider(
                            AccountStandardFragment.getInfoProviderBuilder(R.id.exchange_currencies_buy_account_fragment, this, new Supplier<BalanceAccount>() {
                                @Override
                                public BalanceAccount get() {
                                    return exchangeElement.getBuyAccount();
                                }
                            })
                                    .provideAccountFieldInfo(ExchangeCurrenciesElementCore.FIELD_BUY_ACCOUNT, exchangeHighlighter, new CoreNotifier.HintedLinker() {
                                        @Override
                                        public boolean link(HintedArrayAdapter.ObjectContainer data) {
                                            final BalanceAccount object = (BalanceAccount) data.getObject();
                                            final BalanceAccount prev = exchangeElement.getBuyAccount();
                                            if ((prev == null && object != null) || (prev != null && !prev.equals(object))) {
                                                exchangeElement.setBuyAccount(object);
                                                return true;
                                            }
                                            return false;
                                        }
                                    })
                                    .build()
                    )
                    .addProvider(
                            AccountStandardFragment.getInfoProviderBuilder(R.id.exchange_currencies_sell_account_fragment, this, new Supplier<BalanceAccount>() {
                                @Override
                                public BalanceAccount get() {
                                    return exchangeElement.getSellAccount();
                                }
                            })
                                    .provideAccountFieldInfo(ExchangeCurrenciesElementCore.FIELD_SELL_ACCOUNT, exchangeHighlighter, new CoreNotifier.HintedLinker() {
                                        @Override
                                        public boolean link(HintedArrayAdapter.ObjectContainer data) {
                                            final BalanceAccount object = (BalanceAccount) data.getObject();
                                            final BalanceAccount prev = exchangeElement.getSellAccount();
                                            if ((prev == null && object != null) || (prev != null && !prev.equals(object))) {
                                                exchangeElement.setSellAccount(object);
                                                return true;
                                            }
                                            return false;
                                        }
                                    })
                                    .build()
                    )
                    .addProvider(FundsAgentFragment.getInfoProvider(
                            R.id.exchange_currencies_agent_fragment,
                            null,
                            new CoreElementFieldInfo(ExchangeCurrenciesElementCore.FIELD_AGENT, new CoreNotifier.HintedLinker() {
                                @Override
                                public boolean link(HintedArrayAdapter.ObjectContainer data) {
                                    final FundsMutationAgent object = (FundsMutationAgent) data.getObject();
                                    final FundsMutationAgent prev = exchangeElement.getAgent();
                                    if ((prev == null && object != null) || (prev != null && !prev.equals(object))) {
                                        exchangeElement.setAgent(object);
                                        return true;
                                    }
                                    return false;
                                }
                            }, exchangeHighlighter),
                            new Supplier<FundsMutationAgent>() {
                                @Override
                                public FundsMutationAgent get() {
                                    return exchangeElement.getAgent();
                                }
                            }
                    ))
                    .addProvider(DateTimeFragment.getInfoProvider(R.id.exchange_currencies_datetime_fragment, exchangeHighlighter, ExchangeCurrenciesElementCore.FIELD_TIMESTAMP, exchangeElement))
                    .build();

    private TextView exchangeCurrenciesNaturalRate;
    private TextView exchangeCurrenciesCustomRate;

    // transient state
    private BigDecimal naturalRateVal;
    private BigDecimal customRateVal;
    // end of transient state

    {
        exchangeElement.setPersonalMoneyExchange(true);
    }

    @Override
    protected FragmentsInfoProvider getInfoProvider() {
        return infoProvider;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_exchange_currencies;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.menu_exchange_currencies;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            final String nrStr = savedInstanceState.getString(KEY_NATURAL_RATE);
            if (nrStr != null) {
                naturalRateVal = new BigDecimal(nrStr);
            }
            final String crStr = savedInstanceState.getString(KEY_CUSTOM_RATE);
            if (crStr != null) {
                customRateVal = new BigDecimal(crStr);
            }
        }

        if (naturalRateVal != null) {
            exchangeCurrenciesNaturalRate.setText(naturalRateVal.toPlainString());
            exchangeElement.setNaturalRate(naturalRateVal);
        }
        if (customRateVal != null) {
            exchangeCurrenciesCustomRate.setText(customRateVal.toPlainString());
            exchangeElement.setCustomRate(customRateVal);
        }

        CoreNotifier.addLink(this, exchangeCurrenciesNaturalRate, new CoreNotifier.DecimalLinker() {
            @Override
            public boolean link(BigDecimal data) {
                naturalRateVal = data;
                final BigDecimal prev = exchangeElement.getNaturalRate();
                if ((prev == null && data != null) || (prev != null && !prev.equals(data))) {
                    exchangeElement.setNaturalRate(data);
                    return true;
                }
                return false;
            }
        });
        CoreNotifier.addLink(this, exchangeCurrenciesCustomRate, new CoreNotifier.DecimalLinker() {
            @Override
            public boolean link(BigDecimal data) {
                customRateVal = data;
                final BigDecimal prev = exchangeElement.getCustomRate();
                if ((prev == null && data != null) || (prev != null && !prev.equals(data))) {
                    exchangeElement.setCustomRate(data);
                    return true;
                }
                return false;
            }
        });

        final View infoView = findViewById(R.id.exchange_currencies_info);
        exchangeHighlighter.setGlobalInfoView(infoView);
        setGlobalInfoViewPerFragment(R.id.exchange_currencies_buy_account_fragment, AccountStandardFragment.BUTTON_NEW_ACCOUNT_SUBMIT, infoView);
        setGlobalInfoViewPerFragment(R.id.exchange_currencies_sell_account_fragment, AccountStandardFragment.BUTTON_NEW_ACCOUNT_SUBMIT, infoView);
        setGlobalInfoViewPerFragment(R.id.exchange_currencies_agent_fragment, FundsAgentFragment.BUTTON_NEW_AGENT_SUBMIT, infoView);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_NATURAL_RATE, naturalRateVal != null ? naturalRateVal.toPlainString() : null);
        outState.putString(KEY_CUSTOM_RATE, customRateVal != null ? customRateVal.toPlainString() : null);
    }

    @Override
    protected void clearViewReferences() {
        exchangeCurrenciesNaturalRate = null;
        exchangeCurrenciesCustomRate = null;
    }

    @Override
    protected void collectEssentialViews() {
        exchangeCurrenciesNaturalRate = (TextView) findViewById(R.id.exchange_currencies_natural_rate);
        exchangeCurrenciesCustomRate = (TextView) findViewById(R.id.exchange_currencies_custom_rate);
    }

    @Override
    protected void activityInnerFeedback() {
        Feedbacking.decimalTextViewFeedback(exchangeElement.getNaturalRate(), exchangeCurrenciesNaturalRate);
        Feedbacking.decimalTextViewFeedback(exchangeElement.getCustomRate(), exchangeCurrenciesCustomRate);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void exchange(View view) {
        exchangeElement.lock();
        new AsyncTask<ExchangeCurrenciesElementCore, Void, ExchangeCurrenciesElementCore>() {
            @Override
            protected ExchangeCurrenciesElementCore doInBackground(ExchangeCurrenciesElementCore[] params) {
                final ExchangeCurrenciesElementCore core = params[0];
                CoreUtils.doSubmitAndStore(core);
                return core;
            }

            @Override
            protected void onPostExecute(ExchangeCurrenciesElementCore core) {
                final Submitter.Result result = core.getStoredResult();

                exchangeHighlighter.processSubmitResult(result);
                if (result.isSuccessful()) {
                    UiUtils.replaceAccountInSpinner(core.getBuyAccount(), (Spinner) findViewById(R.id.exchange_currencies_buy_account_fragment).findViewById(R.id.accounts_spinner), getResources());
                    BalancesUiThreadState.addMoney(core.getBuyMoneySettable().getAmount(), ExchangeCurrenciesActivity.this);
                    UiUtils.replaceAccountInSpinner(core.getSellAccount(), (Spinner) findViewById(R.id.exchange_currencies_sell_account_fragment).findViewById(R.id.accounts_spinner), getResources());
                    BalancesUiThreadState.addMoney(core.getSellMoneySettable().getAmount().negated(), ExchangeCurrenciesActivity.this);
                    Toast.makeText(getApplicationContext(), R.string.register_exchange_success, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.register_exchange_failure, Toast.LENGTH_SHORT)
                            .show();
                }

                finishSubmit(core, R.id.activity_exchange_currencies);
            }
        }.execute(exchangeElement);
    }

}
