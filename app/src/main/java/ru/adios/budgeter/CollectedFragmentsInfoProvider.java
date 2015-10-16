package ru.adios.budgeter;

import android.support.annotation.IdRes;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;

/**
 * Created by Michail Kulikov
 * 10/13/15
 */
final class CollectedFragmentsInfoProvider implements CoreElementActivity.FragmentsInfoProvider {

    public static final class Builder {

        private final ImmutableMap.Builder<Integer, InfoProvider> dataMapBuilder = ImmutableMap.builder();
        private final CoreElementActivity activity;

        public Builder(CoreElementActivity activity) {
            this.activity = activity;
        }

        public Builder addProvider(InfoProvider infoProvider) {
            dataMapBuilder.put(infoProvider.getFragmentId(), infoProvider);
            return this;
        }

        public CollectedFragmentsInfoProvider build() {
            return new CollectedFragmentsInfoProvider(dataMapBuilder.build(), activity);
        }

    }

    private final ImmutableMap<Integer, InfoProvider> dataMap;
    private final CoreElementActivity activity;

    private CollectedFragmentsInfoProvider(ImmutableMap<Integer, InfoProvider> dataMap, CoreElementActivity activity) {
        this.dataMap = dataMap;
        this.activity = activity;
    }

    @Override
    public CoreElementActivity.CoreElementSubmitInfo getSubmitInfo(@IdRes int fragmentId, String buttonName) {
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
        for (final InfoProvider infoProvider : dataMap.values()) {
            infoProvider.performFeedback(activity);
        }
    }

    @Nonnull
    private <T, Sub extends Submitter<T>> InfoProvider<T, Sub> getProvider(@IdRes int fragmentId) {
        @SuppressWarnings("unchecked")
        final InfoProvider<T, Sub> infoProvider = (InfoProvider<T, Sub>) dataMap.get(fragmentId);
        if (infoProvider == null) {
            throw new IllegalArgumentException("Unsupported fragment: " + activity.getResources().getResourceName(fragmentId));
        }
        return infoProvider;
    }

    protected interface InfoProvider<T, Sub extends Submitter<T>> {

        @IdRes
        int getFragmentId();

        CoreElementActivity.CoreElementSubmitInfo<T, Sub> getSubmitInfo(String buttonName);

        CoreElementActivity.CoreElementFieldInfo getCoreElementFieldInfo(String fragmentFieldName);

        void performFeedback(CoreElementActivity activity);

    }

}
