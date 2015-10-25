package ru.adios.budgeter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import org.joda.money.Money;

import java8.util.function.Supplier;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.util.BalancesUiThreadState;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.HintedArrayAdapter;
import ru.adios.budgeter.util.UiUtils;

public class AddFundsActivity extends CoreElementActivity {

    private final FundsAdditionElementCore additionElement = new FundsAdditionElementCore(Schema.TREASURY);
    private final CoreErrorHighlighter addFundsErrorHighlighter = new CoreErrorHighlighter();
    private final CollectibleFragmentInfoProvider<Treasury.BalanceAccount, AccountStandardFragment.HybridAccountCore> accountsInfoProvider =
            AccountStandardFragment.getInfoProviderBuilder(R.id.add_funds_account_fragment, this, new Supplier<Treasury.BalanceAccount>() {
                @Override
                public Treasury.BalanceAccount get() {
                    return additionElement.getAccount();
                }
            })
                    .provideAccountFieldInfo(FundsAdditionElementCore.FIELD_ACCOUNT, addFundsErrorHighlighter, new CoreNotifier.HintedLinker() {
                        @Override
                        public void link(HintedArrayAdapter.ObjectContainer data) {
                            additionElement.setAccount((Treasury.BalanceAccount) data.getObject());
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
    protected final int getMenuId() {
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
                core.submitAndStoreResult();
                return core;
            }

            @Override
            protected void onPostExecute(FundsAdditionElementCore core) {
                final Submitter.Result<Treasury.BalanceAccount> result = core.getStoredResult();

                addFundsErrorHighlighter.processSubmitResult(result);
                if (result.isSuccessful()) {
                    UiUtils.replaceAccountInSpinner(result.submitResult, (Spinner) findViewById(R.id.accounts_spinner));
                    //noinspection ConstantConditions
                    BalancesUiThreadState.addMoney(Money.of(additionElement.getAmountUnit(), additionElement.getAmountDecimal()), AddFundsActivity.this);
                }

                finishSubmit(core, R.id.activity_add_funds);
            }
        }.execute(additionElement);
    }

}
