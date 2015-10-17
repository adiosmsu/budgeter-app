package ru.adios.budgeter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.google.common.collect.ImmutableCollection;

import org.joda.money.CurrencyUnit;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.OffsetTime;

import java.math.BigDecimal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java8.util.function.Consumer;
import ru.adios.budgeter.util.BalancedMenuHandler;
import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.DateEditView;
import ru.adios.budgeter.util.HintedArrayAdapter;
import ru.adios.budgeter.util.TimeEditView;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Michail Kulikov
 * 10/7/15
 */
public abstract class CoreElementActivity extends AppCompatActivity {

    private BalancedMenuHandler menuHandler;
    private boolean feedbackCommencing = false;

    public final boolean isFeedbackCommencing() {
        return feedbackCommencing;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getLayoutId());
        initMenuHandler();
        coreFeedback();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        initMenuHandler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initMenuHandler();
    }

    @Override
    protected void onPause() {
        super.onPause();
        menuHandler.destroy();
        menuHandler = null;
    }

    private void initMenuHandler() {
        if (menuHandler == null) {
            menuHandler = new BalancedMenuHandler();
            menuHandler.init(this);
        }
    }

    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(getMenuId(), menu);
        menuHandler.onCreateMenu(menu);
        return true;
    }

    protected abstract FragmentsInfoProvider getInfoProvider();

    @LayoutRes
    protected abstract int getLayoutId();

    @MenuRes
    protected abstract int getMenuId();

    public final void coreFeedback() {
        if (feedbackCommencing)
            return;

        feedbackCommencing = true;
        try {
            getInfoProvider().performFeedback();
            activityInnerFeedback();
        } finally {
            feedbackCommencing = false;
        }
    }

    protected abstract void activityInnerFeedback();

    protected final void textViewFeedback(String text, @IdRes int textViewId) {
        if (text != null) {
            innerTextViewFeedback(text, (TextView) findViewById(textViewId));
        }
    }

    protected final void textViewFeedback(String text, @IdRes int fragmentId, @IdRes int textViewId) {
        if (text != null) {
            innerTextViewFeedback(text, (TextView) findViewById(fragmentId).findViewById(textViewId));
        }
    }

    private void innerTextViewFeedback(@Nonnull String text, TextView textView) {
        if (!textView.getText().equals(text)) {
            textView.setText(text);
            textView.invalidate();
        }
    }

    protected final void decimalTextViewFeedback(BigDecimal decimal, @IdRes int textViewId) {
        if (decimal != null) {
            innerDecimalTextViewFeedback(decimal, (TextView) findViewById(textViewId));
        }
    }

    protected final void decimalTextViewFeedback(BigDecimal decimal, @IdRes int fragmentId, @IdRes int textViewId) {
        if (decimal != null) {
            innerDecimalTextViewFeedback(decimal, (TextView) findViewById(fragmentId).findViewById(textViewId));
        }
    }

    private void innerDecimalTextViewFeedback(@Nonnull BigDecimal decimal, TextView textView) {
        final String decimalText = decimal.toPlainString();
        if (!decimalText.equals(textView.getText())) {
            textView.setText(decimalText);
            textView.invalidate();
        }
    }

    protected final void currenciesSpinnerFeedback(CurrencyUnit unit, @IdRes int spinnerId) {
        if (unit != null) {
            innerCurrenciesSpinnerFeedback(unit, (Spinner) findViewById(spinnerId));
        }
    }

    protected final void currenciesSpinnerFeedback(CurrencyUnit unit, @IdRes int fragmentId, @IdRes int spinnerId) {
        if (unit != null) {
            innerCurrenciesSpinnerFeedback(unit, (Spinner) findViewById(fragmentId).findViewById(spinnerId));
        }
    }

    private void innerCurrenciesSpinnerFeedback(@Nonnull CurrencyUnit unit, Spinner spinner) {
        if (!spinner.getSelectedItem().toString().equals(unit.getCode())) {
            spinner.setSelection(Constants.getCurrencyDropdownPosition(unit), true);
            spinner.invalidate();
        }
    }

    protected final <T> void hintedArraySpinnerFeedback(T object, @IdRes int spinnerId) {
        if (object != null) {
            innerHintedArraySpinnerFeedback(object, (Spinner) findViewById(spinnerId));
        }
    }

    protected final <T> void hintedArraySpinnerFeedback(T object, @IdRes int fragmentId, @IdRes int spinnerId) {
        if (object != null) {
            innerHintedArraySpinnerFeedback(object, (Spinner) findViewById(fragmentId).findViewById(spinnerId));
        }
    }

    private <T> void innerHintedArraySpinnerFeedback(T object, Spinner spinnerView) {
        final SpinnerAdapter adapter = spinnerView.getAdapter();

        int pos = -1;
        if (adapter instanceof HintedArrayAdapter) {
            if (!((HintedArrayAdapter.ObjectContainer) spinnerView.getSelectedItem()).getObject().equals(object)) {
                @SuppressWarnings("unchecked")
                final HintedArrayAdapter<T> hintedArrayAdapter = (HintedArrayAdapter) adapter;
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (hintedArrayAdapter.getItem(i).getObject().equals(object)) {
                        pos = i;
                        break;
                    }
                }
            }
        } else if (!spinnerView.getSelectedItem().equals(object)) {
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).equals(object)) {
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

    protected final void radioGroupFeedback(Enum e, @IdRes int radioId) {
        if (e != null) {
            final RadioGroup radioGroup = (RadioGroup) findViewById(radioId);
            final int ord = e.ordinal();

            @IdRes
            final int checked = radioGroup.getCheckedRadioButtonId();
            for (int i = 0; i < radioGroup.getChildCount(); i++) {
                @IdRes
                final int id = radioGroup.getChildAt(i).getId();

                if (id == checked && i == ord) {
                    return;
                }

                if (i == ord) {
                    radioGroup.check(id);
                    radioGroup.invalidate();
                    return;
                }
            }
        }
    }

    protected final void dateTimeFeedback(OffsetDateTime dateTime, @IdRes int dateId, @IdRes int timeId) {
        if (dateTime != null) {
            final DateEditView dateEditView = (DateEditView) findViewById(dateId);
            final TimeEditView timeEditView = (TimeEditView) findViewById(timeId);

            final OffsetDateTime date = DateTimeUtils.cutTime(dateTime);
            final OffsetTime time = dateTime.toOffsetTime();

            if (!date.equals(dateEditView.getDate())) {
                dateEditView.setDate(date);
                dateEditView.invalidate();
            }
            if (!time.equals(timeEditView.getTime())) {
                timeEditView.setTime(time);
                timeEditView.invalidate();
            }
        }
    }

    public final void addFieldFragmentInfo(@IdRes int fragmentId, String fragmentFieldName, View fieldView, View fieldInfoView) {
        checkNotNull(fieldView, "fieldView is null");
        checkNotNull(fieldInfoView, "fieldInfoView is null");
        checkFragmentAllowance(fragmentId);

        final CoreElementFieldInfo fieldInfo = getInfoProvider().getCoreElementFieldInfo(fragmentId, fragmentFieldName);
        if (fieldInfo != null) {
            fieldInfo.errorHighlighter.addElementInfo(fieldInfo.coreFieldName, fieldInfoView);
            CoreNotifier.addLink(this, fieldView, fieldInfo.linker);
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
                        submitter.submitAndStoreResult();
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
        boolean coincide = false;
        for (final int fid : getInfoProvider().allowedFragments()) {
            if (fragmentId == fid) {
                coincide = true;
                break;
            }
        }
        if (!coincide) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " doesn't work with " + getResources().getResourceName(fragmentId));
        }
    }

    protected interface FragmentsInfoProvider {

        CoreElementSubmitInfo getSubmitInfo(@IdRes int fragmentId, String buttonName);

        ImmutableCollection<Integer> allowedFragments();

        CoreElementFieldInfo getCoreElementFieldInfo(@IdRes int fragmentId, String fragmentFieldName);

        void performFeedback();

    }

    protected static final class CoreElementFieldInfo {

        protected final String coreFieldName;
        protected final CoreNotifier.Linker linker;
        protected final CoreErrorHighlighter errorHighlighter;

        protected CoreElementFieldInfo(String coreFieldName, CoreNotifier.Linker linker, CoreErrorHighlighter errorHighlighter) {
            this.coreFieldName = coreFieldName;
            this.linker = linker;
            this.errorHighlighter = errorHighlighter;
        }

    }

    protected static final class CoreElementSubmitInfo<T, Sub extends Submitter<T>> {

        protected final Sub submitter;
        @Nullable
        protected final Consumer<T> successRunnable;
        protected final CoreErrorHighlighter errorHighlighter;

        protected CoreElementSubmitInfo(Sub submitter, @Nullable Consumer<T> successRunnable, CoreErrorHighlighter errorHighlighter) {
            this.submitter = submitter;
            this.successRunnable = successRunnable;
            this.errorHighlighter = errorHighlighter;
        }

    }

}
