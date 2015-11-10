package ru.adios.budgeter.core;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Created by Michail Kulikov
 * 11/10/15
 */
@NotThreadSafe
public abstract class AbstractCollectibleFeedbacker implements CollectibleFragmentInfoProvider.Feedbacker {

    private boolean cleared = true;

    @Override
    public final void clearViewReferences() {
        if (!cleared) {
            clearViewReferencesOptimal();
            cleared = true;
        }
    }

    protected abstract void clearViewReferencesOptimal();

    @Override
    public final void performFeedback() {
        if (!cleared) {
            performFeedbackSafe();
        }
    }

    protected abstract void performFeedbackSafe();

    @Override
    public final void collectEssentialViews(CoreElementActivity activity) {
        if (cleared) {
            collectEssentialViewsOptimal(activity);
            cleared = false;
        }
    }

    protected abstract void collectEssentialViewsOptimal(CoreElementActivity activity);

}
