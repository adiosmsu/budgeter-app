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
import java.util.LinkedList;

import javax.annotation.concurrent.Immutable;

import java8.util.function.Function;
import java8.util.function.Supplier;
import ru.adios.budgeter.R;

/**
 * Created by Michail Kulikov
 * 11/27/15
 */
@UiThread
public final class RefreshingLeveledAdapter<DataType, IdType extends Serializable> extends RefreshingAdapter<DataType, DataType, IdType> {

    static final Logger logger = LoggerFactory.getLogger(RefreshingLeveledAdapter.class);

    final ParentingDataExtractor<DataType, IdType> parentingDataExtractor;

    private IdType potentialUpLevelId;
    private DataType potentialUpLevelData;
    private boolean potentialUpNulling = false;
    private int potentialUpLevelPosition = -1;
    private boolean upPressed = false;

    boolean refreshCommencing = false;

    private final WrappingPresenter innerPresenter;
    private final LeveledRefreshListener innerRefreshListener;
    private final LeveledRestoreListener innerRestoreListener;

    // begin transient state
    private final LinkedList<Integer> upLevelPositionsStack = new LinkedList<>();
    private IdType upLevelId;
    private DataType upLevelData;
    private Integer defaultPosition = null;
    // end transient state

    public RefreshingLeveledAdapter(Context context, Refresher<DataType, DataType> refresher, ParentingDataExtractor<DataType, IdType> pe, @LayoutRes int resource) {
        super(context, refresher, pe, resource);
        this.parentingDataExtractor = pe;
        innerPresenter = new WrappingPresenter();
        innerRefreshListener = new LeveledRefreshListener();
        innerRestoreListener = new LeveledRestoreListener();
        innerInit();
    }

    public RefreshingLeveledAdapter(Context context, Refresher<DataType, DataType> refresher, ParentingDataExtractor<DataType, IdType> pe, @LayoutRes int resource, @IdRes int fieldId) {
        super(context, refresher, pe, resource, fieldId);
        this.parentingDataExtractor = pe;
        innerPresenter = new WrappingPresenter();
        innerRefreshListener = new LeveledRefreshListener();
        innerRestoreListener = new LeveledRestoreListener();
        innerInit();
    }

    private void innerInit() {
        super.setStringPresenter(innerPresenter);
        super.setOnRefreshListener(innerRefreshListener);
        super.setOnRestoreListener(innerRestoreListener);
    }

    public void initDoNotCallIfActivityRestored() {
        if (!refreshCommencing) {
            refreshCommencing = true;
            handleNewUpLevel(null, null, true);
        }
    }

