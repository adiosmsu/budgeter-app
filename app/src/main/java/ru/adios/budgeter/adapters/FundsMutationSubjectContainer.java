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
