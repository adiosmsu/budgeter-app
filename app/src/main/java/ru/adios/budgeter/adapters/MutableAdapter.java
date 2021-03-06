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

import java.util.Collection;

/**
 * Created by Michail Kulikov
 * 11/28/15
 */
public interface MutableAdapter<T> extends TypedAdapter<T> {

    void add(T object);

    void addAll(Collection<? extends T> collection);

    @SuppressWarnings({"unchecked"})
    void addAll(T... items);

    void insert(T object, int index);

    void remove(T object);

    void clear();

    boolean isMutable();

}