    @Override
    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        innerRefreshListener.refListener = onRefreshListener;
    }

    @Override
    public void removeOnRefreshListener(OnRefreshListener onRefreshListener) {
        if (innerRefreshListener.refListener != null && innerRefreshListener.refListener.equals(onRefreshListener)) {
            innerRefreshListener.refListener = null;
        }
    }

    @Override
    public void setOnRestoreListener(OnRestoreListener onRestoreListener) {
        innerRestoreListener.resListener = onRestoreListener;
    }

    @Override
    public void removeOnRestoreListener(OnRestoreListener onRestoreListener) {
        if (innerRestoreListener.resListener != null && innerRestoreListener.resListener.equals(onRestoreListener)) {
            innerRestoreListener.resListener = null;
        }
    }

    @Override
    public int getDefaultPosition() {
        return defaultPosition != null ? defaultPosition : -1;
    }

    @Override
    public SavedState getSavedState() {
        return new SavedState(super.getSavedState(), upLevelId, upLevelPositionsStack, defaultPosition);
    }

    @Override
    public void restoreSavedState(Parcelable state) {
        final SavedState savedState = (SavedState) state;
        super.restoreSavedState(savedState.getSuperState());

        //noinspection unchecked
        upLevelId = (IdType) savedState.upLevelId;

        if (upLevelId == null) {
            finishRestore(savedState, null);
        } else {
            if (parentingDataExtractor.isAsync()) {
                refreshCommencing = true;

                Futures.addCallback(
                        parentingDataExtractor.provideAsynchrony(new Supplier<DataType>() {
                            @Override
                            public DataType get() {
                                return parentingDataExtractor.extractData(upLevelId);
                            }
                        }),
                        new AbsFutureCallback<DataType>() {
                            @Override
                            public void onSuccess(DataType result) {
                                finishRestore(savedState, result);
                                refreshCommencing = false;
                            }
                        }
                );
            } else {
                finishRestore(savedState, parentingDataExtractor.extractData(upLevelId));
            }
        }
    }

    void finishRestore(SavedState savedState, DataType ulData) {
        upLevelData = ulData;
        upLevelPositionsStack.clear();
        for (final Object o : savedState.stackContents) {
            upLevelPositionsStack.add((Integer) o);
        }
        defaultPosition = savedState.defaultPosition >= 0 ? savedState.defaultPosition : null;
        innerRestoreListener.onRestoredState();
    }

    @Override
    public boolean refresh(DataType data) {
        if (!refreshCommencing) {
            refreshCommencing = true;

            final IdType id = parentingDataExtractor.extractId(data);

            if (upLevelId != null && upLevelId.equals(id)) {
                upPressed = true;
                refreshUsingParentOf(upLevelId);
            } else {
                int i = 0;
                for (final DataType innerItem : innerList()) {
                    if (innerItem.equals(data)) {
                        potentialUpLevelPosition = i;
                        break;
                    }
                    i++;
                }
                refreshUsingData(id, data);
            }

            return true;
        }

        return false;
    }

    private void refreshUsingParentOf(final IdType id) {
        refreshInnerGeneral(id, new Function<IdType, IdentifiedData<DataType, IdType>>() {
            @Override
            public IdentifiedData<DataType, IdType> apply(IdType i) {
                return parentingDataExtractor.extractParent(i);
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
                    return new IdentifiedData<>(parentingDataExtractor.extractData(i), i);
                }
            });
        }
    }

    private void refreshInnerGeneral(final IdType id, final Function<IdType, IdentifiedData<DataType, IdType>> f) {
        if (parentingDataExtractor.isAsync()) {
            if (refresher.isAsync()) {
                Futures.addCallback(
                        parentingDataExtractor.provideAsynchrony(new Supplier<AsyncResult<DataType, IdType>>() {
                            @Override
                            public AsyncResult<DataType, IdType> get() {
                                final IdentifiedData<DataType, IdType> data = f.apply(id);
                                return new AsyncResult<>(data, data != null ? refresher.gatherData(data.data) : refresher.gatherData(null));
                            }
                        }),
                        new AbsFutureCallback<AsyncResult<DataType, IdType>>() {
                            @Override
                            public void onSuccess(AsyncResult<DataType, IdType> result) {
                                handleNewUpLevel(result.identifiedData, false);
                                processRefreshResult(result.refreshed, result.identifiedData == null ? null : result.identifiedData.data);
                            }
                        }
                );
            } else {
                Futures.addCallback(
                        parentingDataExtractor.provideAsynchrony(new Supplier<IdentifiedData<DataType, IdType>>() {
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
        if (newUpLevelData == null && newUpLevelId == null) {
            potentialUpNulling = true;
        }

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
        innerPresenter.presenter = stringPresenter;
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

        StringPresenter<DataType> presenter;

        @Override
        public String getStringPresentation(DataType item) {
            String presentation = (presenter != null)
                    ? presenter.getStringPresentation(item)
                    : item instanceof String ? (String) item : item.toString();

            if (item.equals(upLevelData)) {
                presentation = context.getResources().getString(R.string.refreshing_leveled_adapter_up_str) + " (" + presentation + ')';
            }

            return presentation;
        }
    }

    private final class LeveledRefreshListener implements OnRefreshListener {

        OnRefreshListener refListener;

        @Override
        public void onNoDataLoaded() {
            refreshCommencing = false;

            final DataType dataBackup = potentialUpLevelData;
            if (potentialUpLevelId != null && potentialUpLevelData != null) {
                potentialUpLevelId = null;
                potentialUpLevelData = null;
            }
            if (potentialUpNulling) {
                potentialUpNulling = false;
            }
            if (potentialUpLevelPosition >= 0) {
                if (dataBackup != null) {
                    int i = 0;
                    for (final DataType innerItem : innerList()) {
                        if (innerItem.equals(dataBackup)) {
                            defaultPosition = upLevelId != null
                                    ? i + 1
                                    : i;
                            break;
                        }
                        i++;
                    }
                }
                potentialUpLevelPosition = -1;
            }
            if (upPressed) {
                upPressed = false;
            }

            if (refListener != null) {
                refListener.onNoDataLoaded();
            }
        }

        @Override
        public void onRefreshed() {
            refreshCommencing = false;

            if ((potentialUpLevelId != null && potentialUpLevelData != null) || potentialUpNulling) {
                upLevelId = potentialUpLevelId;
                upLevelData = potentialUpLevelData;
                potentialUpLevelId = null;
                potentialUpLevelData = null;
                potentialUpNulling = false;
            }
            if (potentialUpLevelPosition >= 0) {
                defaultPosition = 0;
                upLevelPositionsStack.push(potentialUpLevelPosition);
                potentialUpLevelPosition = -1;
            }
            if (upPressed) {
                defaultPosition = upLevelPositionsStack.poll();
                upPressed = false;
            }

            if (refListener != null) {
                refListener.onRefreshed();
            }
        }

    }

    private static final class LeveledRestoreListener implements OnRestoreListener {

        OnRestoreListener resListener;
        private short restoreCounter = 0;

        @Override
        public void onRestoredState() {
            restoreCounter++;
            if (restoreCounter >= 2) {
                if (resListener != null) {
                    resListener.onRestoredState();
                }
                restoreCounter = 0;
            }
        }
    }

    public static class SavedState extends RefreshingState {

        final Serializable upLevelId;
        final Object[] stackContents;
        final int defaultPosition;

        SavedState(RefreshingState superState, Serializable upLevelId, LinkedList<Integer> stack, Integer defaultPosition) {
            super(superState);
            this.upLevelId = upLevelId;
            this.stackContents = stack.toArray();
            this.defaultPosition = defaultPosition != null ? defaultPosition : -1;
        }

        SavedState(Parcel in) {
            super(in);
            upLevelId = in.readSerializable();
            stackContents = in.readArray(null);
            defaultPosition = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel destination, int flags) {
            super.writeToParcel(destination, flags);
            destination.writeSerializable(upLevelId);
            destination.writeArray(stackContents);
            destination.writeInt(defaultPosition);
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
