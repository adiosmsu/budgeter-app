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

import java.io.Serializable;

import javax.annotation.concurrent.ThreadSafe;

import java8.util.function.Consumer;
import java8.util.function.Supplier;
import ru.adios.budgeter.util.concurrent.AsynchronyProvider;

/**
 * Created by Michail Kulikov
 * 11/27/15
 */
@UiThread
public class RefreshingAdapter<Type, Param, I extends Serializable> extends ViewProvidingBaseAdapter<Type> implements PersistingStateful {

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
    private final DataExtractor<Param, I> dataExtractor;
    private OnRefreshListener onRefreshListener;
    private ImmutableList<Type> items = ImmutableList.of();
    private Param currentParam;
    private boolean refreshCommencing = false;
    private Consumer<Throwable> onFailConsumer = new Consumer<Throwable>() {
        @Override
        public void accept(Throwable throwable) {
            logRefreshThrowable(throwable);
        }
    };

    public RefreshingAdapter(Context context, Refresher<Type, Param> refresher, DataExtractor<Param, I> de, @LayoutRes int resource) {
        super(context, resource);
        this.refresher = refresher;
        this.dataExtractor = de;
    }

    public RefreshingAdapter(Context context, Refresher<Type, Param> refresher, DataExtractor<Param, I> de, @LayoutRes int resource, @IdRes int fieldId) {
        super(context, resource, fieldId);
        this.refresher = refresher;
        this.dataExtractor = de;
    }

    @Override
    public RefreshingState getSavedState() {
        return new RefreshingState(RefreshingState.EMPTY_STATE, currentParam != null ? dataExtractor.extractId(currentParam) : null);
    }

    @Override
    public void restoreSavedState(Parcelable state) {
        if (!(state instanceof RefreshingState)) {
            throw new IllegalArgumentException("Wrong state class, expecting RefreshingState but "
                    + "received " + state.getClass().toString() + " instead.");
        }

        final Serializable currentId = ((RefreshingState) state).currentId;
        if (currentId != null) {
            if (dataExtractor.isAsync()) {
                refreshCommencing = true;

                if (refresher.isAsync()) {
                    AsynchronyProvider.Static.workWithProvider(
                            dataExtractor,
                            new Consumer<AsyncRestoreResult<Type, Param>>() {
                                @Override
                                public void accept(AsyncRestoreResult<Type, Param> result) {
                                    processRefreshResult(result.list, result.param);
                                }
                            },
                            onFailConsumer,
                            new Supplier<AsyncRestoreResult<Type, Param>>() {
                                @Override
                                public AsyncRestoreResult<Type, Param> get() {
                                    //noinspection unchecked
                                    final Param param = dataExtractor.extractData((I) currentId);
                                    return new AsyncRestoreResult<>(refresher.gatherData(param), param);
                                }
                            }
                    );
                } else {
                    AsynchronyProvider.Static.workWithProvider(
                            dataExtractor,
                            new Consumer<Param>() {
                                @Override
                                public void accept(Param param) {
                                    processRefreshResult(refresher.gatherData(param), param);
                                }
                            },
                            onFailConsumer,
                            new Supplier<Param>() {
                                @Override
                                public Param get() {
                                    //noinspection unchecked
                                    return dataExtractor.extractData((I) currentId);
                                }
                            }
                    );
                }
            } else {
                //noinspection unchecked
                refreshInner(dataExtractor.extractData((I) currentId));
            }
        } else {
            refreshInner(null);
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
                            processRefreshResult(data, param);
                        }
                    },
                    onFailConsumer,
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

    void processRefreshResult(ImmutableList<Type> data, @Nullable Param param) {
        refreshCommencing = false;
        if (data != null && data.size() > 0) {
            currentParam = param;
        }
        setItems(data);
    }

    void logRefreshThrowable(Throwable throwable) {
        logger.error("Error gathering data for RefreshingAdapter through " + refresher, throwable);
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

    private void setItems(@Nullable ImmutableList<Type> items) {
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

    private static final class AsyncRestoreResult<T, P> {
        final P param;
        final ImmutableList<T> list;

        AsyncRestoreResult(ImmutableList<T> list, P param) {
            this.list = list;
            this.param = param;
        }
    }

    public static class RefreshingState extends AbsSavedState {

        final Serializable currentId;

        RefreshingState(Parcel source) {
            super(source);
            currentId = source.readSerializable();
        }

        RefreshingState(Parcelable superState, Serializable currentId) {
            super(superState);
            this.currentId = currentId;
        }

        RefreshingState(RefreshingState superState) {
            super(superState);
            currentId = superState.currentId;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeSerializable(currentId);
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
