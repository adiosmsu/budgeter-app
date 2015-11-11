package ru.adios.budgeter.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.UiThread;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;

import java8.util.function.Supplier;
import ru.adios.budgeter.BalancesUiThreadState;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.Constants;
import ru.adios.budgeter.FundsMutationElementCore;
import ru.adios.budgeter.FundsMutator;
import ru.adios.budgeter.R;
import ru.adios.budgeter.Submitter;
import ru.adios.budgeter.adapters.HintedArrayAdapter;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;
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
import ru.adios.budgeter.fragments.FundsSubjectFragment;
import ru.adios.budgeter.util.UiUtils;

@UiThread
public class FundsMutationActivity extends CoreElementActivity {

    public static final String KEY_HIGHLIGHTER = "fu_ma_act_high";
    public static final String KEY_COST = "fu_ma_cost_key";
    public static final String KEY_PAY = "fu_ma_pay_key";
    public static final String KEY_NATURAL_RATE = "fu_ma_nr_key";
    public static final String KEY_CUSTOM_RATE = "fu_ma_cr_key";

    private final FundsMutationElementCore mutationElement =
            new FundsMutationElementCore(Constants.ACCOUNTER, BundleProvider.getBundle().treasury(), Constants.CURRENCIES_EXCHANGE_SERVICE.getExchangeService());
    private final CoreErrorHighlighter mutationHighlighter = new CoreErrorHighlighter(KEY_HIGHLIGHTER);

