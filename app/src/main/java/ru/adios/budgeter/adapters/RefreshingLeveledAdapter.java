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
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

import ru.adios.budgeter.R;

/**
 * Created by Michail Kulikov
 * 11/27/15
 */
@UiThread
public final class RefreshingLeveledAdapter<DataType> extends RefreshingAdapter<DataType, DataType> {

    public interface ParentExtractor<T> {
        @Nullable
        T extractParent(T data);
    }

    private final ParentExtractor<DataType> parentExtractor;
    private DataType upLevel;
    private StringPresenter<DataType> innerPresenter;

    public RefreshingLeveledAdapter(Context context, Refresher<DataType, DataType> refresher, ParentExtractor<DataType> pe, @LayoutRes int resource) {
        super(context, refresher, resource);
        this.parentExtractor = pe;
        setStringPresenter(new WrappingPresenter());
    }

    public RefreshingLeveledAdapter(Context context, Refresher<DataType, DataType> refresher, ParentExtractor<DataType> pe, @LayoutRes int resource, @IdRes int fieldId) {
        super(context, refresher, resource, fieldId);
        this.parentExtractor = pe;
        setStringPresenter(new WrappingPresenter());
    }

    public void init() {
        refresh(null);
    }

    @Override
    public void refresh(@Nullable DataType dataType) {
        upLevel = (dataType == upLevel)
                ? parentExtractor.extractParent(dataType)
                : dataType;
        super.refresh(upLevel);
    }

    @Override
    public void setStringPresenter(StringPresenter<DataType> stringPresenter) {
        innerPresenter = stringPresenter;
    }

    @Override
    public int getCount() {
        int size = super.getCount();
        if (upLevel != null) {
            size++;
        }
        return size;
    }

    @Override
    public DataType getItem(int position) {
        if (upLevel != null) {
            if (position == 0) {
                return upLevel;
            }
            return super.getItem(position - 1);
        }
        return super.getItem(position);
    }

    private final class WrappingPresenter extends UnchangingStringPresenter<DataType> {
        @Override
        public String getStringPresentation(DataType item) {
            if (item == upLevel) {
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

}
