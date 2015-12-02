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

import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.annotation.UiThread;

import java.util.LinkedHashMap;

import java8.util.Optional;
import ru.adios.budgeter.api.data.BalanceAccount;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.core.CoreUtils;

/**
 * Created by Michail Kulikov
 * 11/29/15
 */
public final class Presenters {

    public static BalanceAccountDefaultPresenter getBalanceAccountDefaultPresenter(Resources resources) {
        return new BalanceAccountDefaultPresenter(resources);
    }

    public static SubjectParentLoadingPresenter getSubjectParentLoadingPresenter() {
        return new SubjectParentLoadingPresenter();
    }

    public static AgentDefaultPresenter getAgentDefaultPresenter() {
        return AGENT_DEFAULT_PRESENTER;
    }

    @UiThread
    public static final class BalanceAccountDefaultPresenter extends UnchangingStringPresenter<BalanceAccount> {

        private final Resources resources;

        public BalanceAccountDefaultPresenter(Resources resources) {
            this.resources = resources;
        }

        @Override
        public String getStringPresentation(BalanceAccount item) {
            return CoreUtils.getExtendedAccountString(item, resources);
        }

    }

    @UiThread
    public static final class SubjectParentLoadingPresenter implements StringPresenter<FundsMutationSubject> {

        private static final String PENDING_MARKER = "<{@PENDING_MARKER@}>";
        private static final String NULL_MARKER = "<{@NULL_MARKER@}>";
        private static final LinkedHashMap<Integer, String> parentsCache = new LinkedHashMap<Integer, String>(1000, 1f, true) {
            @Override
            protected boolean removeEldestEntry(Entry<Integer, String> eldest) {
                return size() >= 999;
            }
        };
        private ObservedAdapter adapter;

        @Override
        public String getStringPresentation(FundsMutationSubject subject) {
            final String typeStr = subject.type.toString().toLowerCase();
            final StringBuilder builder = new StringBuilder(subject.name.length() + typeStr.length() + 20);
            builder.append(subject.name)
                    .append(" (")
                    .append(typeStr);

            final Integer subHash = subject.hashCode();
            final String parentInfo = parentsCache.get(subHash);
            if (!PENDING_MARKER.equals(parentInfo)) {
                if (parentInfo != null) {
                    if (!NULL_MARKER.equals(parentInfo)) {
                        builder.append(", parent: ").append(parentInfo);
                    }
                } else {
                    new AsyncTask<FundsMutationSubject, Void, String>() {
                        @Override
                        protected String doInBackground(FundsMutationSubject... params) {
                            final Optional<FundsMutationSubject> parent = params[0].getParent();
                            return parent.isPresent()
                                    ? parent.get().name
                                    : null;
                        }

                        @Override
                        protected void onPostExecute(String s) {
                            if (s != null) {
                                parentsCache.put(subHash, s);
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                }
                            } else {
                                parentsCache.put(subHash, NULL_MARKER);
                            }
                        }
                    }.execute(subject);
                }
            }

            builder.append(')');
            return builder.toString();
        }

        @Override
        public void registerAdapter(ObservedAdapter adapter) {
            this.adapter = adapter;
        }

    }

    public static final AgentDefaultPresenter AGENT_DEFAULT_PRESENTER = new AgentDefaultPresenter();

    @UiThread
    public static final class AgentDefaultPresenter extends UnchangingStringPresenter<FundsMutationAgent> {
        @Override
        public String getStringPresentation(FundsMutationAgent item) {
            return item.name;
        }
    }

    private Presenters() {}

}
