package ru.adios.budgeter.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.annotation.UiThread;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import org.joda.money.Money;

import java8.util.function.Supplier;
import ru.adios.budgeter.BalancesUiThreadState;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.FundsAdditionElementCore;
import ru.adios.budgeter.R;
import ru.adios.budgeter.Submitter;
import ru.adios.budgeter.adapters.HintedArrayAdapter;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.core.CollectedFragmentsInfoProvider;
import ru.adios.budgeter.core.CollectibleFragmentInfoProvider;
import ru.adios.budgeter.core.CoreElementActivity;
import ru.adios.budgeter.core.CoreErrorHighlighter;
import ru.adios.budgeter.core.CoreNotifier;
import ru.adios.budgeter.core.CoreUtils;
import ru.adios.budgeter.fragments.AccountStandardFragment;
import ru.adios.budgeter.fragments.EnterAmountFragment;
import ru.adios.budgeter.util.UiUtils;

@UiThread
public class AddFundsActivity extends CoreElementActivity {

    public static final String KEY_HIGHLIGHTER = "add_funds_act_high";

    private final FundsAdditionElementCore additionElement = new FundsAdditionElementCore(BundleProvider.getBundle().treasury());
    private final CoreErrorHighlighter addFundsErrorHighlighter = new CoreErrorHighlighter(KEY_HIGHLIGHTER);
    private final CollectibleFragmentInfoProvider<BalanceAccount, AccountStandardFragment.HybridAccountCore> accountsInfoProvider =
            AccountStandardFragment.getInfoProviderBuilder(R.id.add_funds_account_fragment, this, new Supplier<BalanceAccount>() {
                @Override
                public BalanceAccount get() {
                    return additionElement.getAccount();
                }
            })
                    .provideAccountFieldInfo(FundsAdditionElementCore.FIELD_ACCOUNT, addFundsErrorHighlighter, new CoreNotifier.HintedLinker() {
                        @Override
                        public boolean link(HintedArrayAdapter.ObjectContainer data) {
                            final BalanceAccount object = (BalanceAccount) data.getObject();
                            final BalanceAccount prev = additionElement.getAccount();
                            if ((prev == null && object != null) || (prev != null && !prev.equals(object))) {
                                additionElement.setAccount(object);
                                return true;
                            }
                            return false;
                        }
                    })
                    .build();

    private final CollectedFragmentsInfoProvider infoProvider =
            new CollectedFragmentsInfoProvider.Builder(this)
                    .addProvider(EnterAmountFragment.getInfoProvider(
                            R.id.add_funds_amount_fragment,
                            additionElement,
                            addFundsErrorHighlighter,
                            FundsAdditionElementCore.FIELD_AMOUNT_DECIMAL,
                            FundsAdditionElementCore.FIELD_AMOUNT_UNIT))
                    .addProvider(accountsInfoProvider)
                    .build();

    @Override
    protected final FragmentsInfoProvider getInfoProvider() {
        return infoProvider;
    }

    @Override
    @LayoutRes
    protected final int getLayoutId() {
        return R.layout.activity_add_funds;
    }

    @Override
    @MenuRes
    protected final int getMenuResource() {
        return R.menu.menu_add_funds;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final View infoView = findViewById(R.id.add_funds_info);
        setGlobalViewToHighlighter(accountsInfoProvider.getSubmitInfo(AccountStandardFragment.BUTTON_NEW_ACCOUNT_SUBMIT).errorHighlighter, infoView);
        setGlobalViewToHighlighter(addFundsErrorHighlighter, infoView);
    }

    private static void setGlobalViewToHighlighter(CoreErrorHighlighter highlighter, View infoView) {
        highlighter.setGlobalInfoView(infoView);
        highlighter.setWorker(infoView, new CoreErrorHighlighter.ViewWorker() {
            @Override
            public void successWork(View view) {
                setLayoutParams(view, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 0));
            }

            @Override
            public void failureWork(View view) {
                setLayoutParams(view, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
            }

            private void setLayoutParams(View view, RelativeLayout.LayoutParams pars) {
                pars.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                pars.addRule(RelativeLayout.BELOW, R.id.add_funds_amount_fragment);
                view.setLayoutParams(pars);
            }
        });
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

    public void addFunds(View view) {
        additionElement.lock();
        new AsyncTask<FundsAdditionElementCore, Void, FundsAdditionElementCore>() {
            @Override
            protected FundsAdditionElementCore doInBackground(FundsAdditionElementCore[] params) {
                final FundsAdditionElementCore core = params[0];
                CoreUtils.doSubmitAndStore(core);
                return core;
            }

            @Override
            protected void onPostExecute(FundsAdditionElementCore core) {
                final Submitter.Result<BalanceAccount> result = core.getStoredResult();

                addFundsErrorHighlighter.processSubmitResult(result);
                if (result.isSuccessful()) {
                    UiUtils.replaceAccountInSpinner(result.submitResult, (Spinner) findViewById(R.id.accounts_spinner), getResources());
                    //noinspection ConstantConditions
                    BalancesUiThreadState.addMoney(Money.of(additionElement.getAmountUnit(), additionElement.getAmountDecimal()), AddFundsActivity.this);
                    Toast.makeText(getApplicationContext(), R.string.funds_add_success, Toast.LENGTH_SHORT)
                            .show();
                }

                finishSubmit(core, R.id.activity_add_funds);
            }
        }.execute(additionElement);
    }

}
