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

import java.io.Serializable;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Created by Michail Kulikov
 * 12/2/15
 */
@ThreadSafe
public interface ParentingDataExtractor<T, I extends Serializable> extends DataExtractor<T, I> {

    @Nullable
    IdentifiedData<T, I> extractParent(I id);

}
