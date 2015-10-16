package ru.adios.budgeter;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import javax.annotation.Nullable;

import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import ru.adios.budgeter.api.FundsMutationAgent;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.util.CachingHintedContainer;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.HintedArrayAdapter;
import ru.adios.budgeter.util.UiUtils;

import static com.google.common.base.Preconditions.checkState;


/**
 * Fragment for choosing and optionally adding a agent entity of a deal.
 */
public class FundsAgentFragment extends Fragment {

    public static final String FIELD_AGENTS = "agent";
    public static final String FIELD_NEW_AGENT_NAME = "new_agent_name";
    public static final String BUTTON_NEW_AGENT_SUBMIT = "new_agent_submit";

    public static CollectibleFragmentInfoProvider<FundsMutationAgent, AgentAdditionElementCore> getInfoProvider(@IdRes int fragmentId,
                                                                                                                @Nullable Consumer<FundsMutationAgent> agentSubmitSuccessCallback,
                                                                                                                CoreElementActivity.CoreElementFieldInfo agentFieldInfo,
                                                                                                                Supplier<FundsMutationAgent> feedbackAgentSupplier) {
        final AgentAdditionElementCore agentCore = new AgentAdditionElementCore(Schema.FUNDS_MUTATION_AGENTS);
        final CoreErrorHighlighter agentsErrorHighlighter = new CoreErrorHighlighter();

        return new CollectibleFragmentInfoProvider.Builder<FundsMutationAgent, AgentAdditionElementCore>(fragmentId, new Feedbacker(agentCore, feedbackAgentSupplier))
                .addButtonInfo(BUTTON_NEW_AGENT_SUBMIT, new CoreElementActivity.CoreElementSubmitInfo<>(agentCore, agentSubmitSuccessCallback, agentsErrorHighlighter))
                .addFieldInfo(FIELD_AGENTS, agentFieldInfo)
                .addFieldInfo(FIELD_NEW_AGENT_NAME, new CoreElementActivity.CoreElementFieldInfo(AgentAdditionElementCore.FIELD_NAME, new CoreNotifier.TextLinker() {
                    @Override
                    public void link(String data) {
                        agentCore.setName(data);
                    }
                }, agentsErrorHighlighter))
                .build();
    }

    public FundsAgentFragment() {
        // Required empty public constructor
    }

    /**
     * Overridden to fail early.
     * @param activity activity of CoreElementActivity type.
     */
    @Override
    public void onAttach(Activity activity) {
        checkState(activity instanceof CoreElementActivity, "Activity must extend CoreElementActivity: %s", activity);
        super.onAttach(activity);
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
        HintedArrayAdapter.adaptArbitraryContainedSpinner(
                agentsSpinner,
                activity,
                Schema.FUNDS_MUTATION_AGENTS.streamAll().map(new Function<FundsMutationAgent, HintedArrayAdapter.ObjectContainer<FundsMutationAgent>>() {
                    @Override
                    public HintedArrayAdapter.ObjectContainer<FundsMutationAgent> apply(FundsMutationAgent agent) {
                        return CONTAINER_FACTORY.create(agent);
                    }
                }).collect(Collectors.<HintedArrayAdapter.ObjectContainer<FundsMutationAgent>>toList())
        );
        activity.addFieldFragmentInfo(id, FIELD_AGENTS, agentsSpinner, inflated.findViewById(R.id.agents_spinner_info));

        // hidden parts
        final EditText nameInput = (EditText) inflated.findViewById(R.id.agents_name_input);
        final TextView nameInputInfo = (TextView) inflated.findViewById(R.id.agents_name_input_info);
        activity.addFieldFragmentInfo(id, FIELD_NEW_AGENT_NAME, nameInput, nameInputInfo);

        final Button submitButton = (Button) inflated.findViewById(R.id.agents_submit_button);

        // button roundness and listener to show hidden interface
        final Button addButton = (Button) inflated.findViewById(R.id.agents_add_button);
        UiUtils.makeButtonSquaredByHeight(addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getVisibility() == View.VISIBLE) {
                    nameInput.setVisibility(View.VISIBLE);
                    nameInputInfo.setVisibility(View.INVISIBLE);
                    submitButton.setVisibility(View.VISIBLE);
                    v.setVisibility(View.INVISIBLE);
                    inflated.invalidate();
                }
            }
        });
        addButton.invalidate();

        // add submit button logic
        activity.addSubmitFragmentInfo(id, submitButton, BUTTON_NEW_AGENT_SUBMIT, new Consumer<FundsMutationAgent>() {
            @Override
            public void accept(FundsMutationAgent agent) {
                nameInput.setVisibility(View.GONE);
                nameInputInfo.setVisibility(View.GONE);
                submitButton.setVisibility(View.GONE);
                addButton.setVisibility(View.VISIBLE);
                UiUtils.addToHintedSpinner(agent, agentsSpinner, CONTAINER_FACTORY);
                inflated.invalidate();
            }
        });

        return inflated;
    }

    private static final FundsMutationAgentContainerFactory CONTAINER_FACTORY = new FundsMutationAgentContainerFactory();
    public static final class FundsMutationAgentContainerFactory implements HintedArrayAdapter.ContainerFactory<FundsMutationAgent> {
        private FundsMutationAgentContainerFactory() {}

        @Override
        public HintedArrayAdapter.ObjectContainer<FundsMutationAgent> create(FundsMutationAgent fundsMutationAgent) {
            return new CachingHintedContainer<FundsMutationAgent>(fundsMutationAgent) {
                @Override
                protected String calculateToString() {
                    return getObject().name;
                }
            };
        }

    }

    private static final class Feedbacker implements CollectibleFragmentInfoProvider.Feedbacker {

        private final AgentAdditionElementCore agentsElement;
        private final Supplier<FundsMutationAgent> agentSupplier;

        private Feedbacker(AgentAdditionElementCore agentsElement, Supplier<FundsMutationAgent> agentSupplier) {
            this.agentsElement = agentsElement;
            this.agentSupplier = agentSupplier;
        }

        @Override
        public void performFeedback(CoreElementActivity activity) {
            activity.textViewFeedback(agentsElement.getName(), R.id.agents_name_input);
            activity.hintedArraySpinnerFeedback(agentSupplier.get(), R.id.agents_spinner);
        }

    }

}
