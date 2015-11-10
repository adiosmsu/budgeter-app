package ru.adios.budgeter.widgets;

import android.content.Context;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.widget.Spinner;

/**
 * Created by Michail Kulikov
 * 10/13/15
 */
@UiThread
public class FlexibleNotifyingSpinner extends Spinner {

    private boolean notifyEvenIfSameSelection = false;

    public FlexibleNotifyingSpinner(Context context) {
        super(context);
    }

    public FlexibleNotifyingSpinner(Context context, int mode) {
        super(context, mode);
    }

    public FlexibleNotifyingSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlexibleNotifyingSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FlexibleNotifyingSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode) {
        super(context, attrs, defStyleAttr, mode);
    }

    @Override
    public void setSelection(int position, boolean animate) {
        super.setSelection(position, animate);
        flexibleSameSelectionNotification(position);
    }

    @Override
    public void setSelection(int position) {
        super.setSelection(position);
        flexibleSameSelectionNotification(position);
    }

    private void flexibleSameSelectionNotification(int position) {
        if (notifyEvenIfSameSelection && position == getSelectedItemPosition()) {
            final OnItemSelectedListener listener = getOnItemSelectedListener();
            if (listener != null) {
                listener.onItemSelected(this, this, position, getId());
            }
        }
    }

    public void setNotifyEvenIfSameSelection(boolean notifyEvenIfSameSelection) {
        this.notifyEvenIfSameSelection = notifyEvenIfSameSelection;
    }

    public boolean willNotifyEvenIfSameSelection() {
        return notifyEvenIfSameSelection;
    }

}
