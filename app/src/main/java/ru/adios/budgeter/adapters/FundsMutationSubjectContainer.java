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

import android.os.AsyncTask;
import android.support.annotation.UiThread;

import javax.annotation.Nonnull;

import java8.util.Optional;
import ru.adios.budgeter.api.data.FundsMutationSubject;

/**
 * Created by Michail Kulikov
 * 10/15/15
 */
@UiThread
public final class FundsMutationSubjectContainer extends CachingHintedContainer<FundsMutationSubject> {

    public static final Factory FACTORY = new Factory();

    private String parentInfo;

    public FundsMutationSubjectContainer(FundsMutationSubject subject) {
        super(subject);
        new AsyncTask<FundsMutationSubject, Void, String>() {
            @Override
            protected String doInBackground(FundsMutationSubject... params) {
                final Optional<FundsMutationSubject> parent = params[0].getParent();
                if (parent.isPresent()) {
                    return parent.get().name;
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                if (s != null) {
                    parentInfo = s;
                    invalidateCache();
                }
            }
        }.execute(subject);
    }

    @Nonnull
    @Override
    protected String calculateToString() {
        final FundsMutationSubject subject = getObject();
        final String typeStr = subject.type.toString().toLowerCase();
        final StringBuilder builder = new StringBuilder(subject.name.length() + typeStr.length() + 20);
        builder.append(subject.name)
                .append(" (")
                .append(typeStr);
        if (parentInfo != null) {
            builder.append(", parent: ").append(parentInfo);
        }
        builder.append(')');
        return builder.toString();
    }

    public static final class Factory implements HintedArrayAdapter.ContainerFactory<FundsMutationSubject> {

        @Override
        public HintedArrayAdapter.ObjectContainer<FundsMutationSubject> create(FundsMutationSubject subject) {
            return new FundsMutationSubjectContainer(subject);
        }

    }

}
