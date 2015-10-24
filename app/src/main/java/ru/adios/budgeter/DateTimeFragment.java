package ru.adios.budgeter;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.OffsetTime;

import ru.adios.budgeter.util.CoreErrorHighlighter;
import ru.adios.budgeter.util.CoreNotifier;
import ru.adios.budgeter.util.DateEditView;
import ru.adios.budgeter.util.TimeEditView;

/**
 * Fragment for easy handling of date-time input.
 */
public class DateTimeFragment extends CoreFragment {

    public static final String FIELD_DATE = "date";
    public static final String FIELD_TIME = "time";

    public static CollectibleFragmentInfoProvider getInfoProvider(@IdRes final int fragmentId, final CoreErrorHighlighter highlighter, String fieldCoreName, final TimestampSettable tsSet) {
        return new CollectibleFragmentInfoProvider.Builder(fragmentId, new CollectibleFragmentInfoProvider.Feedbacker() {
            @Override
            public void performFeedback(CoreElementActivity activity) {
                activity.dateTimeFeedback(tsSet.getTimestamp(), R.id.enter_date, R.id.enter_time);
            }
        })
                .addFieldInfo(FIELD_DATE, new CoreElementActivity.CoreElementFieldInfo(fieldCoreName, new CoreNotifier.ArbitraryLinker() {
                    @Override
                    public void link(Object data) {
                        if (data instanceof OffsetDateTime) {
                            final OffsetDateTime date = (OffsetDateTime) data;

                            final OffsetDateTime ts = tsSet.getTimestamp();
                            if (ts != null) {
                                tsSet.setTimestamp(OffsetDateTime.of(date.toLocalDate(), ts.toLocalTime(), date.getOffset()));
                            } else {
                                tsSet.setTimestamp(date);
                            }
                        }
                    }
                }, highlighter))
                .addFieldInfo(FIELD_TIME, new CoreElementActivity.CoreElementFieldInfo(fieldCoreName, new CoreNotifier.ArbitraryLinker() {
                    @Override
                    public void link(Object data) {
                        if (data instanceof OffsetTime) {
                            final OffsetTime time = (OffsetTime) data;

                            OffsetDateTime ts = tsSet.getTimestamp();
                            if (ts == null) {
                                ts = OffsetDateTime.now();
                            }
                            tsSet.setTimestamp(OffsetDateTime.of(ts.toLocalDate(), time.toLocalTime(), time.getOffset()));
                        }
                    }
                }, highlighter))
                .build();
    }

    public DateTimeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View inflated = inflater.inflate(R.layout.fragment_date_time, container, false);

        final CoreElementActivity activity = (CoreElementActivity) getActivity();

        // Register listeners in parent activity and initialize elements
        final DateEditView dateView = (DateEditView) inflated.findViewById(R.id.enter_date);
        final TimeEditView timeView = (TimeEditView) inflated.findViewById(R.id.enter_time);
        dateView.init(activity);
        timeView.init(activity);
        final int id = getId();
        activity.addFieldFragmentInfo(id, FIELD_DATE, dateView, inflated.findViewById(R.id.enter_datetime_info));
        activity.addFieldFragmentInfo(id, FIELD_TIME, timeView, inflated.findViewById(R.id.enter_datetime_info));

        return inflated;
    }

}
