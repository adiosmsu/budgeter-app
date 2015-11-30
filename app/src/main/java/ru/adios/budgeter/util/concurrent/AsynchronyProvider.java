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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.concurrent.ThreadSafe;

import java8.util.function.Consumer;
import java8.util.function.Supplier;

/**
 * Created by Michail Kulikov
 * 12/1/15
 */
@ThreadSafe
public interface AsynchronyProvider {

    final class Static {

        public static <V> void workWithProvider(AsynchronyProvider p, final Consumer<V> onSuccess, @Nullable final Consumer<Throwable> onFail, Supplier<V> supplier) {
            if (p.isAsync()) {
                Futures.addCallback(
                        p.provideAsynchrony(supplier),
                        new FutureCallback<V>() {
                            @Override
                            public void onSuccess(V result) {
                                onSuccess.accept(result);
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                if (onFail != null) {
                                    onFail.accept(t);
                                } else {
                                    t.printStackTrace();
                                }
                            }
                        }
                );
            } else {
                onSuccess.accept(supplier.get());
            }
        }

        private Static() {}

    }

    <V> ListenableFuture<V> provideAsynchrony(Supplier<V> supplier);

    boolean isAsync();

}
