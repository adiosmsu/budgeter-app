package ru.adios.budgeter.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.threeten.bp.OffsetDateTime;

import java8.util.function.Supplier;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.PriceAdditionElementCore;
import ru.adios.budgeter.R;
import ru.adios.budgeter.Submitter;
import ru.adios.budgeter.adapters.HintedArrayAdapter;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.api.data.SubjectPrice;
import ru.adios.budgeter.core.CollectedFragmentsInfoProvider;
import ru.adios.budgeter.core.CoreElementActivity;
import ru.adios.budgeter.core.CoreErrorHighlighter;
import ru.adios.budgeter.core.CoreNotifier;
import ru.adios.budgeter.core.Feedbacking;
import ru.adios.budgeter.fragments.EnterAmountFragment;
import ru.adios.budgeter.fragments.FundsAgentFragment;
import ru.adios.budgeter.fragments.FundsSubjectFragment;
import ru.adios.budgeter.widgets.DateEditView;

public class AddPriceActivity extends CoreElementActivity {

    public static final String KEY_HIGHLIGHTER = "add_price_act_high";

    private final PriceAdditionElementCore priceElement =
            new PriceAdditionElementCore(BundleProvider.getBundle().subjectPrices(), BundleProvider.getBundle().fundsMutationSubjects());
    private final CoreErrorHighlighter priceHighlighter = new CoreErrorHighlighter(KEY_HIGHLIGHTER);

    private final CollectedFragmentsInfoProvider infoProvider = new CollectedFragmentsInfoProvider.Builder(this)
            .addProvider(
                    FundsSubjectFragment.getInfoProvider(
                            R.id.price_subject_fragment,
                            this,
                            null,
                            new CoreElementFieldInfo(PriceAdditionElementCore.FIELD_SUBJECT, new CoreNotifier.HintedLinker() {
                                @Override
                                public boolean link(HintedArrayAdapter.ObjectContainer data) {
                                    final FundsMutationSubject object = (FundsMutationSubject) data.getObject();
                                    final FundsMutationSubject prev = priceElement.getSubject();
                                    if ((prev == null && object != null) || (prev != null && !prev.equals(object))) {
                                        priceElement.setSubject(object);
                                        return true;
                                    }
                                    return false;
                                }
                            }, priceHighlighter),
                            new Supplier<FundsMutationSubject>() {
                                @Override
                                public FundsMutationSubject get() {
                                    return priceElement.getSubject();
                                }
                            }
                    )
            )
            .addProvider(
                    FundsAgentFragment.getInfoProvider(
                            R.id.price_agent_fragment,
                            null,
                            new CoreElementFieldInfo(PriceAdditionElementCore.FIELD_AGENT, new CoreNotifier.HintedLinker() {
                                @Override
                                public boolean link(HintedArrayAdapter.ObjectContainer data) {
                                    final FundsMutationAgent object = (FundsMutationAgent) data.getObject();
                                    final FundsMutationAgent prev = priceElement.getAgent();
                                    if ((prev == null && object != null) || (prev != null && !prev.equals(object))) {
                                        priceElement.setAgent(object);
                                        return true;
                                    }
                                    return false;
                                }
                            }, priceHighlighter),
                            new Supplier<FundsMutationAgent>() {
                                @Override
                                public FundsMutationAgent get() {
                                    return priceElement.getAgent();
                                }
                            }
                    )
            )
            .addProvider(
                    EnterAmountFragment.getInfoProvider(
                            R.id.price_amount_fragment,
                            priceElement.getPriceSettable(),
                            priceHighlighter,
                            PriceAdditionElementCore.FIELD_PRICE_AMOUNT,
                            PriceAdditionElementCore.FIELD_PRICE_UNIT
                    )
            )
            .build();

    private DateEditView dateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dateView = (DateEditView) findViewById(R.id.price_date);
        dateView.init(this, savedInstanceState);
        priceHighlighter.addElementInfo(PriceAdditionElementCore.FIELD_DAY, findViewById(R.id.price_date_info));
        CoreNotifier.addLink(this, dateView, new CoreNotifier.ArbitraryLinker() {
            @Override
            public boolean link(Object data) {
                if (data instanceof OffsetDateTime) {
                    final UtcDay object = new UtcDay((OffsetDateTime) data);
                    if (!object.equals(priceElement.getDay())) {
                        priceElement.setDay(object);
                        return true;
                    }
                }
                return false;
            }
        });

        final View infoView = findViewById(R.id.price_info);
        priceHighlighter.setGlobalInfoView(infoView);
        setGlobalInfoViewPerFragment(R.id.price_subject_fragment, FundsSubjectFragment.BUTTON_NEW_SUBJECT_SUBMIT, infoView);
        setGlobalInfoViewPerFragment(R.id.price_agent_fragment, FundsAgentFragment.BUTTON_NEW_AGENT_SUBMIT, infoView);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        dateView.retain(this, outState);
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

    @Override
    protected void activityInnerFeedback() {
        Feedbacking.dateFeedback(priceElement.getDay(), dateView);
    }

    @Override
    protected FragmentsInfoProvider getInfoProvider() {
        return infoProvider;
    }

    @Override
    @LayoutRes
    protected int getLayoutId() {
        return R.layout.activity_add_price;
    }

    @Override
    @MenuRes
    protected int getMenuResource() {
        return R.menu.menu_add_price;
    }

    public void addPrice(View view) {
        priceElement.lock();
        new AsyncTask<PriceAdditionElementCore, Void, PriceAdditionElementCore>() {
            @Override
            protected PriceAdditionElementCore doInBackground(PriceAdditionElementCore... params) {
                final PriceAdditionElementCore core = params[0];
                core.submitAndStoreResult();
                return core;
            }

            @Override
            protected void onPostExecute(PriceAdditionElementCore core) {
                final Submitter.Result<SubjectPrice> storedResult = core.getStoredResult();

                priceHighlighter.processSubmitResult(storedResult);
                if (storedResult.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), R.string.price_add_success, Toast.LENGTH_SHORT)
                            .show();
                }

                finishSubmit(core, R.id.activity_add_price);
            }
        }.execute(priceElement);
    }

}
