package ru.adios.budgeter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.MenuItem;
import android.view.View;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.math.BigDecimal;

import java8.util.function.Supplier;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.api.FundsMutationSubject;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.HintedArrayAdapter;

public class FundsMutationActivity extends CoreElementActivity {

    private final FundsMutationElementCore mutationElement = new FundsMutationElementCore(Constants.ACCOUNTER, Schema.TREASURY, Constants.CURRENCIES_EXCHANGE_SERVICE.getExchangeService());
    private final CoreErrorHighlighter mutationHighlighter = new CoreErrorHighlighter();

    private final CollectedFragmentsInfoProvider infoProvider =
            new CollectedFragmentsInfoProvider.Builder(this)
                    .addProvider(FundsSubjectFragment.getInfoProvider(
                            R.id.funds_mutation_subject_fragment,
                            null,
                            new CoreElementFieldInfo(FundsMutationElementCore.FIELD_SUBJECT, new CoreNotifier.HintedLinker() {
                                @Override
                                public void link(HintedArrayAdapter.ObjectContainer data) {
                                    mutationElement.setSubject((FundsMutationSubject) data.getObject());
                                }
                            }, mutationHighlighter),
                            new Supplier<FundsMutationSubject>() {
                                @Override
                                public FundsMutationSubject get() {
                                    return mutationElement.getSubject();
                                }
                            }
                    ))
                    .addProvider(EnterAmountFragment.getInfoProvider(
                            R.id.funds_mutation_subject_cost_fragment,
                            mutationElement,
                            mutationHighlighter,
                            FundsMutationElementCore.FIELD_AMOUNT_DECIMAL,
                            FundsMutationElementCore.FIELD_AMOUNT_UNIT
                    ))
                    .addProvider(EnterAmountFragment.getInfoProvider(
                            R.id.funds_mutation_paid_amount_fragment,
                            new MoneySettable() {
                                @Override
                                public void setAmount(int coins, int cents) {
                                    mutationElement.setPayeeAmount(coins, cents);
                                }

                                @Override
                                public void setAmountDecimal(BigDecimal amountDecimal) {
                                    mutationElement.setPayeeAmount(amountDecimal);
                                }

                                @Override
                                public BigDecimal getAmountDecimal() {
                                    return mutationElement.getPayeeAmount();
                                }

                                @Override
                                public void setAmountUnit(String code) {
                                    mutationElement.setPayeeAccountUnit(code);
                                }

                                @Override
                                public void setAmountUnit(CurrencyUnit unit) {
                                    mutationElement.setPayeeAccountUnit(unit);
                                }

                                @Override
                                public CurrencyUnit getAmountUnit() {
                                    return mutationElement.getPayeeAccountUnit();
                                }

                                @Override
                                public void setAmount(Money amount) {
                                    mutationElement.setPaidMoney(amount);
                                }

                                @Override
                                public Money getAmount() {
                                    return mutationElement.getPaidMoney();
                                }
                            },
                            mutationHighlighter,
                            FundsMutationElementCore.FIELD_PAYEE_AMOUNT,
                            FundsMutationElementCore.FIELD_PAYEE_ACCOUNT_UNIT
                    ))
                    .addProvider(
                            AccountStandardFragment.getInfoProviderBuilder(R.id.funds_mutation_relevant_balance_fragment, this, new Supplier<Treasury.BalanceAccount>() {
                                @Override
                                public Treasury.BalanceAccount get() {
                                    return mutationElement.getRelevantBalance();
                                }
                            })
                                    .provideAccountFieldInfo(FundsMutationElementCore.FIELD_RELEVANT_BALANCE, mutationHighlighter, new CoreNotifier.HintedLinker() {
                                        @Override
                                        public void link(HintedArrayAdapter.ObjectContainer data) {
                                            mutationElement.setRelevantBalance((Treasury.BalanceAccount) data.getObject());
                                        }
                                    })
                                    .build()
                    )
                    .addProvider(FundsAgentFragment.getInfoProvider(
                            R.id.funds_mutation_agent_fragment,
                            null,
                            new CoreElementFieldInfo(FundsMutationElementCore.FIELD_AGENT, new CoreNotifier.HintedLinker() {
                                @Override
                                public void link(HintedArrayAdapter.ObjectContainer data) {
                                    mutationElement.setAgent((FundsMutationAgent) data.getObject());
                                }
                            }, mutationHighlighter),
                            new Supplier<FundsMutationAgent>() {
                                @Override
                                public FundsMutationAgent get() {
                                    return mutationElement.getAgent();
                                }
                            }
                    ))
                    .build();

