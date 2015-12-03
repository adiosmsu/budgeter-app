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

import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import java.io.Serializable;

import javax.annotation.concurrent.ThreadSafe;

import ru.adios.budgeter.util.concurrent.AsyncTaskProvider;

/**
 * Created by Michail Kulikov
 * 12/2/15
 */
@ThreadSafe
public abstract class AsyncDataExtractor<T, I extends Serializable> extends AsyncTaskProvider implements DataExtractor<T, I> {

    @WorkerThread
    @Override
    public abstract T extractData(I id);

    @UiThread
    @Override
    public abstract I extractId(T data);

}
