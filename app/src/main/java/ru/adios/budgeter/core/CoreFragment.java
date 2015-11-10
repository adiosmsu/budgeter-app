package ru.adios.budgeter.core;

import android.app.Fragment;
import android.content.Context;
import android.support.annotation.UiThread;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by Michail Kulikov
 * 10/24/15
 */
@UiThread
public abstract class CoreFragment extends Fragment {

    /**
     * Overridden to fail early.
     * @param context activity of CoreElementActivity type.
     */
    @Override
    public void onAttach(Context context) {
        checkState(context instanceof CoreElementActivity, "Activity must extend CoreElementActivity: %s", context);
        super.onAttach(context);
    }

}
