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

package ru.adios.budgeter.util.concurrent;

import android.os.AsyncTask;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;

import java8.util.function.Supplier;

/**
 * Created by Michail Kulikov
 * 12/1/15
 */
public final class AsyncTaskProvider implements AsynchronyProvider {

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public <V> ListenableFuture<V> provideAsynchrony(Supplier<V> supplier) {
        final AsyncListenableFuture<V> future = new AsyncListenableFuture<>();
        new AsyncTask<Supplier, Void, ResultContainer<V>>() {
            @Override
            protected ResultContainer<V> doInBackground(Supplier... params) {
                V res = null;
                Throwable th = null;
                try {
                    //noinspection unchecked
                    res = ((Supplier<V>) params[0]).get();
                } catch (Throwable e) {
                    th = e;
                }
                return new ResultContainer<>(res, th);
            }

            @Override
            protected void onPostExecute(ResultContainer<V> vResultContainer) {
                vResultContainer.enrichFuture(future);
            }
        }.execute(supplier);
        return future;
    }

    private static final class ResultContainer<V> {

        private final V res;
        private final Throwable th;

        ResultContainer(V res, Throwable th) {
            this.res = res;
            this.th = th;
        }

        void enrichFuture(AsyncListenableFuture<V> future) {
            if (th != null) {
                future.setTh(th);
            } else {
                future.setRes(res);
            }
        }

    }

    private static final class AsyncListenableFuture<V> extends AbstractFuture<V> {

        void setTh(Throwable th) {
            setException(th);
        }

        void setRes(V res) {
            set(res);
        }

    }

}
