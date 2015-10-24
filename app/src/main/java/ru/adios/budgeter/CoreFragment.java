package ru.adios.budgeter;

import android.app.Activity;
import android.app.Fragment;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by Michail Kulikov
 * 10/24/15
 */
public abstract class CoreFragment extends Fragment {

    /**
     * Overridden to fail early.
     * @param activity activity of CoreElementActivity type.
     */
    @Override
    public void onAttach(Activity activity) {
        checkState(activity instanceof CoreElementActivity, "Activity must extend CoreElementActivity: %s", activity);
        super.onAttach(activity);
    }

}
