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

import java.io.Serializable;

import javax.annotation.concurrent.Immutable;

/**
 * Created by Michail Kulikov
 * 12/3/15
 */
@Immutable
public final class IdentifiedData<T, I extends Serializable> {

    final T data;
    final I id;

    public IdentifiedData(T data, I id) {
        this.data = data;
        this.id = id;
    }

}
