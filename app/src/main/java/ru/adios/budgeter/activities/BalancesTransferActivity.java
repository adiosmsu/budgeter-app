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
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.v7.widget.AppCompatSpinner;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayList;

import javax.annotation.Nullable;

import java8.util.Optional;
import java8.util.OptionalInt;
import java8.util.function.Function;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import ru.adios.budgeter.BalancesTransferCore;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.R;
import ru.adios.budgeter.Submitter;
import ru.adios.budgeter.adapters.NullableDecoratingAdapter;
import ru.adios.budgeter.adapters.Presenters;
import ru.adios.budgeter.adapters.StringPresenter;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.core.CollectedFragmentsInfoProvider;
import ru.adios.budgeter.core.CollectibleFragmentInfoProvider;
import ru.adios.budgeter.core.CoreElementActivity;
import ru.adios.budgeter.core.CoreErrorHighlighter;
import ru.adios.budgeter.core.CoreNotifier;
import ru.adios.budgeter.core.CoreUtils;
import ru.adios.budgeter.core.Feedbacking;
import ru.adios.budgeter.fragments.AccountStandardFragment;
import ru.adios.budgeter.fragments.EnterAmountFragment;
import ru.adios.budgeter.util.EmptyOnItemSelectedListener;
import ru.adios.budgeter.util.UiUtils;

public class BalancesTransferActivity extends CoreElementActivity {

    public static final String KEY_HIGHLIGHTER = "balances_transfer_act_high";
    public static final String KEY_SELECTED_RECEIVER_ACCOUNT = "bt_select_rec_acc_key";
    public static final String KEY_SUGGESTED_RECEIVERS = "bt_sug_recs_key";


    private final Treasury treasury = BundleProvider.getBundle().treasury();
    private final BalancesTransferCore transferElement = new BalancesTransferCore(treasury);
    private final CoreErrorHighlighter transferErrorHighlighter = new CoreErrorHighlighter(KEY_HIGHLIGHTER);

    private final CollectibleFragmentInfoProvider<BalanceAccount, AccountStandardFragment.HybridAccountCore> senderAccountInfoProvider =
            AccountStandardFragment.getInfoProviderBuilder(R.id.balances_transfer_sender_account_fragment, this, new Supplier<BalanceAccount>() {
                @Override
                public BalanceAccount get() {
                    return transferElement.getSenderAccount();
                }
            })
                    .provideAccountFieldInfo(BalancesTransferCore.FIELD_SENDER_ACCOUNT, transferErrorHighlighter, new CoreNotifier.ArbitraryLinker() {
                        @Override
                        public boolean link(Object data) {
                            final BalanceAccount object = (BalanceAccount) data;
                            final BalanceAccount prev = transferElement.getSenderAccount();

                            if ((prev == null && object != null) || (prev != null && !prev.equals(object))) {
                                transferElement.setSenderAccount(object);
                                new AsyncTask<BalancesTransferCore, Void, ImmutableList<BalanceAccount>>() {
                                    @Override
                                    protected ImmutableList<BalanceAccount> doInBackground(BalancesTransferCore... params) {
                                        final BalancesTransferCore core = params[0];
                                        return ImmutableList.copyOf(core.getSuggestedAccountsStream().map(new Function<BalanceAccount, BalanceAccount>() {
                                            @Override
                                            public BalanceAccount apply(final BalanceAccount balanceAccount) {
                                                return balanceAccount.id.isPresent() ? balanceAccount : treasury.getAccountWithId(balanceAccount);
                                            }
                                        }).collect(Collectors.<BalanceAccount>toList()));
                                    }

                                    @Override
                                    protected void onPostExecute(ImmutableList<BalanceAccount> result) {
                                        final ImmutableList<Long> ids = getIdsFromAccountsList(result);
                                        if (!suggestedReceivers.equals(ids)) {
                                            suggestedReceivers = ids;
                                            selectedReceiverAccount = 0;

                                            if (receiverAccountSpinner == null) {
                                                collectEssentialViews();
                                            }

                                            fillReceiversSpinner(result);
                                            receiverAccountSpinner.setSelection(0);
                                            receiverAccountSpinner.setVisibility(View.VISIBLE);
                                            receiverAccountInfo.setVisibility(View.INVISIBLE);
                                        }
                                    }
                                }.execute(transferElement);
                                return true;
                            }
                            return false;
                        }
                    })
                    .build();

