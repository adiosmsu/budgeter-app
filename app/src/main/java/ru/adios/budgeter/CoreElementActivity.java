package ru.adios.budgeter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.joda.money.CurrencyUnit;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import ru.adios.budgeter.api.Treasury;
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

    protected final void textViewFeedback(String text, @IdRes int textViewId) {
        if (text != null) {
            final TextView textView = (TextView) findViewById(textViewId);
            if (!textView.getText().equals(text)) {
                textView.setText(text);
                textView.invalidate();
            }
        }
    }

    protected final void decimalTextViewFeedback(BigDecimal decimal, @IdRes int textViewId) {
        if (decimal != null) {
            final String decimalText = decimal.toPlainString();
            final TextView textView = (TextView) findViewById(textViewId);
            if (!decimalText.equals(textView.getText())) {
                textView.setText(decimalText);
                textView.invalidate();
            }
        }
    }

    protected final void currenciesSpinnerFeedback(CurrencyUnit unit, @IdRes int spinnerId) {
        if (unit != null) {
            final Spinner spinner = (Spinner) findViewById(spinnerId);
            if (!spinner.getSelectedItem().toString().equals(unit.getCode())) {
                spinner.setSelection(Constants.getCurrencyDropdownPosition(unit), true);
                spinner.invalidate();
            }
        }
    }

    protected final void accountSpinnerFeedback(Treasury.BalanceAccount account, @IdRes int spinnerId) {
        if (account != null) {
            final Spinner spinnerView = (Spinner) findViewById(spinnerId);
            if (!spinnerView.getSelectedItem().equals(account)) {
                int pos = -1;
                final SpinnerAdapter adapter = spinnerView.getAdapter();
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (adapter.getItem(i).equals(account)) {
                        pos = i;
                        break;
                    }
                }
                if (pos >= 0) {
                    spinnerView.setSelection(pos, true);
                    spinnerView.invalidate();
                }
            }
        }
    }

    public final void addFieldFragmentInfo(@IdRes int fragmentId, String fragmentFieldName, View fieldView, View fieldInfoView) {
        checkNotNull(fieldView, "fieldView is null");
        checkNotNull(fieldInfoView, "fieldInfoView is null");
        checkFragmentAllowance(fragmentId);

        final CoreElementFieldInfo fieldInfo = getCoreElementFieldInfo(fragmentId, fragmentFieldName);
        if (fieldInfo != null) {
            getErrorHighlighter(fragmentId).addElementInfo(fieldInfo.coreFieldName, fieldInfoView);
            CoreNotifier.addLink(this, fieldView, fieldInfo.linker);
        } else {
            throw new IllegalArgumentException(getClass().getSimpleName() + " isn't aware of fragmentFieldName: " + fragmentFieldName);
        }
    }

    public final void addButtonFragmentInfo(@IdRes final int fragmentId, String buttonName, Button button, @Nullable final Runnable successRunnable) {
        checkNotNull(buttonName, "buttonName is null");
        checkNotNull(button, "button is null");
        checkFragmentAllowance(fragmentId);

        final CoreElementSubmitInfo submitInfo = getSubmitInfo(fragmentId, buttonName);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Submitter.Result>() {
                    @Override
                    protected Submitter.Result doInBackground(Void... params) {
                        return submitInfo.submitter.submit();
                    }

                    @Override
                    protected void onPostExecute(Submitter.Result result) {
                        getErrorHighlighter(fragmentId).processSubmitResult(result);
                        if (!result.isSuccessful()) {
                            if (submitInfo.successRunnable != null) {
                                submitInfo.successRunnable.run();
                            }

                            if (successRunnable != null) {
                                successRunnable.run();
                            }
                        }
                    }
                }.doInBackground();
            }
        });
    }

    private void checkFragmentAllowance(@IdRes int fragmentId) {
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
    }

    protected abstract CoreErrorHighlighter getErrorHighlighter(@IdRes int fragmentId);

    protected abstract CoreElementSubmitInfo getSubmitInfo(@IdRes int fragmentId, String buttonName);

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

    protected static final class CoreElementSubmitInfo {

        private final Submitter submitter;
        @Nullable
        private final Runnable successRunnable;

        protected CoreElementSubmitInfo(Submitter submitter, @Nullable Runnable successRunnable) {
            this.submitter = submitter;
            this.successRunnable = successRunnable;
        }

    }

}
