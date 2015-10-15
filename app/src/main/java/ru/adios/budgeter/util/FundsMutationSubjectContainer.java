package ru.adios.budgeter.util;

import javax.annotation.Nonnull;

import ru.adios.budgeter.api.FundsMutationSubject;

/**
 * Created by Michail Kulikov
 * 10/15/15
 */
public final class FundsMutationSubjectContainer extends CachingHintedContainer<FundsMutationSubject> {

    public static final Factory FACTORY = new Factory();

    public FundsMutationSubjectContainer(FundsMutationSubject subject) {
        super(subject);
    }

    @Nonnull
    @Override
    protected String calculateToString() {
        final FundsMutationSubject subject = getObject();
        return subject.name + " (" + subject.type.toString().toLowerCase() + ')';
    }

    public static final class Factory implements HintedArrayAdapter.ContainerFactory<FundsMutationSubject> {

        @Override
        public HintedArrayAdapter.ObjectContainer<FundsMutationSubject> create(FundsMutationSubject subject) {
            return new FundsMutationSubjectContainer(subject);
        }

    }

}
