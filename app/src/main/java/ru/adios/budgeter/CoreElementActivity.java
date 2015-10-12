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

import java8.util.function.Consumer;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.HintedArrayAdapter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Michail Kulikov
 * 10/7/15
 */
public abstract class CoreElementActivity<T> extends AppCompatActivity {

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
            final SpinnerAdapter adapter = spinnerView.getAdapter();

            int pos = -1;
            if (adapter instanceof HintedArrayAdapter) {
                if (!((HintedArrayAdapter.ObjectContainer) spinnerView.getSelectedItem()).getObject().equals(account)) {
                    @SuppressWarnings("unchecked")
                    HintedArrayAdapter<Treasury.BalanceAccount> hintedArrayAdapter = (HintedArrayAdapter<Treasury.BalanceAccount>) adapter;
                    for (int i = 0; i < adapter.getCount(); i++) {
                        if (hintedArrayAdapter.getItem(i).getObject().equals(account)) {
                            pos = i;
                            break;
                        }
                    }
                }
            } else if (!spinnerView.getSelectedItem().equals(account)) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (adapter.getItem(i).equals(account)) {
                        pos = i;
                        break;
                    }
                }
            }

            if (pos >= 0) {
                spinnerView.setSelection(pos, true);
                spinnerView.invalidate();
            }
        }
    }

    public final void addFieldFragmentInfo(@IdRes int fragmentId, String fragmentFieldName, View fieldView, View fieldInfoView) {
        checkNotNull(fieldView, "fieldView is null");
        checkNotNull(fieldInfoView, "fieldInfoView is null");
        checkFragmentAllowance(fragmentId);

        final CoreElementFieldInfo fieldInfo = getCoreElementFieldInfo(fragmentId, fragmentFieldName);
        if (fieldInfo != null) {
            fieldInfo.errorHighlighter.addElementInfo(fieldInfo.coreFieldName, fieldInfoView);
            CoreNotifier.addLink(this, fieldView, fieldInfo.linker);
        } else {
            throw new IllegalArgumentException(getClass().getSimpleName() + " isn't aware of fragmentFieldName: " + fragmentFieldName);
        }
    }

    public final void addSubmitFragmentInfo(@IdRes final int fragmentId, Button submitButton, final String buttonName, @Nullable final Consumer<T> successRunnable) {
        checkNotNull(submitButton, "button is null");
        checkNotNull(buttonName, "buttonName is null");
        checkFragmentAllowance(fragmentId);

        final CoreElementSubmitInfo<T> submitInfo = getSubmitInfo(fragmentId, buttonName);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Submitter.Result<T>>() {
                    @Override
                    protected Submitter.Result<T> doInBackground(Void... params) {
                        return submitInfo.submitter.submit();
                    }

                    @Override
                    protected void onPostExecute(Submitter.Result<T> result) {
                        submitInfo.errorHighlighter.processSubmitResult(result);
                        if (result.isSuccessful()) {
                            if (submitInfo.successRunnable != null) {
                                submitInfo.successRunnable.accept(result.submitResult);
                            }

                            if (successRunnable != null) {
                                successRunnable.accept(result.submitResult);
                            }
                        }
                    }
                }.execute();
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

    protected abstract CoreElementSubmitInfo<T> getSubmitInfo(@IdRes int fragmentId, String buttonName);

    protected abstract int[] allowedFragments();

    @Nullable
    protected abstract CoreElementFieldInfo getCoreElementFieldInfo(@IdRes int fragmentId, String fragmentFieldName);

    protected static final class CoreElementFieldInfo {

        private final String coreFieldName;
        private final CoreNotifier.Linker linker;
        private final CoreErrorHighlighter errorHighlighter;

        protected CoreElementFieldInfo(String coreFieldName, CoreNotifier.Linker linker, CoreErrorHighlighter errorHighlighter) {
            this.coreFieldName = coreFieldName;
            this.linker = linker;
            this.errorHighlighter = errorHighlighter;
        }

    }

    protected static final class CoreElementSubmitInfo<T> {

        private final Submitter<T> submitter;
        @Nullable
        private final Consumer<T> successRunnable;
        private final CoreErrorHighlighter errorHighlighter;

        protected CoreElementSubmitInfo(Submitter<T> submitter, @Nullable Consumer<T> successRunnable, CoreErrorHighlighter errorHighlighter) {
            this.submitter = submitter;
            this.successRunnable = successRunnable;
            this.errorHighlighter = errorHighlighter;
        }

    }

}
