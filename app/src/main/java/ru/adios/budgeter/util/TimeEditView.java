package ru.adios.budgeter.util;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import org.threeten.bp.Clock;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.OffsetTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;

import javax.annotation.Nullable;

/**
 * Created by Michail Kulikov
 * 10/16/15
 */
public class TimeEditView extends TextView {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm").withLocale(getResources().getConfiguration().locale);

    public TimeEditView(Context context) {
        super(context);
    }

    public TimeEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimeEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public final void init(final Activity activity) {
        setTime(OffsetTime.now());
        super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
                final TimePickerDialogFragment fragment = new TimePickerDialogFragment();
                fragment.setTimeEditView(TimeEditView.this);
                fragment.show(ft, "time_edit_view_dialog");
            }
        });
    }

    public final String formatTime(OffsetTime time) {
        return time.format(formatter);
    }

    @Nullable
    public final OffsetTime formatText(CharSequence text) {
        try {
            return OffsetTime.parse(text, formatter);
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

    public final static class TimePickerDialogFragment extends DialogFragment {

        private TimeEditView timeEditView;

        public TimePickerDialogFragment() {}

        public void setTimeEditView(TimeEditView timeEditView) {
            this.timeEditView = timeEditView;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final TimePickerDialog.OnTimeSetListener dateSetListener = new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    final OffsetTime of = OffsetTime.of(hourOfDay, minute, 0, 0, ZoneOffset.systemDefault().getRules().getOffset(Clock.systemDefaultZone().instant()));
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

    }

}