    private final CollectedFragmentsInfoProvider infoProvider =
            new CollectedFragmentsInfoProvider.Builder(this)
                    .addProvider(FundsSubjectFragment.getInfoProvider(
                            R.id.funds_mutation_subject_fragment,
                            this,
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
                            mutationElement.getPayeeMoneySettable(),
                            mutationHighlighter,
                            FundsMutationElementCore.FIELD_PAYEE_AMOUNT,
                            FundsMutationElementCore.FIELD_PAYEE_ACCOUNT_UNIT
                    ))
                    .addProvider(
                            AccountStandardFragment.getInfoProviderBuilder(R.id.funds_mutation_relevant_balance_fragment, this, new Supplier<BalanceAccount>() {
                                @Override
                                public BalanceAccount get() {
                                    return mutationElement.getRelevantBalance();
                                }
                            })
                                    .provideAccountFieldInfo(FundsMutationElementCore.FIELD_RELEVANT_BALANCE, mutationHighlighter, new CoreNotifier.HintedLinker() {
                                        @Override
                                        public void link(HintedArrayAdapter.ObjectContainer data) {
                                            mutationElement.setRelevantBalance((BalanceAccount) data.getObject());
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
                    .addProvider(DateTimeFragment.getInfoProvider(R.id.funds_mutation_datetime_fragment, mutationHighlighter, FundsMutationElementCore.FIELD_TIMESTAMP, mutationElement))
                    .build();

    private final CheckedChangeListener paidMoneyListener = new CheckedChangeListener(R.id.funds_mutation_paid_amount_fragment, false);
    private final CheckedChangeListener costListener = new CheckedChangeListener(R.id.funds_mutation_subject_cost_fragment, true);
    private TextView fundsMutationNaturalRate;
    private TextView fundsMutationCustomRate;
    private TextView fundsMutationQuantity;
    private RadioGroup fundsMutationDirectionRadio;

    // transient state
    private boolean costShown = true;
    private boolean payShown = true;
    private BigDecimal naturalRateVal;
    private BigDecimal customRateVal;
    // end of transient state

    @Override
    protected final FragmentsInfoProvider getInfoProvider() {
        return infoProvider;
    }

    @Override
    protected final int getLayoutId() {
        return R.layout.activity_funds_mutation;
    }

    @Override
    protected final int getMenuResource() {
        return R.menu.menu_funds_mutation;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_COST)) {
                costShown = savedInstanceState.getBoolean(KEY_COST);
            }
            if (savedInstanceState.containsKey(KEY_PAY)) {
                payShown = savedInstanceState.getBoolean(KEY_PAY);
            }
            final String nrStr = savedInstanceState.getString(KEY_NATURAL_RATE);
            if (nrStr != null) {
                naturalRateVal = new BigDecimal(nrStr);
            }
            final String crStr = savedInstanceState.getString(KEY_CUSTOM_RATE);
            if (crStr != null) {
                customRateVal = new BigDecimal(crStr);
            }
        }

        if (costShown) {
            findViewById(R.id.funds_mutation_subject_cost_fragment).setVisibility(View.VISIBLE);
        } else {
            hideFrag(findViewById(R.id.funds_mutation_subject_cost_fragment));
        }
        if (payShown) {
            findViewById(R.id.funds_mutation_paid_amount_fragment).setVisibility(View.VISIBLE);
        } else {
            hideFrag(findViewById(R.id.funds_mutation_paid_amount_fragment));
        }

        if (naturalRateVal != null) {
            fundsMutationNaturalRate.setText(naturalRateVal.toPlainString());
            mutationElement.setNaturalRate(naturalRateVal);
        }
        if (customRateVal != null) {
            fundsMutationCustomRate.setText(customRateVal.toPlainString());
            mutationElement.setCustomRate(customRateVal);
        }

        mutationHighlighter.addElementInfo(FundsMutationElementCore.FIELD_QUANTITY, findViewById(R.id.funds_mutation_quantity_info));
        CoreNotifier.addLink(this, fundsMutationQuantity, new CoreNotifier.NumberLinker() {
            @Override
            public void link(Number data) {
                mutationElement.setQuantity(data.intValue());
            }
        });
        mutationHighlighter.addElementInfo(FundsMutationElementCore.FIELD_DIRECTION, findViewById(R.id.funds_mutation_direction_radio_info));
        CoreNotifier.addLink(this, fundsMutationDirectionRadio, new CoreNotifier.NumberLinker() {
            @Override
            public void link(Number data) {
                mutationElement.setDirection(FundsMutator.MutationDirection.values()[data.intValue()]);
            }
        });
        CoreNotifier.addLink(this, fundsMutationNaturalRate, new CoreNotifier.DecimalLinker() {
            @Override
            public void link(BigDecimal data) {
                naturalRateVal = data;
                mutationElement.setNaturalRate(data);
            }
        });
        CoreNotifier.addLink(this, fundsMutationCustomRate, new CoreNotifier.DecimalLinker() {
            @Override
            public void link(BigDecimal data) {
                customRateVal = data;
                mutationElement.setCustomRate(data);
            }
        });

        final CheckBox paidAmountBox = (CheckBox) findViewById(R.id.funds_mutation_paid_amount_box);
        paidAmountBox.setOnCheckedChangeListener(paidMoneyListener);
        final CheckBox costBox = (CheckBox) findViewById(R.id.funds_mutation_subject_cost_box);
        costBox.setOnCheckedChangeListener(costListener);

        final View infoView = findViewById(R.id.funds_mutation_info);
        mutationHighlighter.setGlobalInfoView(infoView);
        setGlobalInfoViewPerFragment(R.id.funds_mutation_subject_fragment, FundsSubjectFragment.BUTTON_NEW_SUBJECT_SUBMIT, infoView);
        setGlobalInfoViewPerFragment(R.id.funds_mutation_relevant_balance_fragment, AccountStandardFragment.BUTTON_NEW_ACCOUNT_SUBMIT, infoView);
        setGlobalInfoViewPerFragment(R.id.funds_mutation_agent_fragment, FundsAgentFragment.BUTTON_NEW_AGENT_SUBMIT, infoView);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_COST, costShown);
        outState.putBoolean(KEY_PAY, payShown);
        outState.putString(KEY_NATURAL_RATE, naturalRateVal != null ? naturalRateVal.toPlainString() : null);
        outState.putString(KEY_CUSTOM_RATE, customRateVal != null ? customRateVal.toPlainString() : null);
    }

    @Override
    protected void clearViewReferences() {
        fundsMutationNaturalRate = null;
        fundsMutationCustomRate = null;
        fundsMutationQuantity = null;
        fundsMutationDirectionRadio = null;
    }

