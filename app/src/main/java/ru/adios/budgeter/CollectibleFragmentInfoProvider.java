package ru.adios.budgeter;

import android.support.annotation.IdRes;

import com.google.common.collect.ImmutableMap;

/**
 * Created by Michail Kulikov
 * 10/13/15
 */
public final class CollectibleFragmentInfoProvider<T, Sub extends Submitter<T>> implements CollectedFragmentsInfoProvider.InfoProvider<T, Sub> {

    public static final class Builder<T, Sub extends Submitter<T>> {

        @IdRes
        private final int id;
        private final Feedbacker feedbacker;
        private final ImmutableMap.Builder<String, CoreElementActivity.CoreElementSubmitInfo<T, Sub>> submitterButtonsMapBuilder = ImmutableMap.builder();
        private final ImmutableMap.Builder<String, CoreElementActivity.CoreElementFieldInfo> fieldsInfoMapBuilder = ImmutableMap.builder();

        public Builder(int id, Feedbacker feedbacker) {
            this.id = id;
            this.feedbacker = feedbacker;
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
            return new CollectibleFragmentInfoProvider<>(id, submitterButtonsMapBuilder.build(), fieldsInfoMapBuilder.build(), feedbacker);
        }

    }

    @IdRes
    private final int id;
    private final ImmutableMap<String, CoreElementActivity.CoreElementSubmitInfo<T, Sub>> submitterButtonsMap;
    private final ImmutableMap<String, CoreElementActivity.CoreElementFieldInfo> fieldInfoMap;
    private final Feedbacker feedbacker;

    private CollectibleFragmentInfoProvider(int id,
                                            ImmutableMap<String, CoreElementActivity.CoreElementSubmitInfo<T, Sub>> submitterButtonsMap,
                                            ImmutableMap<String, CoreElementActivity.CoreElementFieldInfo> fieldInfoMap,
                                            Feedbacker feedbacker) {
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
    public void performFeedback(CoreElementActivity activity) {
        feedbacker.performFeedback(activity);
    }

    protected interface Feedbacker {

        void performFeedback(CoreElementActivity activity);

    }

}
