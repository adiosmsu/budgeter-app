package ru.adios.budgeter.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.UiThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.OffsetTime;

import ru.adios.budgeter.R;
import ru.adios.budgeter.TimestampSettable;
import ru.adios.budgeter.core.AbstractCollectibleFeedbacker;
import ru.adios.budgeter.core.CollectibleFragmentInfoProvider;
import ru.adios.budgeter.core.CoreElementActivity;
import ru.adios.budgeter.core.CoreErrorHighlighter;
import ru.adios.budgeter.core.CoreFragment;
import ru.adios.budgeter.core.CoreNotifier;
import ru.adios.budgeter.core.Feedbacking;
import ru.adios.budgeter.widgets.DateEditView;
import ru.adios.budgeter.widgets.TimeEditView;

/**
 * Fragment for easy handling of date-time input.
 */
@UiThread
public class DateTimeFragment extends CoreFragment {

    public static final String FIELD_DATE = "date";
    public static final String FIELD_TIME = "time";

    public static CollectibleFragmentInfoProvider getInfoProvider(@IdRes final int fragmentId,
                                                                  final CoreErrorHighlighter highlighter,
                                                                  String fieldCoreName,
                                                                  final TimestampSettable tsSet) {
        return new CollectibleFragmentInfoProvider.Builder(
                fragmentId,
                new Feedbacker(fragmentId, tsSet),
                highlighter
        )
                .addFieldInfo(FIELD_DATE, new CoreElementActivity.CoreElementFieldInfo(fieldCoreName, new CoreNotifier.ArbitraryLinker() {
                    @Override
                    public boolean link(Object data) {
                        if (data instanceof OffsetDateTime) {
                            final OffsetDateTime date = (OffsetDateTime) data;

                            final OffsetDateTime ts = tsSet.getTimestamp();
                            if (ts != null) {
                                final OffsetDateTime cur = OffsetDateTime.of(date.toLocalDate(), ts.toLocalTime(), date.getOffset());
                                if (!cur.equals(ts)) {
                                    tsSet.setTimestamp(cur);
                                    return true;
                                }
                            } else {
                                tsSet.setTimestamp(date);
                                return true;
                            }
                        }
                        return false;
                    }
                }, highlighter))
                .addFieldInfo(FIELD_TIME, new CoreElementActivity.CoreElementFieldInfo(fieldCoreName, new CoreNotifier.ArbitraryLinker() {
                    @Override
                    public boolean link(Object data) {
                        if (data instanceof OffsetTime) {
                            final OffsetTime time = (OffsetTime) data;

                            OffsetDateTime ts = tsSet.getTimestamp();
                            if (ts == null) {
                                ts = OffsetDateTime.now();
                            }
                            final OffsetDateTime cur = OffsetDateTime.of(ts.toLocalDate(), time.toLocalTime(), time.getOffset());
                            if (!cur.equals(ts)) {
                                tsSet.setTimestamp(cur);
                                return true;
                            }
                        }
                        return false;
                    }
                }, highlighter))
                .build();
    }


    private CoreElementActivity activity;
    private DateEditView dateView;
    private TimeEditView timeView;

    public DateTimeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (CoreElementActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View inflated = inflater.inflate(R.layout.fragment_date_time, container, false);

        final CoreElementActivity activity = (CoreElementActivity) getActivity();

        // Register listeners in parent activity and initialize elements
        dateView = (DateEditView) inflated.findViewById(R.id.enter_date);
        timeView = (TimeEditView) inflated.findViewById(R.id.enter_time);
        dateView.init(activity, savedInstanceState);
        timeView.init(activity, savedInstanceState);
        final int id = getId();
        activity.addFieldFragmentInfo(id, FIELD_DATE, dateView, inflated.findViewById(R.id.enter_datetime_info));
        activity.addFieldFragmentInfo(id, FIELD_TIME, timeView, inflated.findViewById(R.id.enter_datetime_info));

        return inflated;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        dateView.retain(activity, outState);
        timeView.retain(activity, outState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
    }


    public static final class Feedbacker extends AbstractCollectibleFeedbacker {

        @IdRes
        private final int fragmentId;
        private final TimestampSettable tsSet;

        private Feedbacker(@IdRes int fragmentId, TimestampSettable tsSet) {
            this.fragmentId = fragmentId;
            this.tsSet = tsSet;
        }

        private DateEditView enterDate;
        private TimeEditView enterTime;

        @Override
        public void clearViewReferencesOptimal() {
            enterDate = null;
            enterTime = null;
        }

        @Override
        public void performFeedbackSafe() {
            Feedbacking.dateTimeFeedback(tsSet.getTimestamp(), enterDate, enterTime);
        }

        @Override
        public void collectEssentialViewsOptimal(CoreElementActivity activity) {
            final View fragmentLayout = activity.findViewById(fragmentId);
            enterDate = (DateEditView) fragmentLayout.findViewById(R.id.enter_date);
            enterTime = (TimeEditView) fragmentLayout.findViewById(R.id.enter_time);
        }

    }

}
