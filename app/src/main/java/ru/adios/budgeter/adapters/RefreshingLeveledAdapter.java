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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import java8.util.function.Function;
import java8.util.function.Supplier;
import ru.adios.budgeter.R;
import ru.adios.budgeter.util.concurrent.AsynchronyProvider;

/**
 * Created by Michail Kulikov
 * 11/27/15
 */
@UiThread
public final class RefreshingLeveledAdapter<DataType, IdType extends Serializable> extends RefreshingAdapter<DataType, DataType> {

    private static final Logger logger = LoggerFactory.getLogger(RefreshingLeveledAdapter.class);

    @ThreadSafe
    public interface DataExtractor<T, I extends Serializable> extends AsynchronyProvider {

        @Nullable
        IdentifiedData<T, I> extractParent(I id);

        T extractData(I id);

        @UiThread
        I extractId(T data);

    }

    @Immutable
    public static final class IdentifiedData<T, I extends Serializable> {
        final T data;
        final I id;

        public IdentifiedData(T data, I id) {
            this.data = data;
            this.id = id;
        }
    }


    private final DataExtractor<DataType, IdType> dataExtractor;
    private IdType upLevelId;
    private DataType upLevelData;
    private IdType potentialUpLevelId;
    private DataType potentialUpLevelData;
    private StringPresenter<DataType> innerPresenter;
    private OnRefreshListener innerRefListener;

    public RefreshingLeveledAdapter(Context context, Refresher<DataType, DataType> refresher, DataExtractor<DataType, IdType> pe, @LayoutRes int resource) {
        super(context, refresher, resource);
        this.dataExtractor = pe;
        innerInit();
    }

    public RefreshingLeveledAdapter(Context context, Refresher<DataType, DataType> refresher, DataExtractor<DataType, IdType> pe, @LayoutRes int resource, @IdRes int fieldId) {
        super(context, refresher, resource, fieldId);
        this.dataExtractor = pe;
        innerInit();
    }

    private void innerInit() {
        super.setStringPresenter(new WrappingPresenter());
        super.setOnRefreshListener(new LeveledRefreshListener());
    }

    public void init() {
        handleNewUpLevel(null, null, true);
    }

