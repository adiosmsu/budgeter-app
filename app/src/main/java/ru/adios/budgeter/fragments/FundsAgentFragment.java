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

package ru.adios.budgeter.fragments;


import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.UiThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import javax.annotation.Nullable;

import java8.util.Optional;
import java8.util.OptionalInt;
import java8.util.function.Consumer;
import java8.util.function.Supplier;
import ru.adios.budgeter.AgentAdditionElementCore;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.R;
import ru.adios.budgeter.adapters.Presenters;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.core.AbstractCollectibleFeedbacker;
import ru.adios.budgeter.core.CollectibleFragmentInfoProvider;
import ru.adios.budgeter.core.CoreElementActivity;
import ru.adios.budgeter.core.CoreErrorHighlighter;
import ru.adios.budgeter.core.CoreFragment;
import ru.adios.budgeter.core.CoreNotifier;
import ru.adios.budgeter.core.Feedbacking;
import ru.adios.budgeter.util.EmptyOnItemSelectedListener;
import ru.adios.budgeter.util.UiUtils;


/**
 * Fragment for choosing and optionally adding a agent entity of a deal.
 */
@UiThread
public class FundsAgentFragment extends CoreFragment {

    public static final String FIELD_AGENTS = "agent";
    public static final String FIELD_NEW_AGENT_NAME = "new_agent_name";
    public static final String FIELD_NEW_AGENT_DESC = "new_agent_desc";
    public static final String BUTTON_NEW_AGENT_SUBMIT = "new_agent_submit";

    public static final String KEY_HIGHLIGHTER = "agent_frag_highlighter";

    public static CollectibleFragmentInfoProvider<FundsMutationAgent, AgentAdditionElementCore> getInfoProvider(@IdRes int fragmentId,
                                                                                                                @Nullable Consumer<FundsMutationAgent> agentSubmitSuccessCallback,
                                                                                                                CoreElementActivity.CoreElementFieldInfo agentFieldInfo,
                                                                                                                Supplier<FundsMutationAgent> feedbackAgentSupplier) {
        final AgentAdditionElementCore agentCore = new AgentAdditionElementCore(BundleProvider.getBundle().fundsMutationAgents());
        final CoreErrorHighlighter agentsErrorHighlighter = new CoreErrorHighlighter(KEY_HIGHLIGHTER);

        return new CollectibleFragmentInfoProvider.Builder<FundsMutationAgent, AgentAdditionElementCore>(
                fragmentId,
                new Feedbacker(fragmentId, agentCore, feedbackAgentSupplier),
                agentsErrorHighlighter
        )
                .addButtonInfo(BUTTON_NEW_AGENT_SUBMIT, new CoreElementActivity.CoreElementSubmitInfo<>(agentCore, agentSubmitSuccessCallback, agentsErrorHighlighter))
                .addFieldInfo(FIELD_AGENTS, agentFieldInfo)
                .addFieldInfo(FIELD_NEW_AGENT_NAME, new CoreElementActivity.CoreElementFieldInfo(AgentAdditionElementCore.FIELD_NAME, new CoreNotifier.TextLinker() {
                    @Override
                    public boolean link(String data) {
                        final String prev = agentCore.getName();
                        if ((prev == null && data != null) || (prev != null && !prev.equals(data))) {
                            agentCore.setName(data);
                            return true;
                        }
                        return false;
                    }
                }, agentsErrorHighlighter))
                .addFieldInfo(FIELD_NEW_AGENT_DESC, new CoreElementActivity.CoreElementFieldInfo(AgentAdditionElementCore.FIELD_DESCRIPTION, new CoreNotifier.TextLinker() {
                    @Override
                    public boolean link(String data) {
                        final String prev = agentCore.getDescription();
                        if ((prev == null && data != null) || (prev != null && !prev.equals(data))) {
                            agentCore.setDescription(data);
                            return true;
                        }
                        return false;
                    }
                }, agentsErrorHighlighter))
                .build();
    }


    private boolean editOpen = false;
    private int selectedAgent = -1;

    public FundsAgentFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View inflated = inflater.inflate(R.layout.fragment_funds_agent, container, false);

        final CoreElementActivity activity = (CoreElementActivity) getActivity();
        final int id = getId();