    private final CollectedFragmentsInfoProvider infoProvider =
            new CollectedFragmentsInfoProvider.Builder(this)
                    .addProvider(EnterAmountFragment.getInfoProvider(
                            R.id.balances_transfer_amount_fragment,
                            transferElement,
                            transferErrorHighlighter,
                            BalancesTransferCore.FIELD_TRANSFER_AMOUNT_DECIMAL,
                            BalancesTransferCore.FIELD_TRANSFER_AMOUNT_UNIT))
                    .addProvider(senderAccountInfoProvider)
                    .build();


    // transient state
    private int selectedReceiverAccount = -1;
    private ImmutableList<Long> suggestedReceivers = ImmutableList.of();
    // end of transient state

    private AppCompatSpinner receiverAccountSpinner;
    private TextView receiverAccountInfo;

    @Override
    protected final FragmentsInfoProvider getInfoProvider() {
        return infoProvider;
    }

    @Override
    @LayoutRes
    protected final int getLayoutId() {
        return R.layout.activity_balances_transfer;
    }

    @Override
    @MenuRes
    protected final int getMenuResource() {
        return R.menu.menu_balances_transfer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            selectedReceiverAccount = savedInstanceState.getInt(KEY_SELECTED_RECEIVER_ACCOUNT, -1);
            final long[] ids = savedInstanceState.getLongArray(KEY_SUGGESTED_RECEIVERS);
            if (ids == null) {
                suggestedReceivers = ImmutableList.of();
            } else {
                ImmutableList.Builder<Long> builder = ImmutableList.builder();
                for (final long i : ids) {
                    builder.add(i);
                }
                suggestedReceivers = builder.build();
            }
        }

