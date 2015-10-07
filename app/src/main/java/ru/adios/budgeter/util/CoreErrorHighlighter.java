package ru.adios.budgeter.util;

import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import ru.adios.budgeter.Submitter;

/**
 * Designed to update submit errors into activities.
 *
 * Created by adios on 01.10.15.
 */
@NotThreadSafe
public final class CoreErrorHighlighter {

    public interface ViewWorker {

        void successWork(View view);

        void failureWork(View view);

    }

    private final HashMap<String, View> elementNameToView = new HashMap<>();
    private final HashMap<Integer, ViewWorker> viewWorkers = new HashMap<>();
    private View globalInfoView;

    public void addElementInfo(@Nonnull String name, @Nonnull View view) {
        elementNameToView.put(name, view);
    }

    public void setWorker(@Nonnull View view, @Nonnull ViewWorker worker) {
        viewWorkers.put(view.getId(), worker);
    }

    public void setGlobalInfoView(View globalInfoView) {
        this.globalInfoView = globalInfoView;
    }

    public void processSubmitResultUsingHandler(Handler handler, final Submitter.Result result) {
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

    public void processSubmitResult(Submitter.Result result) {
        if (result.isSuccessful()) {
            highlightSuccess();
        } else {
            highlightFailure(result);
        }
    }

    private void highlightFailure(Submitter.Result result) {
        for (final Submitter.FieldError error : result.fieldErrors) {
            final View viewInFault = elementNameToView.get(error.fieldInFault);
            if (viewInFault != null) {
                enrichView(viewInFault, error.errorText);
                viewInFault.setVisibility(View.VISIBLE);
                final ViewWorker worker = viewWorkers.get(viewInFault.getId());
                if (worker != null) {
                    worker.failureWork(viewInFault);
                }
            }
        }
        if (result.generalError != null) {
            final ViewWorker infoWorker = viewWorkers.get(globalInfoView.getId());
            if (infoWorker != null) {
                infoWorker.failureWork(globalInfoView);
            }
            enrichView(globalInfoView, result.generalError);
        }
    }

    private void highlightSuccess() {
        for (final View viewInFault : elementNameToView.values()) {
            if (viewInFault.getVisibility() == View.VISIBLE) {
                enrichView(viewInFault, "");
                viewInFault.setVisibility(View.INVISIBLE);
            }
        }

        if (globalInfoView.getVisibility() == View.VISIBLE) {
            enrichView(globalInfoView, "");
            final ViewWorker infoWorker = viewWorkers.get(globalInfoView.getId());
            if (infoWorker != null) {
                infoWorker.successWork(globalInfoView);
            }
            globalInfoView.setVisibility(View.INVISIBLE);
        }
    }

    private static void enrichView(View view, String text) {
        if (view instanceof TextView) {
            ((TextView) view).setText(text);
        }
    }

}