        // main spinner init
        final Spinner agentsSpinner = (Spinner) inflated.findViewById(R.id.agents_spinner);
        UiUtils.prepareNullableSpinnerAsync(
                agentsSpinner,
                activity,
                id,
                FIELD_AGENTS,
                inflated,
                R.id.agents_spinner_info,
                BundleProvider.getBundle().fundsMutationAgents().streamAll(),
                Presenters.getAgentDefaultPresenter(),
                R.string.agents_spinner_null_val,
                selectedAgent >= 0 ? OptionalInt.of(selectedAgent) : OptionalInt.empty(),
                Optional.<AdapterView.OnItemSelectedListener>of(
                        new EmptyOnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                selectedAgent = parent.getAdapter().getCount() > position ? position : -1;
                            }
                        }
                )
        );

        // hidden parts
        final EditText nameInput = (EditText) inflated.findViewById(R.id.agents_name_input);
        final TextView nameInputInfo = (TextView) inflated.findViewById(R.id.agents_name_input_info);
        final EditText descInput = (EditText) inflated.findViewById(R.id.agents_desc_input);
        final TextView descInputInfo = (TextView) inflated.findViewById(R.id.agents_desc_input_info);
        final TextView descInputOpt = (TextView) inflated.findViewById(R.id.agents_desc_optional);
        activity.addFieldFragmentInfo(id, FIELD_NEW_AGENT_NAME, nameInput, nameInputInfo);
        activity.addFieldFragmentInfo(id, FIELD_NEW_AGENT_DESC, descInput, descInputInfo);

        final Button submitButton = (Button) inflated.findViewById(R.id.agents_submit_button);

        // button roundness and listener to show hidden interface
        final Button addButton = (Button) inflated.findViewById(R.id.agents_add_button);
        UiUtils.makeButtonSquaredByHeight(addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getVisibility() == View.VISIBLE) {
                    editOpen = true;
                    showEdit(v, nameInput, nameInputInfo, descInput, descInputInfo, descInputOpt, submitButton);
                    inflated.invalidate();
                }
            }
        });
        addButton.invalidate();

        // add submit button logic
        activity.addSubmitFragmentInfo(id, submitButton, BUTTON_NEW_AGENT_SUBMIT, new Consumer<FundsMutationAgent>() {
            @Override
            public void accept(FundsMutationAgent agent) {
                editOpen = false;
                hideEdit(nameInput, nameInputInfo, descInput, descInputInfo, descInputOpt, submitButton, addButton);
                UiUtils.addToMutableSpinner(agent, agentsSpinner);
                inflated.invalidate();
            }
        });

        if (editOpen) {
            showEdit(addButton, nameInput, nameInputInfo, descInput, descInputInfo, descInputOpt, submitButton);
        } else {
            hideEdit(nameInput, nameInputInfo, descInput, descInputInfo, descInputOpt, submitButton, addButton);
        }

        return inflated;
    }

    private void hideEdit(EditText nameInput, TextView nameInputInfo,
                          EditText descInput, TextView descInputInfo, TextView descInputOpt, Button submitButton, Button addButton) {
        nameInput.setVisibility(View.GONE);
        nameInputInfo.setVisibility(View.GONE);
        descInput.setVisibility(View.GONE);
        descInputInfo.setVisibility(View.GONE);
        descInputOpt.setVisibility(View.GONE);
        submitButton.setVisibility(View.GONE);
        addButton.setVisibility(View.VISIBLE);
    }

    private void showEdit(View v, EditText nameInput, TextView nameInputInfo, EditText descInput, TextView descInputInfo, TextView descInputOpt, Button submitButton) {
        nameInput.setVisibility(View.VISIBLE);
        nameInputInfo.setVisibility(View.INVISIBLE);
        descInput.setVisibility(View.VISIBLE);
        descInputInfo.setVisibility(View.INVISIBLE);
        descInputOpt.setVisibility(View.VISIBLE);
        submitButton.setVisibility(View.VISIBLE);
        v.setVisibility(View.INVISIBLE);
    }

    private static final class Feedbacker extends AbstractCollectibleFeedbacker {

        @IdRes
        private int fragmentId;
        private final AgentAdditionElementCore agentsElement;
        private final Supplier<FundsMutationAgent> agentSupplier;

        private TextView agentsNameInput;
        private TextView agentsDescInput;
        private Spinner agentsSpinner;

        private Feedbacker(@IdRes int fragmentId, AgentAdditionElementCore agentsElement, Supplier<FundsMutationAgent> agentSupplier) {
            this.fragmentId = fragmentId;
            this.agentsElement = agentsElement;
            this.agentSupplier = agentSupplier;
        }

        @Override
        protected void clearViewReferencesOptimal() {
            agentsNameInput = null;
            agentsDescInput = null;
            agentsSpinner = null;
        }

        @Override
        protected void performFeedbackSafe() {
            Feedbacking.textViewFeedback(agentsElement.getName(), agentsNameInput);
            Feedbacking.textViewFeedback(agentsElement.getDescription(), agentsDescInput);
            Feedbacking.nullableArraySpinnerFeedback(agentSupplier.get(), agentsSpinner);
        }

        @Override
        protected void collectEssentialViewsOptimal(CoreElementActivity activity) {
            final View fragmentLayout = activity.findViewById(fragmentId);
            agentsNameInput = (TextView) fragmentLayout.findViewById(R.id.agents_name_input);
            agentsDescInput = (TextView) fragmentLayout.findViewById(R.id.agents_desc_input);
            agentsSpinner = (Spinner) fragmentLayout.findViewById(R.id.agents_spinner);
        }

    }

}