    @Override
    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.innerRefListener = onRefreshListener;
    }

    @Override
    public Parcelable getSavedState() {
        Parcelable superState = super.getSavedState();
        return new SavedState(superState, upLevelId);
    }

    @Override
    public void restoreSavedState(Parcelable state) {
        final SavedState savedState = (SavedState) state;
        super.restoreSavedState(savedState.getSuperState());
        //noinspection unchecked
        refreshUsingData((IdType) savedState.upLevelId, null);
    }

    @Override
    public void refresh(DataType data) {
        final IdType id = dataExtractor.extractId(data);

        if (upLevelId != null && upLevelId.equals(id)) {
            refreshUsingParentOf(upLevelId);
        } else {
            refreshUsingData(id, data);
        }
    }

    private void refreshUsingParentOf(final IdType id) {
        refreshInnerGeneral(id, new Function<IdType, IdentifiedData<DataType, IdType>>() {
            @Override
            public IdentifiedData<DataType, IdType> apply(IdType i) {
                return dataExtractor.extractParent(i);
            }
        });
    }

    private void refreshUsingData(IdType id, @Nullable DataType data) {
        if (data != null) {
            handleNewUpLevel(id, data, true);
        } else {
            refreshInnerGeneral(id, new Function<IdType, IdentifiedData<DataType, IdType>>() {
                @Override
                public IdentifiedData<DataType, IdType> apply(IdType i) {
                    return new IdentifiedData<>(dataExtractor.extractData(i), i);
                }
            });
        }
    }

    private void refreshInnerGeneral(final IdType id, final Function<IdType, IdentifiedData<DataType, IdType>> f) {
        if (dataExtractor.isAsync()) {
            if (refresher.isAsync()) {
                Futures.addCallback(
                        dataExtractor.provideAsynchrony(new Supplier<AsyncResult<DataType, IdType>>() {
                            @Override
                            public AsyncResult<DataType, IdType> get() {
                                final IdentifiedData<DataType, IdType> data = f.apply(id);
                                return new AsyncResult<>(data, refresher.gatherData(data.data));
                            }
                        }),
                        new AbsFutureCallback<AsyncResult<DataType, IdType>>() {
                            @Override
                            public void onSuccess(AsyncResult<DataType, IdType> result) {
                                handleNewUpLevel(result.identifiedData, false);
                                setItems(result.refreshed);
                            }
                        }
                );
            } else {
                Futures.addCallback(
                        dataExtractor.provideAsynchrony(new Supplier<IdentifiedData<DataType, IdType>>() {
                            @Override
                            public IdentifiedData<DataType, IdType> get() {
                                return f.apply(id);
                            }
                        }),
                        new AbsFutureCallback<IdentifiedData<DataType, IdType>>() {
                            @Override
                            public void onSuccess(@Nullable IdentifiedData<DataType, IdType> result) {
                                handleNewUpLevel(result, true);
                            }
                        }
                );
            }
        } else {
            handleNewUpLevel(f.apply(id), true);
        }
    }

    void handleNewUpLevel(@Nullable IdType newUpLevelId, @Nullable DataType newUpLevelData, boolean doRefresh) {
        potentialUpLevelId = newUpLevelId;
        potentialUpLevelData = newUpLevelData;

        if (doRefresh) {
            super.refresh(newUpLevelData);
        }
    }

    void handleNewUpLevel(@Nullable IdentifiedData<DataType, IdType> identifiedData, boolean doRefresh) {
        if (identifiedData == null) {
            handleNewUpLevel(null, null, doRefresh);
        } else {
            handleNewUpLevel(identifiedData.id, identifiedData.data, doRefresh);
        }
    }

    @Override
    public void setStringPresenter(StringPresenter<DataType> stringPresenter) {
        innerPresenter = stringPresenter;
    }

    @Override
    public int getCount() {
        int size = super.getCount();
        if (upLevelId != null) {
            size++;
        }
        return size;
    }

    @Override
    public DataType getItem(int position) {
        if (upLevelId != null) {
            if (position == 0) {
                return upLevelData;
            }
            return super.getItem(position - 1);
        }
        return super.getItem(position);
    }

    @Immutable
    private static final class AsyncResult<T, I extends Serializable> {
        @Nullable final IdentifiedData<T, I> identifiedData;
        @Nullable final ImmutableList<T> refreshed;

        AsyncResult(@Nullable IdentifiedData<T, I> identifiedData, @Nullable ImmutableList<T> refreshed) {
            this.identifiedData = identifiedData;
            this.refreshed = refreshed;
        }
    }

    private static abstract class AbsFutureCallback<V> implements FutureCallback<V> {
        @Override
        public final void onFailure(@NonNull Throwable t) {
            logger.error("Error extracting data for RefreshingLeveledAdapter", t);
        }
    }

    private final class WrappingPresenter extends UnchangingStringPresenter<DataType> {
        @Override
        public String getStringPresentation(DataType item) {
            if (item == upLevelId) {
                return context.getResources().getString(R.string.refreshing_leveled_adapter_up_str);
            }
            if (innerPresenter != null) {
                return innerPresenter.getStringPresentation(item);
            }
            return item instanceof String
                    ? (String) item
                    : item.toString();
        }
    }

    private final class LeveledRefreshListener implements OnRefreshListener {
        @Override
        public void onNoDataLoaded() {
            if (potentialUpLevelId != null && potentialUpLevelData != null) {
                potentialUpLevelId = null;
                potentialUpLevelData = null;
            }

            if (innerRefListener != null) {
                innerRefListener.onNoDataLoaded();
            }
        }

        @Override
        public void onRefreshed() {
            if (potentialUpLevelId != null && potentialUpLevelData != null) {
                upLevelId = potentialUpLevelId;
                upLevelData = potentialUpLevelData;
                potentialUpLevelId = null;
                potentialUpLevelData = null;
            }

            if (innerRefListener != null) {
                innerRefListener.onRefreshed();
            }
        }
    }

    public static class SavedState extends RefreshingState {

        final Serializable upLevelId;

        SavedState(Parcelable superState, Serializable upLevelId) {
            super(superState);
            this.upLevelId = upLevelId;
        }

        SavedState(Parcel in) {
            super(in);
            upLevelId = in.readSerializable();
        }

        @Override
        public void writeToParcel(Parcel destination, int flags) {
            super.writeToParcel(destination, flags);
            destination.writeSerializable(upLevelId);
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
