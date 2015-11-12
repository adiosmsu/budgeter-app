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

package ru.adios.budgeter.widgets;

import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import java8.util.Optional;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import ru.adios.budgeter.api.RepoOption;

/**
 * Created by Michail Kulikov
 * 11/7/15
 */
@Immutable
public abstract class AbstractDataStore<T> implements DataTableLayout.DataStore {

    private final ImmutableList<String> header;
    private final Function<T, Iterable<String>> iterableFunction;

    public AbstractDataStore(ImmutableList<String> header, Function<T, Iterable<String>> iterableFunction) {
        this.iterableFunction = iterableFunction;
        this.header = header;
    }

    @Override
    @WorkerThread
    public final List<Iterable<String>> loadData(RepoOption... options) {
        return getLoadingStream(options)
                .map(iterableFunction)
                .collect(Collectors.<Iterable<String>>toList());
    }

    @WorkerThread
    protected abstract Stream<T> getLoadingStream(RepoOption[] options);

    @Override
    @UiThread
    public final List<String> getDataHeaders() {
        return header;
    }

    @Override
    @UiThread
    public Optional<Integer> getMaxWidthForData(int index) {
        return Optional.empty();
    }

}
