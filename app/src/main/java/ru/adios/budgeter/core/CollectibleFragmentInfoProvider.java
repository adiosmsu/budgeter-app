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

package ru.adios.budgeter.core;

import android.os.Bundle;
import android.support.annotation.IdRes;

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import java8.util.Optional;
import ru.adios.budgeter.Submitter;

/**
 * Created by Michail Kulikov
 * 10/13/15
 */
@Immutable
public final class CollectibleFragmentInfoProvider<T, Sub extends Submitter<T>> implements CollectedFragmentsInfoProvider.InfoProvider<T, Sub> {

    public static final class Builder<T, Sub extends Submitter<T>> {

        @IdRes
        private final int id;
        private final Feedbacker feedbacker;
        private final ImmutableMap.Builder<String, CoreElementActivity.CoreElementSubmitInfo<T, Sub>> submitterButtonsMapBuilder = ImmutableMap.builder();
        private final ImmutableMap.Builder<String, CoreElementActivity.CoreElementFieldInfo> fieldsInfoMapBuilder = ImmutableMap.builder();
        private final CoreErrorHighlighter highlighter;
        private final Optional<CoreElementActivity.Retainer> optRetainer;

        public Builder(int id, Feedbacker feedbacker, CoreErrorHighlighter highlighter, @Nullable CoreElementActivity.Retainer retainer) {
            this.id = id;
            this.feedbacker = feedbacker;
            this.highlighter = highlighter;
            this.optRetainer = Optional.ofNullable(retainer);
        }

        public Builder(int id, Feedbacker feedbacker, CoreErrorHighlighter highlighter) {
            this(id, feedbacker, highlighter, null);
        }

        public Builder<T, Sub> addButtonInfo(String buttonName, CoreElementActivity.CoreElementSubmitInfo<T, Sub> submitInfo) {
            submitterButtonsMapBuilder.put(buttonName, submitInfo);
            return this;
        }

        public Builder<T, Sub> addFieldInfo(String fragmentFieldName, CoreElementActivity.CoreElementFieldInfo coreElementFieldInfo) {
            fieldsInfoMapBuilder.put(fragmentFieldName, coreElementFieldInfo);
            return this;
        }

        public CollectibleFragmentInfoProvider<T, Sub> build() {
            return new CollectibleFragmentInfoProvider<>(id, submitterButtonsMapBuilder.build(), fieldsInfoMapBuilder.build(), highlighter, feedbacker, optRetainer);
        }

    }

    @IdRes
    private final int id;
    private final ImmutableMap<String, CoreElementActivity.CoreElementSubmitInfo<T, Sub>> submitterButtonsMap;
    private final ImmutableMap<String, CoreElementActivity.CoreElementFieldInfo> fieldInfoMap;
    private final CoreErrorHighlighter highlighter;
    private final Feedbacker feedbacker;
    private final Optional<CoreElementActivity.Retainer> optRetainer;

    private CollectibleFragmentInfoProvider(int id,
                                            ImmutableMap<String, CoreElementActivity.CoreElementSubmitInfo<T, Sub>> submitterButtonsMap,
                                            ImmutableMap<String, CoreElementActivity.CoreElementFieldInfo> fieldInfoMap,
                                            CoreErrorHighlighter highlighter,
                                            Feedbacker feedbacker,
                                            Optional<CoreElementActivity.Retainer> optRetainer) {
        this.id = id;
        this.submitterButtonsMap = submitterButtonsMap;
        this.fieldInfoMap = fieldInfoMap;
        this.highlighter = highlighter;
        this.feedbacker = feedbacker;
        this.optRetainer = optRetainer;
    }

    @Override
    public int getFragmentId() {
        return id;
    }

    @Override
    public CoreElementActivity.CoreElementSubmitInfo<T, Sub> getSubmitInfo(String buttonName) {
        final CoreElementActivity.CoreElementSubmitInfo<T, Sub> submitInfo = submitterButtonsMap.get(buttonName);
        if (submitInfo == null) {
            throw new IllegalArgumentException("Unsupported button name: " + buttonName);
        }
        return submitInfo;
    }

    @Override
    public CoreElementActivity.CoreElementFieldInfo getCoreElementFieldInfo(String fragmentFieldName) {
        final CoreElementActivity.CoreElementFieldInfo fieldInfo = fieldInfoMap.get(fragmentFieldName);
        if (fieldInfo == null) {
            throw new IllegalArgumentException("Unsupported fragment field info name: " + fragmentFieldName);
        }
        return fieldInfo;
    }

    @Override
    public void clearViewReferences() {
        feedbacker.clearViewReferences();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        highlighter.onSaveInstanceState(outState);
        if (optRetainer.isPresent()) {
            optRetainer.get().onSaveInstanceState(outState);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        highlighter.onRestoreInstanceState(savedInstanceState);
        if (optRetainer.isPresent()) {
            optRetainer.get().onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void collectEssentialViews(CoreElementActivity activity) {
        feedbacker.collectEssentialViews(activity);
    }

    @Override
    public void performFeedback() {
        feedbacker.performFeedback();
    }

    public interface Feedbacker {

        void performFeedback();

        void collectEssentialViews(CoreElementActivity activity);

        void clearViewReferences();

    }

}
