package ru.adios.budgeter.fragments;


import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.UiThread;
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

import java8.util.Optional;
import java8.util.OptionalInt;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Supplier;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.R;
import ru.adios.budgeter.SubjectAdditionElementCore;
import ru.adios.budgeter.adapters.FundsMutationSubjectContainer;
import ru.adios.budgeter.adapters.HintedArrayAdapter;
import ru.adios.budgeter.adapters.ModedRequestingAutoCompleteAdapter;
import ru.adios.budgeter.adapters.RequestingAutoCompleteAdapter;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.core.AbstractCollectibleFeedbacker;
import ru.adios.budgeter.core.CollectedFragmentsInfoProvider;
import ru.adios.budgeter.core.CollectibleFragmentInfoProvider;
import ru.adios.budgeter.core.CoreElementActivity;
import ru.adios.budgeter.core.CoreErrorHighlighter;
import ru.adios.budgeter.core.CoreFragment;
import ru.adios.budgeter.core.CoreNotifier;
import ru.adios.budgeter.core.Feedbacking;
import ru.adios.budgeter.util.EmptyOnItemSelectedListener;
import ru.adios.budgeter.util.UiUtils;
import ru.adios.budgeter.widgets.DelayingAutoCompleteTextView;


/**
 * Fragment for choosing and optionally adding a subject of a deal.
 */
@UiThread
public class FundsSubjectFragment extends CoreFragment {

    public static final String FIELD_SUBJECTS = "subject";
    public static final String FIELD_NEW_SUBJECT_NAME = "new_subject_name";
    public static final String FIELD_NEW_SUBJECT_DESC = "new_subject_desc";
    public static final String FIELD_NEW_SUBJECT_PARENT_NAME = "new_subject_parent_name";
    public static final String FIELD_NEW_SUBJECT_TYPE = "new_subject_type";
    public static final String BUTTON_NEW_SUBJECT_SUBMIT = "new_subject_submit";

    private static final String NO_SUCH_PARENT_ERROR = "No such parent";

    public static final String KEY_HIGHLIGHTER = "subj_frag_highlighter";

    public static CollectedFragmentsInfoProvider.InfoProvider<FundsMutationSubject, SubjectAdditionElementCore> getInfoProvider(
            @IdRes final int fragmentId,
            final Activity activity,
            @Nullable Consumer<FundsMutationSubject> subjectSubmitSuccessCallback,
            CoreElementActivity.CoreElementFieldInfo subjectFieldInfo,
            Supplier<FundsMutationSubject> feedbackAccountSupplier
    ) {
        final SubjectAdditionElementCore subjectsElement = new SubjectAdditionElementCore(BundleProvider.getBundle().fundsMutationSubjects());
        final CoreErrorHighlighter subjectsErrorHighlighter = new CoreErrorHighlighter(KEY_HIGHLIGHTER);

        return new CollectibleFragmentInfoProvider.Builder<FundsMutationSubject, SubjectAdditionElementCore>(
                fragmentId,
                new Feedbacker(fragmentId, subjectsElement, feedbackAccountSupplier),
                subjectsErrorHighlighter
        )
                .addButtonInfo(BUTTON_NEW_SUBJECT_SUBMIT, new CoreElementActivity.CoreElementSubmitInfo<>(subjectsElement, subjectSubmitSuccessCallback, subjectsErrorHighlighter))
                .addFieldInfo(FIELD_SUBJECTS, subjectFieldInfo)
                .addFieldInfo(FIELD_NEW_SUBJECT_NAME, new CoreElementActivity.CoreElementFieldInfo(SubjectAdditionElementCore.FIELD_NAME, new CoreNotifier.TextLinker() {
                    @Override
                    public boolean link(String data) {
                        final String prev = subjectsElement.getName();
                        if ((prev == null && data != null) || (prev != null && !prev.equals(data))) {
                            subjectsElement.setName(data);
                            return true;
                        }
                        return false;
                    }
                }, subjectsErrorHighlighter))
                .addFieldInfo(FIELD_NEW_SUBJECT_DESC, new CoreElementActivity.CoreElementFieldInfo(SubjectAdditionElementCore.FIELD_DESCRIPTION, new CoreNotifier.TextLinker() {
                    @Override
                    public boolean link(String data) {
                        final String prev = subjectsElement.getDescription();
                        if ((prev == null && data != null) || (prev != null && !prev.equals(data))) {
                            subjectsElement.setDescription(data);
                            return true;
                        }
                        return false;
                    }
                }, subjectsErrorHighlighter))
                .addFieldInfo(FIELD_NEW_SUBJECT_PARENT_NAME, new CoreElementActivity.CoreElementFieldInfo(SubjectAdditionElementCore.FIELD_PARENT_NAME, new CoreNotifier.TextLinker() {
                    @Override
                    public boolean link(final String data) {
                        new AsyncTask<SubjectAdditionElementCore, Void, Optional<FundsMutationSubject>>() {
                            @Override
                            protected Optional<FundsMutationSubject> doInBackground(SubjectAdditionElementCore[] params) {
                                final SubjectAdditionElementCore e = params[0];
                                return e.setParentName(data, true);
                            }

                            @Override
                            protected void onPostExecute(Optional<FundsMutationSubject> result) {
                                final TextView infoView = (TextView) activity.findViewById(fragmentId).findViewById(R.id.subjects_parent_name_input_info);
                                if (infoView == null) {
                                    return;
                                }

                                if (result.isPresent()) {
                                    subjectsElement.setParentId(result.get().id.getAsLong());
                                    if (infoView.getVisibility() == View.VISIBLE && NO_SUCH_PARENT_ERROR.equals(infoView.getText())) {
                                        infoView.setText("");
                                        infoView.setVisibility(View.INVISIBLE);
                                        infoView.invalidate();
                                    }
                                } else {
                                    if (infoView.getVisibility() != View.VISIBLE) {
                                        infoView.setVisibility(View.VISIBLE);
                                        infoView.setText(NO_SUCH_PARENT_ERROR);
                                        infoView.invalidate();
                                    }
                                }
                            }
                        }.execute(subjectsElement);
                        return false;
                    }
                }, subjectsErrorHighlighter))
                .addFieldInfo(FIELD_NEW_SUBJECT_TYPE, new CoreElementActivity.CoreElementFieldInfo(SubjectAdditionElementCore.FIELD_TYPE, new CoreNotifier.NumberLinker() {
                    @Override
                    public boolean link(Number data) {
                        final int i = data.intValue();
                        final FundsMutationSubject.Type prev = subjectsElement.getType();
                        if ((prev == null && i >= 0) || (prev != null && prev.ordinal() != i)) {
                            subjectsElement.setType(i);
                            return true;
                        }
                        return false;
                    }
                }, subjectsErrorHighlighter))
                .build();
    }


