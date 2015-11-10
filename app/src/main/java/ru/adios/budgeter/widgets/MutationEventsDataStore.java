package ru.adios.budgeter.widgets;

import android.content.res.Resources;
import android.support.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;

import javax.annotation.concurrent.ThreadSafe;

import java8.util.function.Function;
import java8.util.stream.Stream;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.R;
import ru.adios.budgeter.api.RepoOption;
import ru.adios.budgeter.api.data.FundsMutationEvent;
import ru.adios.budgeter.util.Formatting;

/**
 * Created by Michail Kulikov
 * 11/7/15
 */
@ThreadSafe
public class MutationEventsDataStore extends AbstractDataStore<FundsMutationEvent> {

    public MutationEventsDataStore(final Resources resources) {
        super(
                ImmutableList.of(
                        resources.getString(R.string.ah_ops_table_col_time),
                        resources.getString(R.string.ah_ops_table_col_subj),
                        resources.getString(R.string.ah_ops_table_col_money),
                        resources.getString(R.string.ah_ops_table_col_account)
                ),
                new Function<FundsMutationEvent, Iterable<String>>() {
                    @Override
                    public Iterable<String> apply(FundsMutationEvent event) {
                        return ImmutableList.of(
                                Formatting.toStringRusDateTimeShort(event.timestamp),
                                event.subject.name,
                                Formatting.toStringMoneyUsingSign(event.amount.multipliedBy(event.quantity), resources),
                                event.relevantBalance.name
                        );
                    }
                }
        );
    }

    public MutationEventsDataStore(ImmutableList<String> mutationsHeader, Function<FundsMutationEvent, Iterable<String>> iterableFunction) {
        super(mutationsHeader, iterableFunction);
    }

    @Override
    @WorkerThread
    protected Stream<FundsMutationEvent> getLoadingStream(RepoOption[] options) {
        return BundleProvider.getBundle()
                .fundsMutationEvents()
                .streamMutationEvents(options);
    }

    @Override
    @WorkerThread
    public int count() {
        return BundleProvider.getBundle().fundsMutationEvents().countMutationEvents();
    }

}
