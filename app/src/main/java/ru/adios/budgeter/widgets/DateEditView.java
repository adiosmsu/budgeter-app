package ru.adios.budgeter.widgets;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import org.threeten.bp.Clock;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;

import javax.annotation.Nullable;

import ru.adios.budgeter.util.GeneralUtils;

/**
 * Created by Michail Kulikov
 * 10/16/15
 */
@UiThread
public class DateEditView extends TextView {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d yyyy").withLocale(getResources().getConfiguration().locale);

    public DateEditView(Context context) {
        super(context);
    }

    public DateEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DateEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public final void init(final Activity activity) {
        setDate(OffsetDateTime.now());
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
                final DatePickerDialogFragment fragment = new DatePickerDialogFragment();
                fragment.setDateEditView(DateEditView.this);
                fragment.show(ft, "date_edit_view_dialog");
            }
        });
    }

    public final String formatDate(OffsetDateTime date) {
        return date.format(formatter);
    }

    @Nullable
    public final OffsetDateTime formatText(CharSequence text) {
        try {
            return OffsetDateTime.of(LocalDateTime.of(LocalDate.parse(text, formatter), LocalTime.of(0, 0)), GeneralUtils.getLocalZoneOffset());
        } catch (DateTimeException ignore) {
            return null;
        }
    }

    public final void setDate(OffsetDateTime date) {
        setText(formatDate(date));
    }

    @Nullable
    public final OffsetDateTime getDate() {
        final CharSequence text = getText();
        if (text != null && text.length() > 0) {
            return formatText(text);
        }

        return null;
    }

    @UiThread
    public final static class DatePickerDialogFragment extends DialogFragment {

        private DateEditView dateEditView;

        public DatePickerDialogFragment() {}

        public void setDateEditView(DateEditView dateEditView) {
            this.dateEditView = dateEditView;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    final OffsetDateTime of = OffsetDateTime.of(year, monthOfYear + 1, dayOfMonth, 0, 0, 0, 0, ZoneOffset.systemDefault().getRules().getOffset(Clock.systemDefaultZone().instant()));
                    dateEditView.setDate(of);
                }
            };

            OffsetDateTime date = dateEditView.getDate();
            if (date == null) {
                date = OffsetDateTime.now();
                dateEditView.setDate(date);
            }

            return new DatePickerDialog(getActivity(), dateSetListener, date.getYear(), date.getMonth().getValue() - 1, date.getDayOfMonth());
        }

    }

}
