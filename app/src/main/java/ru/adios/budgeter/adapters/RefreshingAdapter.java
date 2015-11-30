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

package ru.adios.budgeter.adapters;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.view.AbsSavedState;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by Michail Kulikov
 * 11/27/15
 */
@UiThread
public class RefreshingAdapter<Type, Param> extends ViewProvidingBaseAdapter<Type> implements PersistingStateful {

    public static abstract class Refresher<T, P> {

        protected final RefreshingAdapter<T, P> adapter;

        public Refresher(RefreshingAdapter<T, P> adapter) {
            this.adapter = adapter;
        }

        /**
         * Must call {@link #processGathered(ImmutableList)} at the end.
         * @param param typed parameter.
         */
        @UiThread
        public abstract void gatherData(@Nullable P param);

        @UiThread
        public final void processGathered(ImmutableList<T> data) {
            adapter.setItems(data);
            adapter.notifyDataSetChanged();
        }

        @UiThread
        public final void processNoData() {
            adapter.processNoData();
        }

    }

    public interface OnRefreshListener {

        void onRefreshed();

        void onNoDataLoaded();

    }

    protected final Refresher<Type, Param> refresher;
    private OnRefreshListener onRefreshListener;
    private ImmutableList<Type> items = ImmutableList.of();

    public RefreshingAdapter(Context context, Refresher<Type, Param> refresher, @LayoutRes int resource) {
        super(context, resource);
        this.refresher = refresher;
        assertRefresherIsOurs(refresher);
    }

    public RefreshingAdapter(Context context, Refresher<Type, Param> refresher, @LayoutRes int resource, @IdRes int fieldId) {
        super(context, resource, fieldId);
        this.refresher = refresher;
        assertRefresherIsOurs(refresher);
    }

    private void assertRefresherIsOurs(Refresher<Type, Param> refresher) {
        checkArgument(refresher.adapter == this, "Expecting Refresher instantiated with instance of this class");
    }

    @Override
    public Parcelable getSavedState() {
        return RefreshingState.EMPTY_STATE;
    }

    @Override
    public void restoreSavedState(Parcelable state) {
        if (state != null && !(state instanceof AbsSavedState)) {
            throw new IllegalArgumentException("Wrong state class, expecting RefreshingState but "
                    + "received " + state.getClass().toString() + " instead.");
        }
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    public void refresh(@Nullable Param param) {
        refresher.gatherData(param);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Type getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    void setItems(ImmutableList<Type> items) {
        this.items = items;
        if (onRefreshListener != null) {
            onRefreshListener.onRefreshed();
        }
    }

    void processNoData() {
        if (onRefreshListener != null) {
            onRefreshListener.onNoDataLoaded();
        }
    }

    public static class RefreshingState extends AbsSavedState {
        RefreshingState(Parcel source) {
            super(source);
        }

        RefreshingState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<RefreshingState> CREATOR =
                new Parcelable.Creator<RefreshingState>() {
                    public RefreshingState createFromParcel(Parcel in) {
                        return new RefreshingState(in);
                    }

                    public RefreshingState[] newArray(int size) {
                        return new RefreshingState[size];
                    }
                };
    }

}
