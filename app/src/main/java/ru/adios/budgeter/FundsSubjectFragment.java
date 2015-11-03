package ru.adios.budgeter;


import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

import javax.annotation.Nullable;

import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Supplier;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.DelayingAutoCompleteTextView;
import ru.adios.budgeter.util.FundsMutationSubjectContainer;
import ru.adios.budgeter.util.HintedArrayAdapter;
import ru.adios.budgeter.util.ModedRequestingAutoCompleteAdapter;
import ru.adios.budgeter.util.RequestingAutoCompleteAdapter;
import ru.adios.budgeter.util.UiUtils;


/**
 * Fragment for choosing and optionally adding a subject of a deal.
 */
public class FundsSubjectFragment extends CoreFragment {

    public static final String FIELD_SUBJECTS = "subject";
    public static final String FIELD_NEW_SUBJECT_NAME = "new_subject_name";
    public static final String FIELD_NEW_SUBJECT_PARENT_NAME = "new_subject_parent_name";
    public static final String FIELD_NEW_SUBJECT_TYPE = "new_subject_type";
    public static final String BUTTON_NEW_SUBJECT_SUBMIT = "new_subject_submit";

    private static final String NO_SUCH_PARENT_ERROR = "No such parent";

    public static CollectedFragmentsInfoProvider.InfoProvider<FundsMutationSubject, SubjectAdditionElementCore> getInfoProvider(@IdRes final int fragmentId,
                                                                                                                                final Activity activity,
                                                                                                                                @Nullable Consumer<FundsMutationSubject> subjectSubmitSuccessCallback,
                                                                                                                                CoreElementActivity.CoreElementFieldInfo subjectFieldInfo,
                                                                                                                                Supplier<FundsMutationSubject> feedbackAccountSupplier) {
        final SubjectAdditionElementCore subjectsElement = new SubjectAdditionElementCore(BundleProvider.getBundle().fundsMutationSubjects());
        final CoreErrorHighlighter subjectsErrorHighlighter = new CoreErrorHighlighter();

        return new CollectibleFragmentInfoProvider.Builder<FundsMutationSubject, SubjectAdditionElementCore>(fragmentId, new Feedbacker(subjectsElement, feedbackAccountSupplier))
                .addButtonInfo(BUTTON_NEW_SUBJECT_SUBMIT, new CoreElementActivity.CoreElementSubmitInfo<>(subjectsElement, subjectSubmitSuccessCallback, subjectsErrorHighlighter))
                .addFieldInfo(FIELD_SUBJECTS, subjectFieldInfo)
                .addFieldInfo(FIELD_NEW_SUBJECT_NAME, new CoreElementActivity.CoreElementFieldInfo(SubjectAdditionElementCore.FIELD_NAME, new CoreNotifier.TextLinker() {
                    @Override
                    public void link(String data) {
                        subjectsElement.setName(data);
                    }
                }, subjectsErrorHighlighter))
                .addFieldInfo(FIELD_NEW_SUBJECT_PARENT_NAME, new CoreElementActivity.CoreElementFieldInfo(SubjectAdditionElementCore.FIELD_PARENT_NAME, new CoreNotifier.TextLinker() {
                    @Override
                    public void link(final String data) {
                        new AsyncTask<SubjectAdditionElementCore, Void, Boolean>() {
                            @Override
                            protected Boolean doInBackground(SubjectAdditionElementCore[] params) {
                                final SubjectAdditionElementCore e = params[0];
                                return e.setParentName(data);
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                final TextView infoView = (TextView) activity.findViewById(fragmentId).findViewById(R.id.subjects_parent_name_input_info);
                                if (infoView == null) {
                                    return;
                                }

                                if (result) {
                                    if (infoView.getVisibility() == View.VISIBLE && NO_SUCH_PARENT_ERROR.equals(infoView.getText())) {
                                        infoView.setText("");
                                        infoView.setVisibility(View.INVISIBLE);
                                        infoView.invalidate();
                                    }
                                } else {
                                    infoView.setVisibility(View.VISIBLE);
                                    infoView.setText(NO_SUCH_PARENT_ERROR);
                                    infoView.invalidate();
                                }
                            }
                        }.execute(subjectsElement);
                    }
                }, subjectsErrorHighlighter))
                .addFieldInfo(FIELD_NEW_SUBJECT_TYPE, new CoreElementActivity.CoreElementFieldInfo(SubjectAdditionElementCore.FIELD_TYPE, new CoreNotifier.NumberLinker() {
                    @Override
                    public void link(Number data) {
                        subjectsElement.setType(data.intValue());
                    }
                }, subjectsErrorHighlighter))
                .build();
    }


