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

import android.content.Context;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;

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
