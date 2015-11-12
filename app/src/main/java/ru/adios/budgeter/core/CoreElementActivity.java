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

package ru.adios.budgeter.core;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.UiThread;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.Button;

import com.google.common.collect.ImmutableCollection;

import javax.annotation.Nullable;

import java8.util.function.Consumer;
import ru.adios.budgeter.FundsAwareMenuActivity;
import ru.adios.budgeter.Submitter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Michail Kulikov
 * 10/7/15
 */
@UiThread
public abstract class CoreElementActivity extends FundsAwareMenuActivity {

    private static final String KEY_INIT = "core_el_ac_init";

    private boolean feedbackCommencing = false;
    private boolean cleared = true;
    private boolean coresInitialized = false;

    public final boolean isFeedbackCommencing() {
        return feedbackCommencing;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        collectEssentialViews();
        cleared = false;

        final FragmentsInfoProvider infoProvider = getInfoProvider();
        infoProvider.collectEssentialViews(this);

        if (savedInstanceState != null) {
            infoProvider.onRestoreInstanceState(savedInstanceState);
            if (savedInstanceState.containsKey(KEY_INIT)) {
                coresInitialized = savedInstanceState.getBoolean(KEY_INIT);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!cleared) {
            clearViewReferences();
            cleared = true;
        }
        getInfoProvider().clearViewReferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cleared) {
            collectEssentialViews();
            cleared = false;
        }
        getInfoProvider().collectEssentialViews(this);
        if (!coresInitialized) {
            coreFeedback();
            coresInitialized = true;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_INIT, coresInitialized);
        getInfoProvider().onSaveInstanceState(outState);
    }

    protected abstract FragmentsInfoProvider getInfoProvider();

    public final void coreFeedback() {
        if (feedbackCommencing)
            return;

        feedbackCommencing = true;
        try {
            getInfoProvider().performFeedback();
            if (!cleared) {
                activityInnerFeedback();
            }
        } finally {
            feedbackCommencing = false;
        }
    }

    protected final void setGlobalInfoViewPerFragment(@IdRes int fragmentId, String buttonName, View infoView) {
        getInfoProvider().getSubmitInfo(fragmentId, buttonName).errorHighlighter.setGlobalInfoView(infoView);
    }

    protected void clearViewReferences() {}

    protected void collectEssentialViews() {}

    protected void activityInnerFeedback() {}

    public final CoreNotifier.Linker addFieldFragmentInfo(@IdRes int fragmentId, String fragmentFieldName, View fieldView, View fieldInfoView) {
        checkNotNull(fieldView, "fieldView is null");
        checkNotNull(fieldInfoView, "fieldInfoView is null");
        checkFragmentAllowance(fragmentId);

        final CoreElementFieldInfo fieldInfo = getInfoProvider().getCoreElementFieldInfo(fragmentId, fragmentFieldName);
        if (fieldInfo != null) {
            fieldInfo.errorHighlighter.addElementInfo(fieldInfo.coreFieldName, fieldInfoView);
            CoreNotifier.addLink(this, fieldView, fieldInfo.linker);
            return fieldInfo.linker;
        } else {
            throw new IllegalArgumentException(getClass().getSimpleName() + " isn't aware of fragmentFieldName: " + fragmentFieldName);
        }
    }

    public final <T> void addSubmitFragmentInfo(@IdRes final int fragmentId, Button submitButton, final String buttonName, @Nullable final Consumer<T> successRunnable) {
        checkNotNull(submitButton, "button is null");
        checkNotNull(buttonName, "buttonName is null");
        checkFragmentAllowance(fragmentId);

        @SuppressWarnings("unchecked")
        final CoreElementSubmitInfo<T, Submitter<T>> submitInfo = (CoreElementSubmitInfo<T, Submitter<T>>) getInfoProvider().getSubmitInfo(fragmentId, buttonName);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onClick(View v) {
                submitInfo.submitter.lock();
                new AsyncTask<Submitter<T>, Void, Submitter<T>>() {
                    @Override
                    protected Submitter<T> doInBackground(Submitter<T>[] params) {
                        final Submitter<T> submitter = params[0];
                        CoreUtils.doSubmitAndStore(submitter);
                        return submitter;
                    }

                    @Override
                    protected void onPostExecute(Submitter<T> submitter) {
                        final Submitter.Result<T> result = submitter.getStoredResult();

                        submitInfo.errorHighlighter.processSubmitResult(result);
                        if (result.isSuccessful()) {
                            if (submitInfo.successRunnable != null) {
                                submitInfo.successRunnable.accept(result.submitResult);
                            }

                            if (successRunnable != null) {
                                successRunnable.accept(result.submitResult);
                            }
                        }

                        submitter.unlock();
                    }
                }.execute(submitInfo.submitter);
            }
        });
    }

    private void checkFragmentAllowance(@IdRes int fragmentId) {
        if (!getInfoProvider().allowedFragments().contains(fragmentId)) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " doesn't work with " + getResources().getResourceName(fragmentId));
        }
    }

    protected final void finishSubmit(Submitter core, @IdRes int layoutId) {
        coreFeedback();
        findViewById(layoutId).invalidate();
        core.unlock();
    }

    public interface FragmentsInfoProvider extends Retainer {

        CoreElementSubmitInfo getSubmitInfo(@IdRes int fragmentId, String buttonName);

        ImmutableCollection<Integer> allowedFragments();

        CoreElementFieldInfo getCoreElementFieldInfo(@IdRes int fragmentId, String fragmentFieldName);

        void performFeedback();

        void collectEssentialViews(CoreElementActivity activity);

        void clearViewReferences();

    }

    public static final class CoreElementFieldInfo {

        protected final String coreFieldName;
        protected final CoreNotifier.Linker linker;
        protected final CoreErrorHighlighter errorHighlighter;

        public CoreElementFieldInfo(String coreFieldName, CoreNotifier.Linker linker, CoreErrorHighlighter errorHighlighter) {
            this.coreFieldName = coreFieldName;
            this.linker = linker;
            this.errorHighlighter = errorHighlighter;
        }

    }

    public static final class CoreElementSubmitInfo<T, Sub extends Submitter<T>> {

        public final Sub submitter;
        @Nullable
        public final Consumer<T> successRunnable;
        public final CoreErrorHighlighter errorHighlighter;

        public CoreElementSubmitInfo(Sub submitter, @Nullable Consumer<T> successRunnable, CoreErrorHighlighter errorHighlighter) {
            this.submitter = submitter;
            this.successRunnable = successRunnable;
            this.errorHighlighter = errorHighlighter;
        }

    }

    public interface Retainer {

        void onSaveInstanceState(Bundle outState);

        void onRestoreInstanceState(Bundle savedInstanceState);

    }

}
