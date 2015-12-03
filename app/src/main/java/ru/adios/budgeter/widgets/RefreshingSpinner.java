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
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

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
    @StringRes
    private int refreshFailedToastResource = -1;

    private final InnerRefreshController onRefreshController = new InnerRefreshController();
    private final InnerRestoreController restoreController = new InnerRestoreController(this);

    public RefreshingSpinner(Context context) {
        super(context);
        super.setOnItemSelectedListener(new InnerSelectionListener());
    }

    public RefreshingSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setOnItemSelectedListener(new InnerSelectionListener());
    }

    public boolean refreshCurrentWithPossibleValue(Object value) {
        if (refreshingAdapter != null) {
            onRefreshController.refCurVal = value;
            return refreshingAdapter.refreshCurrent();
        }

        return false;
    }

    public void setRefreshFailedToastResource(@StringRes int refreshFailedToastResource) {
        this.refreshFailedToastResource = refreshFailedToastResource;
    }

    public void setRestoredSelection(int position) {
        restoreController.setRestoredSelection(position);
    }

    @Override
    public void setAdapter(final SpinnerAdapter a) {
        SpinnerAdapter adapter = DecoratingAdapter.Static.getDecoratedAdapter(getAdapter(), SpinnerAdapter.class);
        if (adapter instanceof RefreshingAdapter) {
            final RefreshingAdapter ra = (RefreshingAdapter) adapter;
            ra.removeOnRefreshListener(onRefreshController);
            ra.removeOnRestoreListener(restoreController);
        } else if (adapter instanceof PersistingStateful) {
            ((PersistingStateful) adapter).removeOnRestoreListener(restoreController);
        }

        adapter = a;
        if (adapter instanceof DecoratingAdapter) {
            decorator = (DecoratingAdapter) adapter;
            adapter = DecoratingAdapter.Static.getDecoratedAdapter(adapter, SpinnerAdapter.class);
        }
        if (adapter instanceof RefreshingAdapter) {
            refreshingAdapter = (RefreshingAdapter) adapter;
            refreshingAdapter.setOnRefreshListener(onRefreshController);
            refreshingAdapter.setOnRestoreListener(restoreController);
        } else if (adapter instanceof PersistingStateful) {
            ((PersistingStateful) adapter).setOnRestoreListener(restoreController);
        }

        super.setAdapter(a);
    }

    @Override
    public final void setOnItemSelectedListener(OnItemSelectedListener listener) {
        throw new UnsupportedOperationException(
                "This implementation doesn't support traditional OnItemSelectedListener method thanks to Google's brilliant design of AdapterView"
        );
    }

    @Override
    public void setSelection(int position) {
        final int previousPosition = getSelectedItemPosition();
        super.setSelection(position);
        checkSameSelection(position, previousPosition);
    }

    @Override
    public void setSelection(int position, boolean animate) {
        final int previousPosition = getSelectedItemPosition();
        super.setSelection(position, animate);
        checkSameSelection(position, previousPosition);
    }

    private void checkSameSelection(int position, int previousPosition) {
        if (position == previousPosition) {
            //noinspection ConstantConditions
            getOnItemSelectedListener().onItemSelected(this, this, position, getAdapter().getItemId(position));
        }
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
        final SpinnerAdapter adapter = getActualAdapter();
        return new SavedState(superState, adapter instanceof PersistingStateful ? ((PersistingStateful) adapter).getSavedState() : null);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        final SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        restoreController.commenceRestore(getActualAdapter(), savedState);
    }

    private SpinnerAdapter getActualAdapter() {
        return decorator != null
                ? DecoratingAdapter.Static.getDecoratedAdapter(getAdapter(), SpinnerAdapter.class)
                : getAdapter();
    }

    void makeFailToast() {
        if (refreshFailedToastResource >= 0) {
            Toast.makeText(getContext(), refreshFailedToastResource, Toast.LENGTH_LONG)
                    .show();
        } else {
            Toast.makeText(getContext(), "Refresh failed", Toast.LENGTH_LONG)
                    .show();
        }
    }


    private final class InnerRefreshController implements RefreshingAdapter.OnRefreshListener {

        private boolean requestedRefreshOnce = false;
        Object refCurVal;
        boolean innerSelectionCommencing = false;

        @Override
        public void onRefreshed() {
            if (requestedRefreshOnce) {
                if (refCurVal == null) {
                    performClick();
                }
            }

            final RefreshingAdapter refreshingAdapter = RefreshingSpinner.this.refreshingAdapter;

            if (refCurVal != null) {
                int count = refreshingAdapter.getCount();
                for (int i = 0; i < count; i++) {
                    if (refCurVal.equals(refreshingAdapter.getItem(i))) {
                        commenceInnerSelection(i);
                        break;
                    }
                }
                refCurVal = null;
            } else {
                int defPos = refreshingAdapter.getDefaultPosition();
                if (defPos >= 0) {
                    commenceInnerSelection(defPos);
                }
            }
        }

        @Override
        public void onNoDataLoaded() {
            refCurVal = null;
        }

        void checkRefreshOnce() {
            if (!requestedRefreshOnce) {
                requestedRefreshOnce = true;
            }
        }

        private void commenceInnerSelection(int pos) {
            innerSelectionCommencing = true;
            final DecoratingAdapter decorator = RefreshingSpinner.this.decorator;
            setSelection(decorator != null ? decorator.decoratedPositionToDecorators(pos) : pos);
        }

    }

    private static final class InnerRestoreController implements PersistingStateful.OnRestoreListener {

        private final RefreshingSpinner spinner;
        private boolean restored = false;
        private int restoredSelection = -1;
        private boolean restoreCommencing = false;

        boolean restoredSelectionCommencing = false;

        InnerRestoreController(RefreshingSpinner spinner) {
            this.spinner = spinner;
        }

        @Override
        public void onRestoredState() {
            if (restoreCommencing) {
                try {
                    checkRestoredSelection();
                } finally {
                    restoreCommencing = false;
                }
            }
        }

        void commenceRestore(SpinnerAdapter adapter, SavedState savedState) {
            if (adapter instanceof PersistingStateful && savedState.adapterState != null) {
                restoreCommencing = true;
                ((PersistingStateful) adapter).restoreSavedState(savedState.adapterState);
            } else {
                checkRestoredSelection();
            }
        }

        void setRestoredSelection(int position) {
            if (restored) {
                spinner.setSelection(position);
            } else {
                restoredSelection = position;
            }
        }

        private void checkRestoredSelection() {
            if (restoredSelection >= 0) {
                restoredSelectionCommencing = true;
                spinner.setSelection(restoredSelection);
                restoredSelection = -1;
            }
            restored = true;
        }

    }

    private final class InnerSelectionListener extends EmptyOnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (refreshingAdapter != null) {
                final InnerRefreshController rC = RefreshingSpinner.this.onRefreshController;
                final InnerRestoreController recCon = RefreshingSpinner.this.restoreController;

                rC.checkRefreshOnce();

                if (!rC.innerSelectionCommencing && !recCon.restoredSelectionCommencing) {
                    if (decorator != null) {
                        if (decorator.isPositionTranslatable(position)) {
                            //noinspection unchecked
                            if (!refreshingAdapter.refresh(decorator.getItemTranslating(position))) {
                                makeFailToast();
                            }
                        }
                    } else {
                        //noinspection unchecked
                        if (!refreshingAdapter.refresh(refreshingAdapter.getItem(position))) {
                            makeFailToast();
                        }
                    }
                } else {
                    if (rC.innerSelectionCommencing) {
                        rC.innerSelectionCommencing = false;
                    } else {
                        recCon.restoredSelectionCommencing = false;
                    }
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
