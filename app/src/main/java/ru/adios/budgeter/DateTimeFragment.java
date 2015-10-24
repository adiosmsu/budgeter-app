package ru.adios.budgeter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Fragment for easy handling of date-time input.
 */
public class DateTimeFragment extends CoreFragment {

    public static final String FIELD_DATE = "date";
    public static final String FIELD_TIME = "time";



    public DateTimeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View inflated = inflater.inflate(R.layout.fragment_date_time, container, false);

        final CoreElementActivity activity = (CoreElementActivity) getActivity();

        return inflated;
    }

}
