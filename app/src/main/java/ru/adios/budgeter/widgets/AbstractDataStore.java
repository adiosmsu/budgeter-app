package ru.adios.budgeter.widgets;

import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import java8.util.Optional;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import ru.adios.budgeter.api.RepoOption;

/**
 * Created by Michail Kulikov
 * 11/7/15
 */
@Immutable
public abstract class AbstractDataStore<T> implements DataTableLayout.DataStore {

    private final ImmutableList<String> header;
    private final Function<T, Iterable<String>> iterableFunction;

    public AbstractDataStore(ImmutableList<String> header, Function<T, Iterable<String>> iterableFunction) {
        this.iterableFunction = iterableFunction;
        this.header = header;
    }

    @Override
    @WorkerThread
    public final List<Iterable<String>> loadData(RepoOption... options) {
        return getLoadingStream(options)
                .map(iterableFunction)
                .collect(Collectors.<Iterable<String>>toList());
    }

    @WorkerThread
    protected abstract Stream<T> getLoadingStream(RepoOption[] options);

    @Override
    @UiThread
    public final List<String> getDataHeaders() {
        return header;
    }

    @Override
    @UiThread
    public Optional<Integer> getMaxWidthForData(int index) {
        return Optional.empty();
    }

}
