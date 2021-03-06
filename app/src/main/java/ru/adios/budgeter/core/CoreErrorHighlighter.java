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

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import ru.adios.budgeter.R;
import ru.adios.budgeter.Submitter;

/**
 * Designed to update submit errors into activities.
 *
 * Created by adios on 01.10.15.
 */
@NotThreadSafe
public final class CoreErrorHighlighter implements CoreElementActivity.Retainer {

    private static final String GLOBAL_INFO_VIEW_NAME = "$reserved:globalInfoView";

    public interface ViewWorker {

        void successWork(View view);

        void failureWork(View view);

    }

    private final String id;

    private final HashMap<String, View> elementNameToView = new HashMap<>();
    private final HashMap<Integer, ViewWorker> viewWorkers = new HashMap<>();
    private final HashSet<String> idleViews = new HashSet<>();
    private View globalInfoView;

    public CoreErrorHighlighter(String id) {
        this.id = id;
    }

    public void addElementInfo(@Nonnull String name, @Nonnull View view) {
        if (name.equals(GLOBAL_INFO_VIEW_NAME)) {
            return;
        }
        elementNameToView.put(name, view);
        idleViews.add(name);
    }

    public void setWorker(@Nonnull View view, @Nonnull ViewWorker worker) {
        viewWorkers.put(view.getId(), worker);
    }

    public void setGlobalInfoView(View globalInfoView) {
        this.globalInfoView = globalInfoView;
        idleViews.add(GLOBAL_INFO_VIEW_NAME);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArray(id, idleViews.toArray(new String[idleViews.size()]));
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        final String[] stringArray = savedInstanceState.getStringArray(id);
        if (stringArray != null) {
            idleViews.clear();
            idleViews.addAll(Arrays.asList(stringArray));
        }
    }

    public <T> void processSubmitResultUsingHandler(Handler handler, final Submitter.Result<T> result) {
        if (result.isSuccessful()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    highlightSuccess();
                }
            });
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    highlightFailure(result);
                }
            });
        }
    }

    public <T> void processSubmitResult(Submitter.Result<T> result) {
        if (result.isSuccessful()) {
            highlightSuccess();
        } else {
            highlightFailure(result);
        }
    }

    private <T> void highlightFailure(Submitter.Result<T> result) {
        final HashSet<String> toHide = new HashSet<>();
        for (final String name : elementNameToView.keySet()) {
            if (!idleViews.contains(name)) {
                toHide.add(name);
            }
        }
        if (!idleViews.contains(GLOBAL_INFO_VIEW_NAME)) {
            toHide.add(GLOBAL_INFO_VIEW_NAME);
        }

        for (final Submitter.FieldError error : result.fieldErrors) {
            final View viewInFault = elementNameToView.get(error.fieldInFault);
            if (viewInFault != null) {
                enrichView(viewInFault, error.errorText);
                viewInFault.setVisibility(View.VISIBLE);
                final int idFault = viewInFault.getId();
                final ViewWorker worker = viewWorkers.get(idFault);
                if (worker != null) {
                    worker.failureWork(viewInFault);
                }
                viewInFault.invalidate();
                if (!idleViews.remove(error.fieldInFault)) {
                    toHide.remove(error.fieldInFault);
                }
            }
        }
        if (result.generalError != null) {
            final int id = globalInfoView.getId();
            final ViewWorker infoWorker = viewWorkers.get(id);
            if (infoWorker != null) {
                infoWorker.failureWork(globalInfoView);
            }
            enrichView(globalInfoView, result.generalError);
            globalInfoView.setVisibility(View.VISIBLE);
            globalInfoView.invalidate();
            if (!idleViews.remove(GLOBAL_INFO_VIEW_NAME)) {
                toHide.remove(GLOBAL_INFO_VIEW_NAME);
            }
        }

        for (final String viewName : toHide) {
            if (viewName.equals(GLOBAL_INFO_VIEW_NAME)) {
                hideGlobalInfoView();
            } else {
                final View view = elementNameToView.get(viewName);
                if (view != null) {
                    hideView(viewName, view);
                }
            }
        }
    }

    private void highlightSuccess() {
        for (final Map.Entry<String, View> entry : elementNameToView.entrySet()) {
            final View viewInFault = entry.getValue();
            final String name = entry.getKey();
            if (!idleViews.contains(name)) {
                hideView(name, viewInFault);
            }
        }

        if (!idleViews.contains(GLOBAL_INFO_VIEW_NAME)) {
            hideGlobalInfoView();
        }
    }

    private void hideView(String name, View view) {
        enrichView(view, "");
        view.setVisibility(View.INVISIBLE);
        view.invalidate();
        idleViews.add(name);
    }

    private void hideGlobalInfoView() {
        enrichView(globalInfoView, "");
        final int id = globalInfoView.getId();
        final ViewWorker infoWorker = viewWorkers.get(id);
        if (infoWorker != null) {
            infoWorker.successWork(globalInfoView);
        }
        globalInfoView.setVisibility(View.INVISIBLE);
        globalInfoView.invalidate();
        idleViews.add(GLOBAL_INFO_VIEW_NAME);
    }

    private static void enrichView(View view, String text) {
        if (view instanceof TextView) {
            final TextView textView = (TextView) view;
            textView.setText(text);
            textView.setTextAppearance(view.getContext(), R.style.TextAppearanceError_Small);
        }
    }

}
