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

package ru.adios.budgeter.util;

import com.google.common.collect.ImmutableList;

import java8.util.function.BiConsumer;
import java8.util.function.BinaryOperator;
import java8.util.function.Function;
import java8.util.function.Supplier;
import java8.util.stream.Collector;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;

/**
 * Created by Michail Kulikov
 * 12/1/15
 */
public class Immutables {

    private static final Supplier LIST_BUILDER_SUPPLIER = new Supplier<ImmutableList.Builder>() {
        @Override
        public ImmutableList.Builder get() {
            return ImmutableList.builder();
        }
    };
    private static final Function LIST_FINISHER_FUNCTION = new Function() {
        @Override
        public Object apply(Object builder) {
            return ((ImmutableList.Builder) builder).build();
        }
    };

    public static <T> ImmutableList<T> listFromStream(Stream<T> stream) {
        return stream.collect(Immutables.<T>getListCollector());
    }

    @SuppressWarnings("unchecked")
    public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> getListCollector() {
        return Collectors.of(
                (Supplier<ImmutableList.Builder<T>>) LIST_BUILDER_SUPPLIER,
                new BiConsumer<ImmutableList.Builder<T>, T>() {
                    @Override
                    public void accept(ImmutableList.Builder<T> b, T t) {
                        b.add(t);
                    }
                },
                new BinaryOperator<ImmutableList.Builder<T>>() {
                    @Override
                    public ImmutableList.Builder<T> apply(ImmutableList.Builder<T> builder1, ImmutableList.Builder<T> builder2) {
                        return builder1.addAll(builder2.build());
                    }
                },
                new Function<ImmutableList.Builder<T>, ImmutableList<T>>() {
                    @Override
                    public ImmutableList<T> apply(ImmutableList.Builder<T> builder) {
                        return builder.build();
                    }
                }
        );
    }

    private Immutables() {}

}
