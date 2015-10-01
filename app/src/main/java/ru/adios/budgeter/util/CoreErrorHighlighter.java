package ru.adios.budgeter.util;

import android.app.Activity;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.view.View;
import android.widget.TextView;

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import ru.adios.budgeter.Submitter;

/**
 * Designed to update submit errors into activities.
 *
 * Created by adios on 01.10.15.
 */
@ThreadSafe
public final class CoreErrorHighlighter {

    public interface ViewWorker {

        void successWork(View view);

        void failureWork(View view);

    }

    private final ImmutableMap<String, Integer> elementNameToViewId;
    private final ImmutableMap<Integer, ViewWorker> viewWorkers;
    @IdRes
    private final int globalInfoViewId;

    public CoreErrorHighlighter(ImmutableMap<String, Integer> elementNameToViewId, ImmutableMap<Integer, ViewWorker> viewWorkers, int globalInfoViewId) {
        this.elementNameToViewId = elementNameToViewId;
        this.viewWorkers = viewWorkers;
        this.globalInfoViewId = globalInfoViewId;
    }

    public void processSubmitResultUsingHandler(Handler handler, final Submitter.Result result, final Activity activity) {
        if (result.isSuccessful()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    highlightSuccess(activity);
                }
            });
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    highlightFailure(result, activity);
                }
            });
        }
    }

    public void processSubmitResult(Submitter.Result result, Activity activity) {
        if (result.isSuccessful()) {
            highlightSuccess(activity);
        } else {
            highlightFailure(result, activity);
        }
    }

    private void highlightFailure(Submitter.Result result, Activity activity) {
        for (final Submitter.FieldError error : result.fieldErrors) {
            final Integer id = elementNameToViewId.get(error.fieldInFault);
            if (id != null) {
                final View viewById = activity.findViewById(id);
                enrichView(viewById, error.errorText);
                viewById.setVisibility(View.VISIBLE);
                final ViewWorker worker = viewWorkers.get(id);
                if (worker != null) {
                    worker.failureWork(viewById);
                }
            }
        }
        if (result.generalError != null) {
            final View infoView = getInfoView(activity);
            final ViewWorker infoWorker = viewWorkers.get(globalInfoViewId);
            if (infoWorker != null) {
                infoWorker.failureWork(infoView);
            }
            enrichView(infoView, result.generalError);
        }
    }

    private void highlightSuccess(Activity activity) {
        for (final Integer id : elementNameToViewId.values()) {
            final View viewById = activity.findViewById(id);
            if (viewById.getVisibility() == View.VISIBLE) {
                enrichView(viewById, "");
                viewById.setVisibility(View.INVISIBLE);
            }
        }

        final View infoView = getInfoView(activity);
        if (infoView.getVisibility() == View.VISIBLE) {
            enrichView(infoView, "");
            final ViewWorker infoWorker = viewWorkers.get(globalInfoViewId);
            if (infoWorker != null) {
                infoWorker.successWork(infoView);
            }
            infoView.setVisibility(View.INVISIBLE);
        }
    }

    private View getInfoView(Activity activity) {
        return activity.findViewById(globalInfoViewId);
    }

    private static void enrichView(View view, String text) {
        if (view instanceof TextView) {
            ((TextView) view).setText(text);
        }
    }

    @NotThreadSafe
    public static final class Builder {

        private final ImmutableMap.Builder<String, Integer> viewsMapBuilder = new ImmutableMap.Builder<>();
        private final ImmutableMap.Builder<Integer, ViewWorker> workersBuilder = new ImmutableMap.Builder<>();
        @IdRes
        private int globalInfoViewId;

        public Builder addElementInfo(@Nonnull String name, @Nonnull @IdRes Integer id) {
            viewsMapBuilder.put(name, id);
            return this;
        }

        public Builder setWorker(@Nonnull @IdRes Integer viewId, @Nonnull ViewWorker worker) {
            workersBuilder.put(viewId, worker);
            return this;
        }

        public Builder setGlobalInfoViewId(@IdRes int globalInfoViewId) {
            this.globalInfoViewId = globalInfoViewId;
            return this;
        }

        public CoreErrorHighlighter build() {
            return new CoreErrorHighlighter(viewsMapBuilder.build(), workersBuilder.build(), globalInfoViewId);
        }

    }

}
