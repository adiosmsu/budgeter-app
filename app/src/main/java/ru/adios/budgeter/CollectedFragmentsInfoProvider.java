package ru.adios.budgeter;

import android.support.annotation.IdRes;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;

/**
 * Created by Michail Kulikov
 * 10/13/15
 */
final class CollectedFragmentsInfoProvider<T> implements CoreElementActivity.FragmentsInfoProvider<T> {

    public static final class Builder<T> {

        private final ImmutableMap.Builder<Integer, InfoProvider<T>> dataMapBuilder = ImmutableMap.builder();
        private final CoreElementActivity<T> activity;

        public Builder(CoreElementActivity<T> activity) {
            this.activity = activity;
        }

        public Builder<T> addProvider(InfoProvider<T> infoProvider) {
            dataMapBuilder.put(infoProvider.getFragmentId(), infoProvider);
            return this;
        }

        public CollectedFragmentsInfoProvider<T> build() {
            return new CollectedFragmentsInfoProvider<>(dataMapBuilder.build(), activity);
        }

    }

    private final ImmutableMap<Integer, InfoProvider<T>> dataMap;
    private final CoreElementActivity<T> activity;

    private CollectedFragmentsInfoProvider(ImmutableMap<Integer, InfoProvider<T>> dataMap, CoreElementActivity<T> activity) {
        this.dataMap = dataMap;
        this.activity = activity;
    }

    @Override
    public CoreElementActivity.CoreElementSubmitInfo<T> getSubmitInfo(@IdRes int fragmentId, String buttonName) {
        return getProvider(fragmentId).getSubmitInfo(buttonName);
    }

    @Override
    public ImmutableCollection<Integer> allowedFragments() {
        return dataMap.keySet();
    }

    @Override
    public CoreElementActivity.CoreElementFieldInfo getCoreElementFieldInfo(@IdRes int fragmentId, String fragmentFieldName) {
        return getProvider(fragmentId).getCoreElementFieldInfo(fragmentFieldName);
    }

    @Override
    public void performFeedback() {
        for (final InfoProvider<T> infoProvider : dataMap.values()) {
            infoProvider.performFeedback(activity);
        }
    }

    @Nonnull
    private InfoProvider<T> getProvider(@IdRes int fragmentId) {
        final InfoProvider<T> infoProvider = dataMap.get(fragmentId);
        if (infoProvider == null) {
            throw new IllegalArgumentException("Unsupported fragment: " + activity.getResources().getResourceName(fragmentId));
        }
        return infoProvider;
    }

    protected interface InfoProvider<T> {

        @IdRes
        int getFragmentId();

        CoreElementActivity.CoreElementSubmitInfo<T> getSubmitInfo(String buttonName);

        CoreElementActivity.CoreElementFieldInfo getCoreElementFieldInfo(String fragmentFieldName);

        void performFeedback(CoreElementActivity<T> activity);

    }

}
