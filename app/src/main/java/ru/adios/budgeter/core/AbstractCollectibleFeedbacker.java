/*
 *
 *  *
 *  *  * Copyright 2015 Michael Kulikov
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

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
