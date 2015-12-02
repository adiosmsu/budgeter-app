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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;

import java8.util.function.Consumer;
import java8.util.function.Supplier;
import ru.adios.budgeter.util.concurrent.AsynchronyProvider;

/**
 * Created by Michail Kulikov
 * 11/27/15
 */
@UiThread
public class RefreshingAdapter<Type, Param> extends ViewProvidingBaseAdapter<Type> implements PersistingStateful {

    static final Logger logger = LoggerFactory.getLogger(RefreshingAdapter.class);

    @ThreadSafe
    public interface Refresher<T, P> extends AsynchronyProvider {

        @Nullable
        ImmutableList<T> gatherData(@Nullable P param);

    }

    public interface OnRefreshListener {

        void onRefreshed();

        void onNoDataLoaded();

    }

    protected final Refresher<Type, Param> refresher;
    private OnRefreshListener onRefreshListener;
    private ImmutableList<Type> items = ImmutableList.of();
    private Param currentParam;
    private boolean refreshCommencing = false;

    public RefreshingAdapter(Context context, Refresher<Type, Param> refresher, @LayoutRes int resource) {
        super(context, resource);
        this.refresher = refresher;
    }

    public RefreshingAdapter(Context context, Refresher<Type, Param> refresher, @LayoutRes int resource, @IdRes int fieldId) {
        super(context, resource, fieldId);
        this.refresher = refresher;
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

    public void removeOnRefreshListener(OnRefreshListener onRefreshListener) {
        if (this.onRefreshListener != null && this.onRefreshListener.equals(onRefreshListener)) {
            this.onRefreshListener = null;
        }
    }

    public int getDefaultPosition() {
        return 0;
    }

    public boolean refresh(@Nullable final Param param) {
        return refreshInner(param);
    }

    private boolean refreshInner(@Nullable final Param param) {
        if (!refreshCommencing) {
            refreshCommencing = true;

            AsynchronyProvider.Static.workWithProvider(
                    refresher,
                    new Consumer<ImmutableList<Type>>() {
                        @Override
                        public void accept(ImmutableList<Type> data) {
                            refreshCommencing = false;
                            if (data != null && data.size() > 0) {
                                currentParam = param;
                            }
                            setItems(data);
                        }
                    },
                    new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            logger.error("Error gathering data for RefreshingAdapter through " + refresher, throwable);
                        }
                    },
                    new Supplier<ImmutableList<Type>>() {
                        @Override
                        public ImmutableList<Type> get() {
                            return refresher.gatherData(param);
                        }
                    }
            );

            return true;
        }

        return false;
    }

    public boolean refreshCurrent() {
        return refreshInner(currentParam);
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

    protected final ImmutableList<Type> innerList() {
        return items;
    }

    void setItems(@Nullable ImmutableList<Type> items) {
        if (items != null && items.size() > 0) {
            this.items = items;
            if (onRefreshListener != null) {
                onRefreshListener.onRefreshed();
            }
            notifyDataSetChanged();
        } else {
            if (onRefreshListener != null) {
                onRefreshListener.onNoDataLoaded();
            }
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
