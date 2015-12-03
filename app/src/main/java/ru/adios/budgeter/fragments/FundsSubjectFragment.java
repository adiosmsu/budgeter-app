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


import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
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
import android.widget.Toast;

import com.google.common.collect.ImmutableList;

import java.util.List;

import java8.util.Optional;
import java8.util.OptionalInt;
import java8.util.function.Consumer;
import java8.util.function.Supplier;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.R;
import ru.adios.budgeter.SubjectAdditionElementCore;
import ru.adios.budgeter.adapters.AsyncParentingDataExtractor;
import ru.adios.budgeter.adapters.AsyncRefresher;
import ru.adios.budgeter.adapters.IdentifiedData;
import ru.adios.budgeter.adapters.ModedRequestingAutoCompleteAdapter;
import ru.adios.budgeter.adapters.NullableDecoratingAdapter;
import ru.adios.budgeter.adapters.Presenters;
import ru.adios.budgeter.adapters.RefreshingLeveledAdapter;
import ru.adios.budgeter.adapters.StringPresenter;
import ru.adios.budgeter.adapters.UnchangingStringPresenter;
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
import ru.adios.budgeter.util.Immutables;
import ru.adios.budgeter.util.UiUtils;
import ru.adios.budgeter.widgets.DelayingAutoCompleteTextView;
import ru.adios.budgeter.widgets.RefreshingSpinner;


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
                        final String prev = subjectsElement.getParentName();
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
                                } else if (prev != null && infoView.getVisibility() != View.VISIBLE) {
                                    infoView.setVisibility(View.VISIBLE);
                                    infoView.setText(NO_SUCH_PARENT_ERROR);
                                    infoView.invalidate();
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
        final RefreshingSpinner subjectsSpinner = (RefreshingSpinner) inflated.findViewById(R.id.subjects_spinner);
        final RefreshingLeveledAdapter<FundsMutationSubject, Long> leveledAdapter = new RefreshingLeveledAdapter<>(
                getContext(),
                new AsyncRefresher<FundsMutationSubject, FundsMutationSubject>() {
                    @Override
                    public ImmutableList<FundsMutationSubject> gatherData(@Nullable FundsMutationSubject param) {
                        long parentId;
                        if (param == null) {
                            parentId = 0;
                        } else {
                            if (param.id.isPresent() && param.childFlag) {
                                parentId = param.id.getAsLong();
                            } else {
                                return ImmutableList.of();
                            }
                        }

                        return BundleProvider.getBundle()
                                .fundsMutationSubjects()
                                .findByParent(parentId)
                                .collect(Immutables.<FundsMutationSubject>getListCollector());
                    }
                },
                new AsyncParentingDataExtractor<FundsMutationSubject, Long>() {
                    @Override
                    public FundsMutationSubject extractData(Long id) {
                        return BundleProvider.getBundle()
                                .fundsMutationSubjects()
                                .getById(id)
                                .orElse(null);
                    }

                    @Override
                    public Long extractId(FundsMutationSubject data) {
                        return data.id.orElse(0);
                    }

                    @Nullable
                    @Override
                    public IdentifiedData<FundsMutationSubject, Long> extractParent(Long id) {
                        //TODO: add method to API instead of this 2 queries
                        final Optional<FundsMutationSubject> byId = BundleProvider.getBundle().fundsMutationSubjects().getById(id);
                        if (byId.isPresent()) {
                            final long pId = byId.get().parentId;
                            if (pId > 0) {
                                final Optional<FundsMutationSubject> parentOpt = BundleProvider.getBundle().fundsMutationSubjects().getById(pId);
                                if (parentOpt.isPresent()) {
                                    return new IdentifiedData<>(parentOpt.get(), pId);
                                }
                            }
                        }

                        return null;
                    }
                },
                android.R.layout.simple_spinner_item
        );
        if (selectedSubject < 0) {
            leveledAdapter.initDoNotCallIfActivityRestored();
        }
        leveledAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final NullableDecoratingAdapter<RefreshingLeveledAdapter<FundsMutationSubject, Long>, FundsMutationSubject> wrapperAdapter = new NullableDecoratingAdapter<>(
                getContext(),
                leveledAdapter,
                subjectsSpinner.getPrompt().toString(),
                android.R.layout.simple_spinner_item
        );
        NullableDecoratingAdapter.adaptSpinner(
                subjectsSpinner,
                wrapperAdapter,
                Optional.<StringPresenter<FundsMutationSubject>>of(Presenters.getSubjectParentLoadingPresenter()),
                OptionalInt.of(R.string.subjects_spinner_null_val)
        );
        subjectsSpinner.setRefreshFailedToastResource(R.string.subjects_spinner_refresh_failed);
        subjectsSpinner.setRestoredSelection(selectedSubject); // -1 is safe to set
        subjectsSpinner.setSelectionListener(new EmptyOnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSubject = parent.getAdapter().getCount() > position ? position : -1;
            }
        });
        final CoreNotifier.Linker linker =
                activity.addFieldFragmentInfo(id, FIELD_SUBJECTS, subjectsSpinner, inflated.findViewById(R.id.subjects_spinner_info));
        if (selectedSubject >= 0) {
            CoreNotifier.linkViewValueWithCore(subjectsSpinner.getSelectedItem(), linker, activity);
        }

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
        final ModedRequestingAutoCompleteAdapter<FundsMutationSubject> autoCompleteAdapter = new ModedRequestingAutoCompleteAdapter<>(
                activity,
                new ModedRequestingAutoCompleteAdapter.Requester<FundsMutationSubject>() {
                    @Override
                    public List<FundsMutationSubject> doActualRequest(String constraint) {
                        return BundleProvider.getBundle().fundsMutationSubjects().nameLikeSearch(constraint);
                    }
                },
                ModedRequestingAutoCompleteAdapter.SQL_ILIKE_DECORATOR
        );
        autoCompleteAdapter.setStringPresenter(
                new UnchangingStringPresenter<FundsMutationSubject>() {
                    @Override
                    public String getStringPresentation(FundsMutationSubject item) {
                        return item.name;
                    }
                }
        );
        parentNameInput.setAdapter(autoCompleteAdapter);
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
                if (!subjectsSpinner.refreshCurrentWithPossibleValue(subject)) {
                    Toast.makeText(activity, R.string.subjects_spinner_refresh_failed, Toast.LENGTH_LONG)
                            .show();
                }
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
            Feedbacking.nullableArraySpinnerFeedback(subjectSupplier.get(), subjectsSpinner);
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
