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

import com.google.common.collect.ImmutableList;

/**
 * Created by Michail Kulikov
 * 11/27/15
 */
@UiThread
public class RefreshingAdapter<Type, Param> extends ViewProvidingBaseAdapter<Type> {

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

    }

    private final Refresher<Type, Param> refresher;
    private Runnable onRefreshListener;
    private ImmutableList<Type> items = ImmutableList.of();

    public RefreshingAdapter(Context context, Refresher<Type, Param> refresher, @LayoutRes int resource) {
        super(context, resource);
        this.refresher = refresher;
    }

    public RefreshingAdapter(Context context, Refresher<Type, Param> refresher, @LayoutRes int resource, @IdRes int fieldId) {
        super(context, resource, fieldId);
        this.refresher = refresher;
    }

    public void setOnRefreshListener(Runnable onRefreshListener) {
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
            onRefreshListener.run();
        }
    }

}
