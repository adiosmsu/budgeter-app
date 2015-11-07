package ru.adios.budgeter.util;

import com.google.common.collect.ImmutableList;

import java.util.List;

import java8.util.Optional;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import ru.adios.budgeter.api.RepoOption;

/**
 * Created by Michail Kulikov
 * 11/7/15
 */
public abstract class AbstractDataStore<T> implements DataTableLayout.DataStore {

    private final ImmutableList<String> header;
    private final Function<T, Iterable<String>> iterableFunction;

    public AbstractDataStore(ImmutableList<String> header, Function<T, Iterable<String>> iterableFunction) {
        this.iterableFunction = iterableFunction;
        this.header = header;
    }

    @Override
    public final List<Iterable<String>> loadData(RepoOption... options) {
        return getLoadingStream(options)
                .map(iterableFunction)
                .collect(Collectors.<Iterable<String>>toList());
    }

    protected abstract Stream<T> getLoadingStream(RepoOption[] options);

    @Override
    public final List<String> getDataHeaders() {
        return header;
    }

    @Override
    public Optional<Integer> getMaxWidthForData(int index) {
        return Optional.empty();
    }

}
