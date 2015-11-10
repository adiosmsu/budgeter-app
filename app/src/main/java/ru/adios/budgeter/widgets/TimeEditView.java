package ru.adios.budgeter.widgets;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.common.base.Preconditions;

import org.threeten.bp.DateTimeException;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetTime;
import org.threeten.bp.format.DateTimeFormatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru.adios.budgeter.util.GeneralUtils;

/**
 * Created by Michail Kulikov
 * 10/16/15
 */
@UiThread
public class TimeEditView extends TextView {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm").withLocale(getResources().getConfiguration().locale);

    private TimePickerDialogFragment curDialog;

    public TimeEditView(Context context) {
        super(context);
    }

    public TimeEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimeEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public final void init(final AppCompatActivity activity, Bundle savedInstanceState) {
        Preconditions.checkState(getId() != NO_ID, "Set id first!");

        if (savedInstanceState != null) {
            try {
                curDialog = (TimePickerDialogFragment) activity.getSupportFragmentManager().getFragment(savedInstanceState, getIdForTx());
                curDialog.setTimeEditView(this);
            } catch (RuntimeException ignore) {}
        }

        setTime(OffsetTime.now());
        super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                final TimePickerDialogFragment fragment = new TimePickerDialogFragment();
                fragment.setTimeEditView(TimeEditView.this);
                fragment.show(ft, "time_edit_view_dialog");
                curDialog = fragment;
            }
        });
    }

    private String getIdForTx() {
        return "time_edit_picker_fragment_" + getId();
    }

    public final void retain(AppCompatActivity activity, Bundle outState) {
        if (curDialog != null) {
            activity.getSupportFragmentManager().putFragment(outState, getIdForTx(), curDialog);
        }
    }

    public final String formatTime(OffsetTime time) {
        return time.format(formatter);
    }

    @Nullable
    public final OffsetTime formatText(CharSequence text) {
        try {
            return OffsetTime.of(LocalTime.parse(text, formatter), GeneralUtils.getLocalZoneOffset());
        } catch (DateTimeException ignore) {
            return null;
        }
    }

    @Nullable
    public final OffsetTime getTime() {
        final CharSequence text = getText();
        if (text != null && text.length() > 0) {
            return formatText(text);
        }

        return null;
    }

    public final void setTime(OffsetTime time) {
        setText(formatTime(time));
    }

    @UiThread
    public final static class TimePickerDialogFragment extends DialogFragment {

        private TimeEditView timeEditView;

        public TimePickerDialogFragment() {}

        public void setTimeEditView(TimeEditView timeEditView) {
            this.timeEditView = timeEditView;
        }

        @Nonnull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final TimePickerDialog.OnTimeSetListener dateSetListener = new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    final OffsetTime of = OffsetTime.of(hourOfDay, minute, 0, 0, GeneralUtils.getLocalZoneOffset());
                    timeEditView.setTime(of);
                }
            };

            OffsetTime time = timeEditView.getTime();
            if (time == null) {
                time = OffsetTime.now();
                timeEditView.setTime(time);
            }

            return new TimePickerDialog(getActivity(), dateSetListener, time.getHour(), time.getMinute(), true);
        }

        @Override
        public void onDetach() {
            super.onDetach();
            final TimePickerDialogFragment me = timeEditView.curDialog;
            if (me != null) {
                timeEditView.curDialog = null;
            }
        }

    }

}