    @Override
    protected final FragmentsInfoProvider getInfoProvider() {
        return infoProvider;
    }

    @Override
    protected final int getLayoutId() {
        return R.layout.activity_funds_mutation;
    }

    @Override
    protected final int getMenuId() {
        return R.menu.menu_funds_mutation;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mutationHighlighter.addElementInfo(FundsMutationElementCore.FIELD_QUANTITY, findViewById(R.id.funds_mutation_quantity_info));
        CoreNotifier.addLink(this, findViewById(R.id.funds_mutation_quantity), new CoreNotifier.NumberLinker() {
            @Override
            public void link(Number data) {
                mutationElement.setQuantity(data.intValue());
            }
        });
        mutationHighlighter.addElementInfo(FundsMutationElementCore.FIELD_DIRECTION, findViewById(R.id.funds_mutation_direction_radio_info));
        CoreNotifier.addLink(this, findViewById(R.id.funds_mutation_direction_radio), new CoreNotifier.NumberLinker() {
            @Override
            public void link(Number data) {
                mutationElement.setDirection(FundsMutator.MutationDirection.values()[data.intValue()]);
            }
        });

        final View infoView = findViewById(R.id.funds_mutation_info);
        mutationHighlighter.setGlobalInfoView(infoView);
        setGlobalInfoViewPerFragment(R.id.funds_mutation_subject_fragment, FundsSubjectFragment.BUTTON_NEW_SUBJECT_SUBMIT, infoView);
        setGlobalInfoViewPerFragment(R.id.funds_mutation_relevant_balance_fragment, AccountStandardFragment.BUTTON_NEW_ACCOUNT_SUBMIT, infoView);
        setGlobalInfoViewPerFragment(R.id.funds_mutation_agent_fragment, FundsAgentFragment.BUTTON_NEW_AGENT_SUBMIT, infoView);
    }

    private void setGlobalInfoViewPerFragment(@IdRes int fragmentId, String buttonName, View infoView) {
        infoProvider.getSubmitInfo(fragmentId, buttonName).errorHighlighter.setGlobalInfoView(infoView);
    }

    @Override
    protected final void activityInnerFeedback() {
        textViewFeedback(String.valueOf(mutationElement.getQuantity()), R.id.funds_mutation_quantity);
        radioGroupFeedback(mutationElement.getDirection(), R.id.funds_mutation_direction_radio);
        dateTimeFeedback(mutationElement.getTimestamp(), R.id.funds_mutation_date, R.id.funds_mutation_time);
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

    public void mutate(View view) {
        new AsyncTask<FundsMutationElementCore, Void, Submitter.Result>() {
            @Override
            protected Submitter.Result doInBackground(FundsMutationElementCore[] params) {
                return params[0].submit();
            }

            @Override
            protected void onPostExecute(Submitter.Result result) {
                mutationHighlighter.processSubmitResult(result);
                if (result.isSuccessful()) {
                    //TODO: find a way to update balance account spinner with new balance amount if that was changed (AND don't forget to update BalancesUiThreadState!)
                }
                findViewById(R.id.activity_funds_mutation).invalidate();
            }
        }.execute(mutationElement);
    }

}
