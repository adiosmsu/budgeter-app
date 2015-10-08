package ru.adios.budgeter;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import javax.annotation.Nullable;

import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Michail Kulikov
 * 10/7/15
 */
public abstract class CoreElementActivity extends AppCompatActivity {

    private boolean feedbackCommencing = false;

    public final boolean isFeedbackCommencing() {
        return feedbackCommencing;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        coreFeedback();
    }

    @LayoutRes
    protected abstract int getLayoutId();

    public final void coreFeedback() {
        if (feedbackCommencing)
            return;

        feedbackCommencing = true;
        try {
            coreFeedbackInternal();
        } finally {
            feedbackCommencing = false;
        }
    }

    protected abstract void coreFeedbackInternal();

    public final void addFieldFragmentInfo(@IdRes int fragmentId, String fragmentFieldName, View fieldView, View fieldInfoView) {
        checkNotNull(fieldView, "fieldView is null");
        checkNotNull(fieldInfoView, "fieldInfoView is null");
        boolean coincide = false;
        for (final int fid : allowedFragments()) {
            if (fragmentId == fid) {
                coincide = true;
                break;
            }
        }
        if (!coincide) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " doesn't work with " + getResources().getResourceName(fragmentId));
        }

        final CoreElementFieldInfo fieldInfo = getCoreElementFieldInfo(fragmentId, fragmentFieldName);
        if (fieldInfo != null) {
            getErrorHighlighter().addElementInfo(fieldInfo.coreFieldName, fieldInfoView);
            CoreNotifier.addLink(this, fieldView, fieldInfo.linker);
        } else {
            throw new IllegalArgumentException(getClass().getSimpleName() + " isn't aware of fragmentFieldName: " + fragmentFieldName);
        }
    }

    protected abstract CoreErrorHighlighter getErrorHighlighter();

    protected abstract int[] allowedFragments();

    @Nullable
    protected abstract CoreElementFieldInfo getCoreElementFieldInfo(@IdRes int fragmentId, String fragmentFieldName);

    protected static final class CoreElementFieldInfo {

        private final String coreFieldName;
        private final CoreNotifier.Linker linker;

        protected CoreElementFieldInfo(String coreFieldName, CoreNotifier.Linker linker) {
            this.coreFieldName = coreFieldName;
            this.linker = linker;
        }

    }

}
