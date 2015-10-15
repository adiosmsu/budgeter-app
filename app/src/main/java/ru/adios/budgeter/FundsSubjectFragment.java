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
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import javax.annotation.Nullable;

import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import ru.adios.budgeter.api.FundsMutationSubject;
import ru.adios.budgeter.inmemrepo.Schema;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.DelayingAutoCompleteTextView;
import ru.adios.budgeter.util.FundsMutationSubjectContainer;
import ru.adios.budgeter.util.HintedArrayAdapter;
import ru.adios.budgeter.util.UiUtils;

import static com.google.common.base.Preconditions.checkState;


/**
 * Fragment for choosing and optionally adding a subject of a deal.
 */
public class FundsSubjectFragment extends Fragment {

    public static final String FIELD_SUBJECTS = "subject";
    public static final String FIELD_NEW_SUBJECT_NAME = "new_subject_name";
    public static final String FIELD_NEW_SUBJECT_PARENT_NAME = "new_subject_parent_name";
    public static final String FIELD_NEW_SUBJECT_TYPE = "new_subject_type";
    public static final String BUTTON_NEW_SUBJECT_SUBMIT = "new_subject_submit";

    public static InfoProvider.Builder getInfoProviderBuilder(@IdRes int fragmentId, Supplier<FundsMutationSubject> accountSupplier) {
        return new InfoProvider.Builder(fragmentId, accountSupplier);
    }

    public static final class InfoProvider implements CollectedFragmentsInfoProvider.InfoProvider<FundsMutationSubject> {

        public static final class Builder {

            private final SubjectAdditionElementCore subjectsElement = new SubjectAdditionElementCore(Schema.FUNDS_MUTATION_SUBJECTS);
            private final CoreErrorHighlighter subjectsErrorHighlighter = new CoreErrorHighlighter();

            private final CollectibleFragmentInfoProvider.Builder<FundsMutationSubject> mainBuilder;

            @Nullable
            private Consumer<FundsMutationSubject> callback;
            private CoreElementActivity.CoreElementFieldInfo subjectFieldInfo;

            public Builder(@IdRes int fragmentId, Supplier<FundsMutationSubject> accountSupplier) {
                mainBuilder = new CollectibleFragmentInfoProvider.Builder<>(fragmentId, new Feedbacker(subjectsElement, accountSupplier));
            }

            public Builder setNewAccountSubmitButtonCallback(@Nullable Consumer<FundsMutationSubject> callback) {
                this.callback = callback;
                return this;
            }

            public Builder provideSubjectFieldInfo(String coreFieldName, CoreErrorHighlighter highlighter, CoreNotifier.HintedLinker linker) {
                subjectFieldInfo = new CoreElementActivity.CoreElementFieldInfo(coreFieldName, linker, highlighter);
                return this;
            }

            public InfoProvider build() {
                checkState(subjectFieldInfo != null, "Subject field info not provided");

                return new InfoProvider(
                        mainBuilder
                                .addButtonInfo(BUTTON_NEW_SUBJECT_SUBMIT, new CoreElementActivity.CoreElementSubmitInfo<>(subjectsElement, new Consumer<FundsMutationSubject>() {
                                    @Override
                                    public void accept(FundsMutationSubject subject) {
                                        if (callback != null) {
                                            callback.accept(subject);
                                        }
                                    }
                                }, subjectsErrorHighlighter))
                                .addFieldInfo(FIELD_SUBJECTS, subjectFieldInfo)
                                .addFieldInfo(FIELD_NEW_SUBJECT_NAME, new CoreElementActivity.CoreElementFieldInfo(SubjectAdditionElementCore.FIELD_NAME, new CoreNotifier.TextLinker() {
                                    @Override
                                    public void link(String data) {
                                        subjectsElement.setName(data);
                                    }
                                }, subjectsErrorHighlighter))
                                .addFieldInfo(FIELD_NEW_SUBJECT_PARENT_NAME, new CoreElementActivity.CoreElementFieldInfo(SubjectAdditionElementCore.FIELD_PARENT_NAME, new CoreNotifier.TextLinker() {
                                    @Override
                                    public void link(String data) {
                                        subjectsElement.setParentName(data);
                                    }
                                }, subjectsErrorHighlighter))
                                .addFieldInfo(FIELD_NEW_SUBJECT_TYPE, new CoreElementActivity.CoreElementFieldInfo(SubjectAdditionElementCore.FIELD_TYPE, new CoreNotifier.NumberLinker() {
                                    @Override
                                    public void link(Number data) {
                                        subjectsElement.setType(data.intValue());
                                    }
                                }, subjectsErrorHighlighter))
                                .build(),
                        subjectsElement,
                        subjectsErrorHighlighter
                );
            }
        }

