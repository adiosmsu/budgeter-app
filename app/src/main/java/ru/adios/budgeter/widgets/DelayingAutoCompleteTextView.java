package ru.adios.budgeter.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;

import javax.annotation.concurrent.ThreadSafe;

/**
 * AutoCompleteTextView with delay after user's last input.
 * Designed for adapters that do remote or otherwise heavy requesting.
 *
 * Created by Michail Kulikov
 * 10/15/15
 */
@UiThread
public class DelayingAutoCompleteTextView extends AutoCompleteTextView {

    private static final int MESSAGE_TEXT_CHANGED = 100;
    private static final int DEFAULT_AUTOCOMPLETE_DELAY_MILLIS = 750;


    private int autoCompleteDelayMillis = DEFAULT_AUTOCOMPLETE_DELAY_MILLIS;
    private ProgressBar loadingIndicator;

    private final Handler mHandler = new InnerHandler(this);

    public DelayingAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setLoadingIndicator(ProgressBar loadingIndicator) {
        this.loadingIndicator = loadingIndicator;
    }

    public void setAutoCompleteDelayMillis(int autoCompleteDelayMillis) {
        this.autoCompleteDelayMillis = autoCompleteDelayMillis;
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.VISIBLE);
            loadingIndicator.invalidate();
        }

        mHandler.removeMessages(MESSAGE_TEXT_CHANGED);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_TEXT_CHANGED, text), autoCompleteDelayMillis);
    }

    private void performSuperFiltering(CharSequence text, int keyCode) {
        super.performFiltering(text, keyCode);
    }

    @Override
    public void onFilterComplete(int count) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.GONE);
            loadingIndicator.invalidate();
        }

        super.onFilterComplete(count);
    }

    @ThreadSafe
    private static class InnerHandler extends Handler {

        private final DelayingAutoCompleteTextView parent;

        public InnerHandler(DelayingAutoCompleteTextView parent) {
            this.parent = parent;
        }

        @Override
        public void handleMessage(Message msg) {
            parent.performSuperFiltering((CharSequence) msg.obj, msg.arg1);
        }

    }

}
