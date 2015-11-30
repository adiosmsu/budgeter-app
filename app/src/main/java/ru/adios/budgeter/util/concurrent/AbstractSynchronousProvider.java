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

import com.google.common.util.concurrent.ListenableFuture;

import java8.util.function.Supplier;

/**
 * Created by Michail Kulikov
 * 12/1/15
 */
public abstract class AbstractSynchronousProvider implements AsynchronyProvider {

    @Override
    public final boolean isAsync() {
        return false;
    }

    @Override
    public final <V> ListenableFuture<V> provideAsynchrony(Supplier<V> supplier) {
        throw new UnsupportedOperationException("This is synchronous implementation");
    }

}
