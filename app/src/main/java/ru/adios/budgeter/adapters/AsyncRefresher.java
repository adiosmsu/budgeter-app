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

import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import java8.util.function.Supplier;
import ru.adios.budgeter.util.concurrent.AsyncTaskProvider;

/**
 * Created by Michail Kulikov
 * 11/27/15
 */
public abstract class AsyncRefresher<T, P> implements RefreshingAdapter.Refresher<T, P> {

    private final AsyncTaskProvider asyncTaskProvider = new AsyncTaskProvider();

    @WorkerThread
    public abstract ImmutableList<T> gatherData(@Nullable P param);

    @Override
    public final boolean isAsync() {
        return true;
    }

    @Override
    public final <V> ListenableFuture<V> provideAsynchrony(Supplier<V> supplier) {
        return asyncTaskProvider.provideAsynchrony(supplier);
    }

}