    private boolean editOpen = false;
    private int selectedSubject = -1;

    public FundsSubjectFragment() {
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
        final View inflated = inflater.inflate(R.layout.fragment_funds_subject, container, false);

        final CoreElementActivity activity = (CoreElementActivity) getActivity();
        final int id = getId();

        // main spinner init
        final Spinner subjectsSpinner = (Spinner) inflated.findViewById(R.id.subjects_spinner);
        UiUtils.prepareHintedSpinnerAsync(
                subjectsSpinner,
                activity,
                id,
                FIELD_SUBJECTS,
                inflated,
                R.id.subjects_spinner_info,
                BundleProvider.getBundle().fundsMutationSubjects().streamAll(),
                new Function<FundsMutationSubject, HintedArrayAdapter.ObjectContainer<FundsMutationSubject>>() {
                    @Override
                    public HintedArrayAdapter.ObjectContainer<FundsMutationSubject> apply(FundsMutationSubject subject) {
                        return new FundsMutationSubjectContainer(subject);
                    }
                },
                selectedSubject >= 0 ? OptionalInt.of(selectedSubject) : OptionalInt.empty(),
                Optional.<AdapterView.OnItemSelectedListener>of(
                        new EmptyOnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                if (parent.getAdapter().getCount() > position) {
                                    selectedSubject = position;
                                }
                            }
                        }
                )
        );

        // hidden parts
        final EditText nameInput = (EditText) inflated.findViewById(R.id.subjects_name_input);
        final TextView nameInputInfo = (TextView) inflated.findViewById(R.id.subjects_name_input_info);
        final EditText descInput = (EditText) inflated.findViewById(R.id.subjects_desc_input);
        final TextView descInputInfo = (TextView) inflated.findViewById(R.id.subjects_desc_input_info);
        final TextView descInputOpt = (TextView) inflated.findViewById(R.id.subjects_desc_optional);
        activity.addFieldFragmentInfo(id, FIELD_NEW_SUBJECT_NAME, nameInput, nameInputInfo);
        activity.addFieldFragmentInfo(id, FIELD_NEW_SUBJECT_DESC, descInput, descInputInfo);
        final FrameLayout parentNameLayout = (FrameLayout) inflated.findViewById(R.id.subjects_parent_name_input_layout);
        final DelayingAutoCompleteTextView parentNameInput = (DelayingAutoCompleteTextView) inflated.findViewById(R.id.subjects_parent_name_input);
        parentNameInput.setThreshold(2);
        parentNameInput.setAutoCompleteDelayMillis(2000);
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
                    editOpen = true;
                    showEdit(v, nameInput, nameInputInfo, descInput,
                            descInputInfo, descInputOpt, parentNameLayout, parentNameInputInfo, typeRadio, typeRadioInfo, submitButton);
                    inflated.invalidate();
                }
            }
        });
        addButton.invalidate();

        // add submit button logic
        activity.addSubmitFragmentInfo(id, submitButton, BUTTON_NEW_SUBJECT_SUBMIT, new Consumer<FundsMutationSubject>() {
            @Override
            public void accept(FundsMutationSubject subject) {
                editOpen = false;
                hideEdit(nameInput, nameInputInfo, descInput,
                        descInputInfo, descInputOpt, parentNameLayout, parentNameInputInfo, typeRadio, typeRadioInfo, submitButton, addButton);
                UiUtils.addToHintedSpinner(subject, subjectsSpinner, FundsMutationSubjectContainer.FACTORY);
                inflated.invalidate();
            }
        });

        if (editOpen) {
            showEdit(addButton, nameInput, nameInputInfo, descInput,
                    descInputInfo, descInputOpt, parentNameLayout, parentNameInputInfo, typeRadio, typeRadioInfo, submitButton);
        } else {
            hideEdit(nameInput, nameInputInfo, descInput,
                    descInputInfo, descInputOpt, parentNameLayout, parentNameInputInfo, typeRadio, typeRadioInfo, submitButton, addButton);
        }

        return inflated;
    }

    private void hideEdit(EditText nameInput, TextView nameInputInfo, EditText descInput, TextView descInputInfo,
                          TextView descInputOpt, FrameLayout parentNameLayout, TextView parentNameInputInfo, RadioGroup typeRadio,
                          TextView typeRadioInfo, Button submitButton, Button addButton) {
        nameInput.setVisibility(View.GONE);
        nameInputInfo.setVisibility(View.GONE);
        descInput.setVisibility(View.GONE);
        descInputInfo.setVisibility(View.GONE);
        descInputOpt.setVisibility(View.GONE);
        parentNameLayout.setVisibility(View.GONE);
        parentNameInputInfo.setVisibility(View.GONE);
        typeRadio.setVisibility(View.GONE);
        typeRadioInfo.setVisibility(View.GONE);
        submitButton.setVisibility(View.GONE);
        addButton.setVisibility(View.VISIBLE);
    }

    private void showEdit(View v, EditText nameInput, TextView nameInputInfo, EditText descInput, TextView descInputInfo,
                          TextView descInputOpt, FrameLayout parentNameLayout, TextView parentNameInputInfo, RadioGroup typeRadio,
                          TextView typeRadioInfo, Button submitButton) {
        nameInput.setVisibility(View.VISIBLE);
        nameInputInfo.setVisibility(View.INVISIBLE);
        descInput.setVisibility(View.VISIBLE);
        descInputInfo.setVisibility(View.INVISIBLE);
        descInputOpt.setVisibility(View.VISIBLE);
        parentNameLayout.setVisibility(View.VISIBLE);
        parentNameInputInfo.setVisibility(View.INVISIBLE);
        typeRadio.setVisibility(View.VISIBLE);
        typeRadioInfo.setVisibility(View.INVISIBLE);
        submitButton.setVisibility(View.VISIBLE);
        v.setVisibility(View.INVISIBLE);
    }

    private static final class Feedbacker extends AbstractCollectibleFeedbacker {

        @IdRes
        private final int fragmentId;
        private final SubjectAdditionElementCore subjectsElement;
        private final Supplier<FundsMutationSubject> subjectSupplier;
        private TextView subjectsNameInput;
        private TextView subjectsDescInput;
        private TextView subjectsParentNameInput;
        private RadioGroup subjectsTypeRadio;
        private Spinner subjectsSpinner;

        private Feedbacker(@IdRes int fragmentId, SubjectAdditionElementCore subjectsElement, Supplier<FundsMutationSubject> subjectSupplier) {
            this.fragmentId = fragmentId;
            this.subjectsElement = subjectsElement;
            this.subjectSupplier = subjectSupplier;
        }

        @Override
        protected void clearViewReferencesOptimal() {
            subjectsNameInput = null;
            subjectsDescInput = null;
            subjectsParentNameInput = null;
            subjectsTypeRadio = null;
            subjectsSpinner = null;
        }

        @Override
        protected void performFeedbackSafe() {
            Feedbacking.textViewFeedback(subjectsElement.getName(), subjectsNameInput);
            Feedbacking.textViewFeedback(subjectsElement.getDescription(), subjectsDescInput);
            Feedbacking.textViewFeedback(subjectsElement.getParentName(), subjectsParentNameInput);
            Feedbacking.radioGroupFeedback(subjectsElement.getType(), subjectsTypeRadio);
            Feedbacking.hintedArraySpinnerFeedback(subjectSupplier.get(), subjectsSpinner);
        }

        @Override
        protected void collectEssentialViewsOptimal(CoreElementActivity activity) {
            final View fragmentLayout = activity.findViewById(fragmentId);
            subjectsNameInput = (TextView) fragmentLayout.findViewById(R.id.subjects_name_input);
            subjectsDescInput = (TextView) fragmentLayout.findViewById(R.id.subjects_desc_input);
            subjectsParentNameInput = (TextView) fragmentLayout.findViewById(R.id.subjects_parent_name_input);
            subjectsTypeRadio = (RadioGroup) fragmentLayout.findViewById(R.id.subjects_type_radio);
            subjectsSpinner = (Spinner) fragmentLayout.findViewById(R.id.subjects_spinner);
        }

    }

}
