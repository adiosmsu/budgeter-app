package ru.adios.budgeter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;

import java.math.BigDecimal;

import java8.util.function.Supplier;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.util.BalancesUiThreadState;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.HintedArrayAdapter;
import ru.adios.budgeter.util.UiUtils;

public class ExchangeCurrenciesActivity extends CoreElementActivity {

    private final ExchangeCurrenciesElementCore exchangeElement =
            new ExchangeCurrenciesElementCore(Constants.ACCOUNTER, BundleProvider.getBundle().treasury(), Constants.CURRENCIES_EXCHANGE_SERVICE.getExchangeService());
    private final CoreErrorHighlighter exchangeHighlighter = new CoreErrorHighlighter();

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
                                        public void link(HintedArrayAdapter.ObjectContainer data) {
                                            exchangeElement.setBuyAccount((BalanceAccount) data.getObject());
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
                                        public void link(HintedArrayAdapter.ObjectContainer data) {
                                            exchangeElement.setSellAccount((BalanceAccount) data.getObject());
                                        }
                                    })
                                    .build()
                    )
                    .addProvider(FundsAgentFragment.getInfoProvider(
                            R.id.exchange_currencies_agent_fragment,
                            null,
                            new CoreElementFieldInfo(ExchangeCurrenciesElementCore.FIELD_AGENT, new CoreNotifier.HintedLinker() {
                                @Override
                                public void link(HintedArrayAdapter.ObjectContainer data) {
                                    exchangeElement.setAgent((FundsMutationAgent) data.getObject());
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

        CoreNotifier.addLink(this, findViewById(R.id.exchange_currencies_natural_rate), new CoreNotifier.DecimalLinker() {
            @Override
            public void link(BigDecimal data) {
                exchangeElement.setNaturalRate(data);
            }
        });
        CoreNotifier.addLink(this, findViewById(R.id.exchange_currencies_custom_rate), new CoreNotifier.DecimalLinker() {
            @Override
            public void link(BigDecimal data) {
                exchangeElement.setCustomRate(data);
            }
        });

        final View infoView = findViewById(R.id.exchange_currencies_info);
        exchangeHighlighter.setGlobalInfoView(infoView);
        setGlobalInfoViewPerFragment(R.id.exchange_currencies_buy_account_fragment, AccountStandardFragment.BUTTON_NEW_ACCOUNT_SUBMIT, infoView);
        setGlobalInfoViewPerFragment(R.id.exchange_currencies_sell_account_fragment, AccountStandardFragment.BUTTON_NEW_ACCOUNT_SUBMIT, infoView);
        setGlobalInfoViewPerFragment(R.id.exchange_currencies_agent_fragment, FundsAgentFragment.BUTTON_NEW_AGENT_SUBMIT, infoView);
    }

    @Override
    protected void activityInnerFeedback() {
        decimalTextViewFeedback(exchangeElement.getNaturalRate(), R.id.exchange_currencies_natural_rate);
        decimalTextViewFeedback(exchangeElement.getCustomRate(), R.id.exchange_currencies_custom_rate);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
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
                doSubmitAndStore(core);
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
                }

                finishSubmit(core, R.id.activity_exchange_currencies);
            }
        }.execute(exchangeElement);
    }

}
