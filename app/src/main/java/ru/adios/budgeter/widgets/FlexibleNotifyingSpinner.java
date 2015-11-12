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

package ru.adios.budgeter.widgets;

import android.content.Context;
import android.support.annotation.UiThread;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;

/**
 * Created by Michail Kulikov
 * 10/13/15
 */
@UiThread
public class FlexibleNotifyingSpinner extends AppCompatSpinner {

    private boolean notifyEvenIfSameSelection = false;

    public FlexibleNotifyingSpinner(Context context) {
        super(context);
    }

    public FlexibleNotifyingSpinner(Context context, int mode) {
        super(context, mode);
    }

    public FlexibleNotifyingSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlexibleNotifyingSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FlexibleNotifyingSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode) {
        super(context, attrs, defStyleAttr, mode);
    }

    @Override
    public void setSelection(int position, boolean animate) {
        super.setSelection(position, animate);
        flexibleSameSelectionNotification(position);
    }

    @Override
    public void setSelection(int position) {
        super.setSelection(position);
        flexibleSameSelectionNotification(position);
    }

    private void flexibleSameSelectionNotification(int position) {
        if (notifyEvenIfSameSelection && position == getSelectedItemPosition()) {
            final OnItemSelectedListener listener = getOnItemSelectedListener();
            if (listener != null) {
                listener.onItemSelected(this, this, position, getId());
            }
        }
    }

    public void setNotifyEvenIfSameSelection(boolean notifyEvenIfSameSelection) {
        this.notifyEvenIfSameSelection = notifyEvenIfSameSelection;
    }

    public boolean willNotifyEvenIfSameSelection() {
        return notifyEvenIfSameSelection;
    }

}