    public FundsSubjectFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View inflated = inflater.inflate(R.layout.fragment_funds_subject, container, false);

        final CoreElementActivity activity = (CoreElementActivity) getActivity();
        final int id = getId();

        // main spinner init
        final Spinner subjectsSpinner = (Spinner) inflated.findViewById(R.id.subjects_spinner);
        UiUtils.prepareHintedSpinnerAsync(subjectsSpinner, activity, id, FIELD_SUBJECTS, inflated, R.id.subjects_spinner_info, BundleProvider.getBundle().fundsMutationSubjects().streamAll(),
                new Function<FundsMutationSubject, HintedArrayAdapter.ObjectContainer<FundsMutationSubject>>() {
                    @Override
                    public HintedArrayAdapter.ObjectContainer<FundsMutationSubject> apply(FundsMutationSubject subject) {
                        return new FundsMutationSubjectContainer(subject);
                    }
                }
        );

        // hidden parts
        final EditText nameInput = (EditText) inflated.findViewById(R.id.subjects_name_input);
        final TextView nameInputInfo = (TextView) inflated.findViewById(R.id.subjects_name_input_info);
        activity.addFieldFragmentInfo(id, FIELD_NEW_SUBJECT_NAME, nameInput, nameInputInfo);
        final FrameLayout parentNameLayout = (FrameLayout) inflated.findViewById(R.id.subjects_parent_name_input_layout);
        final DelayingAutoCompleteTextView parentNameInput = (DelayingAutoCompleteTextView) inflated.findViewById(R.id.subjects_parent_name_input);
        parentNameInput.setThreshold(1);
        parentNameInput.setAdapter(new ModedRequestingAutoCompleteAdapter<>(
                activity,
                new ModedRequestingAutoCompleteAdapter.Requester<FundsMutationSubject>() {
                    @Override
                    public List<FundsMutationSubject> doActualRequest(String constraint) {
                        return BundleProvider.getBundle().fundsMutationSubjects().nameLikeSearch(constraint);
                    }
                },
                new RequestingAutoCompleteAdapter.StringPresenter<FundsMutationSubject>() {
                    @Override
                    public String getStringPresentation(FundsMutationSubject item) {
                        return item.name;
                    }
                },
                ModedRequestingAutoCompleteAdapter.SQL_ILIKE_DECORATOR
        ));
        parentNameInput.setLoadingIndicator((ProgressBar) inflated.findViewById(R.id.subjects_parent_name_input_progress));
        parentNameInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                final FundsMutationSubject subject = (FundsMutationSubject) adapterView.getItemAtPosition(position);
                parentNameInput.setText(subject.name);
            }
        });
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

    private static final class Feedbacker implements CollectibleFragmentInfoProvider.Feedbacker {

        private final SubjectAdditionElementCore subjectsElement;
        private final Supplier<FundsMutationSubject> subjectSupplier;

        private Feedbacker(SubjectAdditionElementCore subjectsElement, Supplier<FundsMutationSubject> subjectSupplier) {
            this.subjectsElement = subjectsElement;
            this.subjectSupplier = subjectSupplier;
        }

        @Override
        public void performFeedback(CoreElementActivity activity) {
            activity.textViewFeedback(subjectsElement.getName(), R.id.subjects_name_input);
            activity.textViewFeedback(subjectsElement.getParentName(), R.id.subjects_parent_name_input);
            activity.radioGroupFeedback(subjectsElement.getType(), R.id.subjects_type_radio);
            activity.hintedArraySpinnerFeedback(subjectSupplier.get(), R.id.subjects_spinner);
        }

    }

}
