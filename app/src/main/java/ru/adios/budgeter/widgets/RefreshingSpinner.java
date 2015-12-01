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
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.UiThread;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.widget.SpinnerAdapter;

import ru.adios.budgeter.adapters.DecoratingAdapter;
import ru.adios.budgeter.adapters.PersistingStateful;
import ru.adios.budgeter.adapters.RefreshingAdapter;

/**
 * Created by Michail Kulikov
 * 11/27/15
 */
@UiThread
public class RefreshingSpinner<DataType> extends AppCompatSpinner {

    private RefreshingAdapter<DataType, DataType> refreshingAdapter;
    private DecoratingAdapter<DataType> decorator;
    private boolean requestedRefreshOnce = false;
    private int restoredSelection = -1;
    private boolean restored = false;
    private boolean restoreCommencing = false;

    public RefreshingSpinner(Context context) {
        super(context);
    }

    public RefreshingSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setRestoredSelection(int position) {
        if (restored) {
            setSelection(position);
        } else {
            this.restoredSelection = position;
        }
    }

    @Override
    public void setAdapter(final SpinnerAdapter a) {
        SpinnerAdapter adapter = a;
        if (adapter instanceof DecoratingAdapter) {
            //noinspection unchecked
            decorator = (DecoratingAdapter<DataType>) adapter;

            while (adapter instanceof DecoratingAdapter) {
                final DecoratingAdapter decAd = (DecoratingAdapter) adapter;
                if (SpinnerAdapter.class.isAssignableFrom(decAd.getWrappedType())) {
                    adapter = (SpinnerAdapter) decAd.getWrapped();
                } else {
                    break;
                }
            }
        }
        if (adapter instanceof RefreshingAdapter) {
            //noinspection unchecked
            refreshingAdapter = (RefreshingAdapter<DataType, DataType>) adapter;
            refreshingAdapter.setOnRefreshListener(new RefreshingAdapter.OnRefreshListener() {
                @Override
                public void onRefreshed() {
                    if (restoreCommencing) {
                        try {
                            if (restoredSelection >= 0) {
                                setSelection(restoredSelection);
                                restoredSelection = -1;
                            }
                            restored = true;
                        } finally {
                            restoreCommencing = false;
                        }
                    } else if (requestedRefreshOnce) {
                        performClick();
                    }
                }
                @Override
                public void onNoDataLoaded() {}
            });
        }

        super.setAdapter(a);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (refreshingAdapter != null) {
            if (!requestedRefreshOnce) {
                requestedRefreshOnce = true;
            }
            if (decorator != null) {
                if (decorator.isPositionTranslatable(which)) {
                    refreshingAdapter.refresh(decorator.getItemTranslating(which));
                }
            } else {
                refreshingAdapter.refresh(refreshingAdapter.getItem(which));
            }
        }
        super.onClick(dialog, which);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SpinnerAdapter adapter = getAdapter();
        return new SavedState(superState, adapter instanceof PersistingStateful ? ((PersistingStateful) adapter).getSavedState() : null);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        final SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        final SpinnerAdapter adapter = getAdapter();
        if (adapter instanceof PersistingStateful && savedState.adapterState != null) {
            ((PersistingStateful) adapter).restoreSavedState(savedState.adapterState);
        }

        restoreCommencing = true;
    }

    public static class SavedState extends BaseSavedState {

        final Parcelable adapterState;

        private SavedState(Parcelable superState, Parcelable adapterState) {
            super(superState);
            this.adapterState = adapterState;
        }

        private SavedState(Parcel in) {
            super(in);
            adapterState = in.readParcelable(null);
        }

        @Override
        public void writeToParcel(Parcel destination, int flags) {
            super.writeToParcel(destination, flags);
            destination.writeParcelable(adapterState, 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }

        };

    }

}