        private final CollectibleFragmentInfoProvider<FundsMutationSubject> delegate;

        public final SubjectAdditionElementCore subjectsElement;
        public final CoreErrorHighlighter subjectsErrorHighlighter;

        private InfoProvider(CollectibleFragmentInfoProvider<FundsMutationSubject> delegate, SubjectAdditionElementCore subjectsElement, CoreErrorHighlighter subjectsErrorHighlighter) {
            this.delegate = delegate;
            this.subjectsElement = subjectsElement;
            this.subjectsErrorHighlighter = subjectsErrorHighlighter;
        }

        @Override
        public int getFragmentId() {
            return delegate.getFragmentId();
        }

        @Override
        public CoreElementActivity.CoreElementSubmitInfo<FundsMutationSubject> getSubmitInfo(String buttonName) {
            return delegate.getSubmitInfo(buttonName);
        }

        @Override
        public CoreElementActivity.CoreElementFieldInfo getCoreElementFieldInfo(String fragmentFieldName) {
            return delegate.getCoreElementFieldInfo(fragmentFieldName);
        }

        @Override
        public void performFeedback(CoreElementActivity<FundsMutationSubject> activity) {
            delegate.performFeedback(activity);
        }

    }

    public static final class Feedbacker implements CollectibleFragmentInfoProvider.Feedbacker<FundsMutationSubject> {

        private final SubjectAdditionElementCore subjectsElement;
        private final Supplier<FundsMutationSubject> subjectSupplier;

        private Feedbacker(SubjectAdditionElementCore subjectsElement, Supplier<FundsMutationSubject> subjectSupplier) {
            this.subjectsElement = subjectsElement;
            this.subjectSupplier = subjectSupplier;
        }

        @Override
        public void performFeedback(CoreElementActivity<FundsMutationSubject> activity) {
            activity.textViewFeedback(subjectsElement.getName(), R.id.subjects_name_input);
            activity.textViewFeedback(subjectsElement.getParentName(), R.id.subjects_parent_name_input);
            activity.radioGroupFeedback(subjectsElement.getType(), R.id.subjects_type_radio);
            activity.hintedArraySpinnerFeedback(subjectSupplier.get(), R.id.subjects_spinner);
        }

    }


    public FundsSubjectFragment() {
        // Required empty public constructor
    }

