package ru.adios.budgeter;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

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

    public abstract void addFieldFragmentInfo(@IdRes int fragmentId, String fragmentFieldName, View fieldView, View fieldInfoView);

}
