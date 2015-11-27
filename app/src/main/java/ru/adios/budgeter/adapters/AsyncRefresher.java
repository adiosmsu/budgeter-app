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

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;

/**
 * Created by Michail Kulikov
 * 11/27/15
 */
public abstract class AsyncRefresher<T, P> extends RefreshingAdapter.Refresher<T, P> {

    public AsyncRefresher(RefreshingAdapter<T, P> adapter) {
        super(adapter);
    }

    @Override
    @UiThread
    public final void gatherData(@Nullable P param) {
        new AsyncTask<Object, Void, ImmutableList<T>>() {
            @Override
            protected ImmutableList<T> doInBackground(Object... params) {
                //noinspection unchecked
                final P par = (P) params[0];
                return gatherDataAsync(par);
            }

            @Override
            protected void onPostExecute(ImmutableList<T> ts) {
                if (ts != null) {
                    processGathered(ts);
                }
            }
        }.execute(param);
    }

    @WorkerThread
    public abstract ImmutableList<T> gatherDataAsync(@Nullable P param);

}