    /**
     * Overridden to fail early.
     * @param activity activity of CoreElementActivity<FundsMutationSubject> type.
     */
    @Override
    public void onAttach(Activity activity) {
        checkState(activity instanceof CoreElementActivity, "Activity must extend CoreElementActivity: %s", activity);
        checkState(((CoreElementActivity) activity).provideClassForChecking() == FundsMutationSubject.class, "CoreElementActivity must be of the FundsMutationSubject type");
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View inflated = inflater.inflate(R.layout.fragment_funds_subject, container, false);

        @SuppressWarnings("unchecked")
        final CoreElementActivity<FundsMutationSubject> activity = (CoreElementActivity<FundsMutationSubject>) getActivity();
        final int id = getId();

        // main spinner init
        final Spinner subjectsSpinner = (Spinner) inflated.findViewById(R.id.subjects_spinner);
        HintedArrayAdapter.adaptArbitraryContainedSpinner(
                subjectsSpinner,
                activity,
                Schema.FUNDS_MUTATION_SUBJECTS.streamAll().map(new Function<FundsMutationSubject, HintedArrayAdapter.ObjectContainer<FundsMutationSubject>>() {
                    @Override
                    public HintedArrayAdapter.ObjectContainer<FundsMutationSubject> apply(FundsMutationSubject subject) {
                        return new FundsMutationSubjectContainer(subject);
                    }
                }).collect(Collectors.<HintedArrayAdapter.ObjectContainer<FundsMutationSubject>>toList())
        );
        activity.addFieldFragmentInfo(id, FIELD_SUBJECTS, subjectsSpinner, inflated.findViewById(R.id.subjects_spinner_info));

        // hidden parts
        final EditText nameInput = (EditText) inflated.findViewById(R.id.subjects_name_input);
        final TextView nameInputInfo = (TextView) inflated.findViewById(R.id.subjects_name_input_info);
        activity.addFieldFragmentInfo(id, FIELD_NEW_SUBJECT_NAME, nameInput, nameInputInfo);
        final FrameLayout parentNameLayout = (FrameLayout) inflated.findViewById(R.id.subjects_parent_name_input_layout);
        final DelayingAutoCompleteTextView parentNameInput = (DelayingAutoCompleteTextView) inflated.findViewById(R.id.subjects_parent_name_input);
        final TextView parentNameInputInfo = (TextView) inflated.findViewById(R.id.subjects_parent_name_input_info);
        activity.addFieldFragmentInfo(id, FIELD_NEW_SUBJECT_PARENT_NAME, parentNameInput, parentNameInputInfo);
        final RadioGroup typeRadio = (RadioGroup) inflated.findViewById(R.id.subjects_type_radio);
        final TextView typeRadioInfo = (TextView) inflated.findViewById(R.id.subjects_type_radio_info);
        activity.addFieldFragmentInfo(id, FIELD_NEW_SUBJECT_TYPE, typeRadio, typeRadioInfo);

        final Button submitButton = (Button) inflated.findViewById(R.id.subjects_submit_button);

        // button roundness and listener to show hidden interface
        final Button addButton = (Button) inflated.findViewById(R.id.subjects_add_button);
        UiUtils.makeButtonSquaredByHeight(addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getVisibility() == View.VISIBLE) {
                    nameInput.setVisibility(View.VISIBLE);
                    nameInputInfo.setVisibility(View.INVISIBLE);
                    parentNameLayout.setVisibility(View.VISIBLE);
                    parentNameInputInfo.setVisibility(View.INVISIBLE);
                    typeRadio.setVisibility(View.VISIBLE);
                    typeRadioInfo.setVisibility(View.INVISIBLE);
                    submitButton.setVisibility(View.VISIBLE);
                    v.setVisibility(View.INVISIBLE);
                    inflated.invalidate();
                }
            }
        });
        addButton.invalidate();

        // add submit button logic
        activity.addSubmitFragmentInfo(id, submitButton, BUTTON_NEW_SUBJECT_SUBMIT, new Consumer<FundsMutationSubject>() {
            @Override
            public void accept(FundsMutationSubject subject) {
                nameInput.setVisibility(View.GONE);
                nameInputInfo.setVisibility(View.GONE);
                parentNameLayout.setVisibility(View.GONE);
                parentNameInputInfo.setVisibility(View.GONE);
                typeRadio.setVisibility(View.GONE);
                typeRadioInfo.setVisibility(View.GONE);
                submitButton.setVisibility(View.GONE);
                addButton.setVisibility(View.VISIBLE);
                UiUtils.addToHintedSpinner(subject, subjectsSpinner, FundsMutationSubjectContainer.FACTORY);
                inflated.invalidate();
            }
        });

        return inflated;
    }

}