        if (selectedReceiverAccount >= 0) {
            final ImmutableList<Long> receiversSnapshot = ImmutableList.copyOf(suggestedReceivers);
            new AsyncTask<Long[], Void, ImmutableList<BalanceAccount>>() {
                @Override
                protected ImmutableList<BalanceAccount> doInBackground(Long[]... params) {
                    final Long[] ids = params[0];
                    final ImmutableList.Builder<BalanceAccount> builder = ImmutableList.builder();
                    for (final Long i : ids) {
                        final Optional<BalanceAccount> byId = treasury.getById(i);
                        if (byId.isPresent()) {
                            builder.add(byId.get());
                        }
                    }
                    return builder.build();
                }

                @Override
                protected void onPostExecute(ImmutableList<BalanceAccount> balanceAccounts) {
                    if (receiversSnapshot.equals(suggestedReceivers)) {
                        fillReceiversSpinner(balanceAccounts);
                        if (receiverAccountSpinner != null) {
                            receiverAccountSpinner.setSelection(selectedReceiverAccount);
                            receiverAccountSpinner.setVisibility(View.VISIBLE);
                            receiverAccountInfo.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            }.execute(receiversSnapshot.toArray(new Long[receiversSnapshot.size()]));
        }

        collectEssentialViews();
        receiverAccountSpinner.setOnItemSelectedListener(new EmptyOnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedReceiverAccount = parent.getAdapter().getCount() > position ? position : -1;
            }
        });
        transferErrorHighlighter.addElementInfo(BalancesTransferCore.FIELD_RECEIVER_ACCOUNT, receiverAccountInfo);
        CoreNotifier.addLink(this, receiverAccountSpinner, new CoreNotifier.ArbitraryLinker() {
            @Override
            public boolean link(Object data) {
                final BalanceAccount object = (BalanceAccount) data;
                final BalanceAccount prev = transferElement.getReceiverAccount();

                if ((prev == null && object != null) || (prev != null && !prev.equals(object))) {
                    transferElement.setReceiverAccount(object);
                    return true;
                }
                return false;
            }
        });

        final View infoView = findViewById(R.id.balances_transfer_info);
        senderAccountInfoProvider.getSubmitInfo(AccountStandardFragment.BUTTON_NEW_ACCOUNT_SUBMIT).errorHighlighter.setGlobalInfoView(infoView);
        transferErrorHighlighter.setGlobalInfoView(infoView);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_RECEIVER_ACCOUNT, selectedReceiverAccount);
        outState.putLongArray(KEY_SUGGESTED_RECEIVERS, transformLongList(suggestedReceivers));
    }

    @Nullable
    private long[] transformLongList(ImmutableList<Long> list) {
        int listSize = list.size();
        if (listSize == 0) {
            return null;
        }
        final long[] res = new long[listSize];
        for (int i1 = 0; i1 < listSize; i1++) {
            final Long i = list.get(i1);
            res[i1] = i != null ? i : -1;
        }
        return res;
    }

    @Override
    protected void clearViewReferences() {
        receiverAccountSpinner = null;
        receiverAccountInfo = null;
    }

    @Override
    protected void collectEssentialViews() {
        receiverAccountSpinner = (AppCompatSpinner) findViewById(R.id.balances_transfer_receiver_account_spinner);
        receiverAccountInfo = (TextView) findViewById(R.id.balances_transfer_receiver_account_spinner_info);
    }

    @Override
    protected final void activityInnerFeedback() {
        if (transferElement.getSenderAccount() == null && selectedReceiverAccount >= 0 && receiverAccountSpinner.getVisibility() != View.GONE) {
            selectedReceiverAccount = -1;
            suggestedReceivers = ImmutableList.of();
            receiverAccountSpinner.setVisibility(View.GONE);
            receiverAccountInfo.setVisibility(View.GONE);
        } else {
            Feedbacking.nullableArraySpinnerFeedback(transferElement.getReceiverAccount(), receiverAccountSpinner);
        }
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

    public void transfer(View view) {
        transferElement.lock();
        new AsyncTask<BalancesTransferCore, Void, BalancesTransferCore>() {
            @Override
            protected BalancesTransferCore doInBackground(BalancesTransferCore[] params) {
                final BalancesTransferCore core = params[0];
                CoreUtils.doSubmitAndStore(core);
                return core;
            }

            @Override
            protected void onPostExecute(BalancesTransferCore core) {
                Submitter.Result<BalancesTransferCore.AccountsPair> result = core.getStoredResult();

                transferErrorHighlighter.processSubmitResult(result);
                if (result.isSuccessful()) {
                    //noinspection ConstantConditions
                    UiUtils.replaceAccountInSpinner(result.submitResult.sender, (AppCompatSpinner) findViewById(R.id.accounts_spinner));
                    //noinspection ConstantConditions
                    UiUtils.replaceAccountInSpinner(result.submitResult.receiver, receiverAccountSpinner);
                    Toast.makeText(getApplicationContext(), R.string.balances_transfer_success, Toast.LENGTH_SHORT)
                            .show();
                }

                finishSubmit(core, R.id.activity_balances_transfer);
            }
        }.execute(transferElement);
    }

    void fillReceiversSpinner(ImmutableList<BalanceAccount> contents) {
        if (receiverAccountSpinner != null) {
            NullableDecoratingAdapter.adaptSpinnerWithArrayWrapper(
                    receiverAccountSpinner,
                    Optional.<StringPresenter<BalanceAccount>>of(Presenters.getBalanceAccountDefaultPresenter(getResources())),
                    new ArrayList<>(contents),
                    OptionalInt.of(R.string.accounts_spinner_null_val)
            );
        }
    }

    static ImmutableList<Long> getIdsFromAccountsList(ImmutableList<BalanceAccount> result) {
        return ImmutableList.copyOf(Lists.transform(result, new com.google.common.base.Function<BalanceAccount, Long>() {
            @Override
            public Long apply(BalanceAccount input) {
                return input.id.get();
            }
        }));
    }

}
