package ru.adios.budgeter;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static com.google.common.base.Preconditions.checkState;

/**
 * Fragment for easy handling of date-time input.
 */
public class DateTimeFragment extends Fragment {

    public DateTimeFragment() {
        // Required empty public constructor
    }

    /**
     * Overridden to fail early.
     * @param activity activity of CoreElementActivity type.
     */
    @Override
    public void onAttach(Activity activity) {
        checkState(activity instanceof CoreElementActivity, "Activity must extend CoreElementActivity: %s", activity);
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_date_time, container, false);
    }

}