    @Override
    protected void collectEssentialViews() {
        fundsMutationNaturalRate = (TextView) findViewById(R.id.funds_mutation_natural_rate);
        fundsMutationCustomRate = (TextView) findViewById(R.id.funds_mutation_custom_rate);
        fundsMutationQuantity = (TextView) findViewById(R.id.funds_mutation_quantity);
        fundsMutationDirectionRadio = (RadioGroup) findViewById(R.id.funds_mutation_direction_radio);
    }

    @Override
    protected final void activityInnerFeedback() {
        Feedbacking.decimalTextViewFeedback(mutationElement.getNaturalRate(), fundsMutationNaturalRate);
        Feedbacking.decimalTextViewFeedback(mutationElement.getCustomRate(), fundsMutationCustomRate);
        Feedbacking.textViewFeedback(String.valueOf(mutationElement.getQuantity()), fundsMutationQuantity);
        Feedbacking.radioGroupFeedback(mutationElement.getDirection(), fundsMutationDirectionRadio);
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

    public void mutate(View view) {
        mutationElement.lock();
        new AsyncTask<FundsMutationElementCore, Void, FundsMutationElementCore>() {
            @Override
            protected FundsMutationElementCore doInBackground(FundsMutationElementCore[] params) {
                final FundsMutationElementCore core = params[0];
                CoreUtils.doSubmitAndStore(core);
                return core;
            }

            @Override
            protected void onPostExecute(FundsMutationElementCore core) {
                final Submitter.Result<BalanceAccount> result = core.getStoredResult();

                mutationHighlighter.processSubmitResult(result);
                if (result.isSuccessful() && result.submitResult != null) {
                    UiUtils.replaceAccountInSpinner(result.submitResult, (Spinner) findViewById(R.id.accounts_spinner), getResources());
                    BalancesUiThreadState.addMoney(mutationElement.getSubmittedMoney(), FundsMutationActivity.this);
                    Toast.makeText(getApplicationContext(), R.string.register_mutation_success, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.register_mutation_failure, Toast.LENGTH_SHORT)
                            .show();
                }


                processResultDependentCheckBox(R.id.funds_mutation_paid_amount_box, result,
                        FundsMutationElementCore.FIELD_PAID_MONEY, FundsMutationElementCore.FIELD_PAYEE_ACCOUNT_UNIT, FundsMutationElementCore.FIELD_PAYEE_AMOUNT);
                processResultDependentCheckBox(R.id.funds_mutation_subject_cost_box, result,
                        FundsMutationElementCore.FIELD_AMOUNT, FundsMutationElementCore.FIELD_AMOUNT_DECIMAL, FundsMutationElementCore.FIELD_AMOUNT_UNIT);

                finishSubmit(core, R.id.activity_funds_mutation);
            }

            private void processResultDependentCheckBox(@IdRes int boxId, Submitter.Result result, String... fieldNames) {
                final CheckBox box = (CheckBox) findViewById(boxId);
                if (box.isChecked() && result.containsFieldErrors(fieldNames)) {
                    box.setChecked(false);
                }
            }
        }.execute(mutationElement);
    }

    private final class CheckedChangeListener implements CompoundButton.OnCheckedChangeListener {

        @IdRes
        private final int fragId;
        private boolean costOrPay;

        public CheckedChangeListener(@IdRes int fragId, boolean costOrPay) {
            this.fragId = fragId;
            this.costOrPay = costOrPay;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            final View paFragment = findViewById(fragId);
            if (isChecked) {
                if (costOrPay) {
                    costShown = false;
                } else {
                    payShown = false;
                }
                hideFrag(paFragment);
            } else {
                if (costOrPay) {
                    costShown = true;
                } else {
                    payShown = true;
                }
                paFragment.setVisibility(View.VISIBLE);
            }
            paFragment.invalidate();
        }

    }

    private void hideFrag(View paFragment) {
        paFragment.setVisibility(View.INVISIBLE);
        ((TextView) paFragment.findViewById(R.id.amount_decimal)).setText(null);
        final Spinner curSp = (Spinner) paFragment.findViewById(R.id.amount_currency);
        curSp.setSelection(curSp.getAdapter().getCount());
    }

}
