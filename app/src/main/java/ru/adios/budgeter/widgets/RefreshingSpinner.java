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
import android.database.DataSetObserver;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.UiThread;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SpinnerAdapter;

import ru.adios.budgeter.adapters.DecoratingAdapter;
import ru.adios.budgeter.adapters.PersistingStateful;
import ru.adios.budgeter.adapters.RefreshingAdapter;
import ru.adios.budgeter.util.EmptyOnItemSelectedListener;

/**
 * Created by Michail Kulikov
 * 11/27/15
 */
@UiThread
public class RefreshingSpinner extends AppCompatSpinner {

    private RefreshingAdapter refreshingAdapter;
    private DecoratingAdapter decorator;
    private OnItemSelectedListener selectionListener;
    private boolean requestedRefreshOnce = false;
    private int restoredSelection = -1;
    private boolean restored = false;
    private boolean restoreCommencing = false;
    private boolean innerSelectionCommencing = false;
    private RefreshingAdapter.OnRefreshListener onRefreshListener = new RefreshingAdapter.OnRefreshListener() {
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
        public void onNoDataLoaded() {
        }
    };
    private DataSetObserver dataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            int defPos = refreshingAdapter.getDefaultPosition();
            if (defPos >= 0) {
                innerSelectionCommencing = true;
                setSelection(decorator != null ? decorator.decoratedPositionToDecorators(defPos) : defPos);
            }
        }
    };

    public RefreshingSpinner(Context context) {
        super(context);
        super.setOnItemSelectedListener(new InnerSelectionListener());
    }

    public RefreshingSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setOnItemSelectedListener(new InnerSelectionListener());
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
        SpinnerAdapter adapter = getDecoratedAdapter(getAdapter());
        if (adapter instanceof RefreshingAdapter) {
            final RefreshingAdapter ra = (RefreshingAdapter) adapter;
            ra.removeOnRefreshListener(onRefreshListener);
            ra.unregisterDataSetObserver(dataSetObserver);
        }

        adapter = a;
        if (adapter instanceof DecoratingAdapter) {
            decorator = (DecoratingAdapter) adapter;
            adapter = getDecoratedAdapter(adapter);
        }
        if (adapter instanceof RefreshingAdapter) {
            refreshingAdapter = (RefreshingAdapter) adapter;
            refreshingAdapter.setOnRefreshListener(onRefreshListener);
            refreshingAdapter.registerDataSetObserver(dataSetObserver);
        }

        super.setAdapter(a);
    }

    private SpinnerAdapter getDecoratedAdapter(SpinnerAdapter adapter) {
        while (adapter instanceof DecoratingAdapter) {
            final DecoratingAdapter decAd = (DecoratingAdapter) adapter;
            if (SpinnerAdapter.class.isAssignableFrom(decAd.getWrappedType())) {
                adapter = (SpinnerAdapter) decAd.getWrapped();
            } else {
                break;
            }
        }
        return adapter;
    }

    @Override
    public final void setOnItemSelectedListener(OnItemSelectedListener listener) {
        throw new UnsupportedOperationException(
                "This implementation doesn't support traditional OnItemSelectedListener method thanks to Google's brilliant design of AdapterView"
        );
    }

    public void setSelectionListener(OnItemSelectedListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    public OnItemSelectedListener getSelectionListener() {
        return selectionListener;
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

    private final class InnerSelectionListener extends EmptyOnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (refreshingAdapter != null) {
                if (!requestedRefreshOnce) {
                    requestedRefreshOnce = true;
                }
                if (!innerSelectionCommencing) {
                    if (decorator != null) {
                        if (decorator.isPositionTranslatable(position)) {
                            //noinspection unchecked
                            refreshingAdapter.refresh(decorator.getItemTranslating(position));
                        }
                    } else {
                        //noinspection unchecked
                        refreshingAdapter.refresh(refreshingAdapter.getItem(position));
                    }
                } else {
                    innerSelectionCommencing = false;
                }
            }

            if (selectionListener != null) {
                selectionListener.onItemSelected(parent, view, position, id);
            }
        }
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
