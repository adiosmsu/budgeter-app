package ru.adios.budgeter;

import android.support.annotation.IdRes;

import com.google.common.collect.ImmutableMap;

/**
 * Created by Michail Kulikov
 * 10/13/15
 */
public final class CollectibleFragmentInfoProvider<T> implements CollectedFragmentsInfoProvider.InfoProvider<T> {

    public static final class Builder<T> {

        @IdRes
        private final int id;
        private final Feedbacker<T> feedbacker;
        private final ImmutableMap.Builder<String, CoreElementActivity.CoreElementSubmitInfo<T>> submitterButtonsMapBuilder = ImmutableMap.builder();
        private final ImmutableMap.Builder<String, CoreElementActivity.CoreElementFieldInfo> fieldsInfoMapBuilder = ImmutableMap.builder();

        public Builder(int id, Feedbacker<T> feedbacker) {
            this.id = id;
            this.feedbacker = feedbacker;
        }

        public Builder<T> addButtonInfo(String buttonName, CoreElementActivity.CoreElementSubmitInfo<T> submitInfo) {
            submitterButtonsMapBuilder.put(buttonName, submitInfo);
            return this;
        }

        public Builder<T> addFieldInfo(String fragmentFieldName, CoreElementActivity.CoreElementFieldInfo coreElementFieldInfo) {
            fieldsInfoMapBuilder.put(fragmentFieldName, coreElementFieldInfo);
            return this;
        }

        public CollectibleFragmentInfoProvider<T> build() {
            return new CollectibleFragmentInfoProvider<>(id, submitterButtonsMapBuilder.build(), fieldsInfoMapBuilder.build(), feedbacker);
        }

    }

    @IdRes
    private final int id;
    private final ImmutableMap<String, CoreElementActivity.CoreElementSubmitInfo<T>> submitterButtonsMap;
    private final ImmutableMap<String, CoreElementActivity.CoreElementFieldInfo> fieldInfoMap;
    private final Feedbacker<T> feedbacker;

    private CollectibleFragmentInfoProvider(int id,
                                            ImmutableMap<String, CoreElementActivity.CoreElementSubmitInfo<T>> submitterButtonsMap,
                                            ImmutableMap<String, CoreElementActivity.CoreElementFieldInfo> fieldInfoMap,
                                            Feedbacker<T> feedbacker) {
        this.id = id;
        this.submitterButtonsMap = submitterButtonsMap;
        this.fieldInfoMap = fieldInfoMap;
        this.feedbacker = feedbacker;
    }

    @Override
    public int getFragmentId() {
        return id;
    }

    @Override
    public CoreElementActivity.CoreElementSubmitInfo<T> getSubmitInfo(String buttonName) {
        final CoreElementActivity.CoreElementSubmitInfo<T> submitInfo = submitterButtonsMap.get(buttonName);
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
    public void performFeedback(CoreElementActivity<T> activity) {
        feedbacker.performFeedback(activity);
    }

    protected interface Feedbacker<T> {

        void performFeedback(CoreElementActivity<T> activity);

    }

}
