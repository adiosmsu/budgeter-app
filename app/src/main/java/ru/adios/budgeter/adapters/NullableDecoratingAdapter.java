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

package ru.adios.budgeter.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Collection;
import java.util.List;

import java8.util.Optional;
import java8.util.OptionalInt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;

/**
 * Created by Michail Kulikov
 * 11/28/15
 */
@UiThread
public class NullableDecoratingAdapter<AdapterType extends BaseAdapter & ThemedSpinnerAdapter, T>
        extends BaseAdapter
        implements NullableAdapter, ThemedSpinnerAdapter, MutableAdapter<T>, StringPresentingAdapter<T> {

    public static <Type> void adaptSpinnerWithArrayWrapper(Spinner spinner, Optional<StringPresenter<Type>> presenter, Type[] array) {
        NullableDecoratingAdapter<CompatArrayAdapter<Type>, Type> adapter = constructArrayWrapper(spinner.getContext(), spinner.getPrompt().toString(), array);
        adaptSpinner(spinner, adapter, presenter);
    }

    public static <Type> void adaptSpinnerWithArrayWrapper(Spinner spinner, Optional<StringPresenter<Type>> presenter, List<Type> list) {
        NullableDecoratingAdapter<CompatArrayAdapter<Type>, Type> adapter = constructArrayWrapper(spinner.getContext(), spinner.getPrompt().toString(), list);
        adaptSpinner(spinner, adapter, presenter);
    }

    private static <Type> void adaptSpinner(Spinner spinner, NullableDecoratingAdapter<CompatArrayAdapter<Type>, Type> adapter, Optional<StringPresenter<Type>> presenter) {
        if (presenter.isPresent()) {
            adapter.setStringPresenter(presenter.get());
        }
        spinner.setAdapter(adapter);
    }

    public static <Type> NullableDecoratingAdapter<CompatArrayAdapter<Type>, Type> constructArrayWrapper(Context context, String nullVal, Type[] array) {
        return constructArrayWrapperInner(context, nullVal, new CompatArrayAdapter<>(context, android.R.layout.simple_spinner_item, array));
    }

    public static <Type> NullableDecoratingAdapter<CompatArrayAdapter<Type>, Type> constructArrayWrapper(Context context, String nullVal, List<Type> list) {
        return constructArrayWrapperInner(context, nullVal, new CompatArrayAdapter<>(context, android.R.layout.simple_spinner_item, list));
    }

    private static <Type> NullableDecoratingAdapter<CompatArrayAdapter<Type>, Type> constructArrayWrapperInner(Context context, String nullVal, CompatArrayAdapter<Type> innerAdapter) {
        innerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return new NullableDecoratingAdapter<>(context, innerAdapter, nullVal, android.R.layout.simple_spinner_item);
    }


    private final AdapterType delegate;
    private final NullViewProvider nullViewProvider;
    private final boolean delegateIsArray;
    private final boolean delegateIsMutable;
    @StringRes
    private final OptionalInt resNullValStr;
    private final Optional<String> nullValStr;
    @IdRes
    private final OptionalInt fieldId;

    private StringPresenter<T> stringPresenter;

    public NullableDecoratingAdapter(Context context, AdapterType delegate, @StringRes int resNullValStr, @LayoutRes int resource) {
        this(context, delegate, resNullValStr, resource, OptionalInt.empty());
    }

    public NullableDecoratingAdapter(Context context, AdapterType delegate, @StringRes int resNullValStr, @LayoutRes int resource, @IdRes OptionalInt fieldId) {
        this.delegate = delegate;
        this.delegateIsArray = delegate instanceof ArrayAdapter;
        this.delegateIsMutable = delegate instanceof MutableAdapter && ((MutableAdapter) delegate).isMutable();
        this.resNullValStr = OptionalInt.of(resNullValStr);
        this.nullValStr = Optional.empty();
        this.fieldId = fieldId;

        nullViewProvider = fieldId.isPresent()
                ? new NullViewProvider(context, resource, fieldId.getAsInt())
                : new NullViewProvider(context, resource);
    }

    public NullableDecoratingAdapter(Context context, AdapterType delegate, String nullValStr, @LayoutRes int resource) {
        this(context, delegate, nullValStr, resource, OptionalInt.empty());
    }

    public NullableDecoratingAdapter(Context context, AdapterType delegate, String nullValStr, @LayoutRes int resource, @IdRes OptionalInt fieldId) {
        this.delegate = delegate;
        this.delegateIsArray = delegate instanceof ArrayAdapter;
        this.delegateIsMutable = delegate instanceof MutableAdapter && ((MutableAdapter) delegate).isMutable();
        this.resNullValStr = OptionalInt.empty();
        this.nullValStr = Optional.of(nullValStr);
        this.fieldId = fieldId;

        nullViewProvider = fieldId.isPresent()
                ? new NullViewProvider(context, resource, fieldId.getAsInt())
                : new NullViewProvider(context, resource);
    }

    @Override
    public <A extends Adapter> void setNullSelection(AdapterView<A> adapterView) {
        adapterView.setSelection(0);
    }

    @Override
    public void setStringPresenter(StringPresenter<T> stringPresenter) {
        this.stringPresenter = stringPresenter;
        if (delegate instanceof StringPresentingAdapter) {
            //noinspection unchecked
            ((StringPresentingAdapter<T>) delegate).setStringPresenter(stringPresenter);
        } else {
            stringPresenter.registerAdapter(this);
        }
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (position == 0) {
            return nullViewProvider.getDropDownView(0, convertView, parent);
        }

        final View dropDownView = delegate.getDropDownView(position - 1, convertView, parent);
        checkStringPresentationForView(position, dropDownView);
        return dropDownView;
    }

    @Override
    public int getCount() {
        return delegate.getCount() + 1; // first item is extra as a null value that does not exist inside a delegate.
    }

    @Override
    public T getItem(int position) {
        if (position == 0) {
            return null;
        }
        //noinspection unchecked
        return (T) delegate.getItem(position - 1);
    }

    @Override
    public long getItemId(int position) {
        if (position == 0) {
            return Long.MIN_VALUE;
        }
        return delegate.getItemId(position - 1);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return 0;
        }
        return delegate.getItemViewType(position - 1);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position == 0) {
            return nullViewProvider.getView(0, convertView, parent);
        }

        final View view = delegate.getView(position - 1, convertView, parent);
        checkStringPresentationForView(position, view);
        return view;
    }

    private void checkStringPresentationForView(int position, View view) {
        if (!(delegate instanceof StringPresentingAdapter) && stringPresenter != null) {
            TextView text;
            try {
                if (fieldId.isPresent()) {
                    text = (TextView) view.findViewById(fieldId.getAsInt());
                } else {
                    text = (TextView) view;
                }
            } catch (ClassCastException e) {
                throw new IllegalStateException("NullableDecoratingAdapter requires the resource ID to be a TextView", e);
            }
            text.setText(stringPresenter.getStringPresentation(getItem(position)));
        }
    }

    @Override
    public int getViewTypeCount() {
        return delegate.getViewTypeCount() + 1;
    }

    @Override
    public boolean hasStableIds() {
        return delegate.hasStableIds();
    }

    @Override
    public boolean isEmpty() {
        return false; // always have null value
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
        delegate.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);
        delegate.unregisterDataSetObserver(observer);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return delegate.areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(int position) {
        return position == 0
                || delegate.isEnabled(position - 1);
    }

    @Override
    @Nullable
    public Resources.Theme getDropDownViewTheme() {
        return delegate.getDropDownViewTheme();
    }

    @Override
    public void setDropDownViewTheme(Resources.Theme theme) {
        nullViewProvider.setDropDownViewTheme(theme);
        delegate.setDropDownViewTheme(theme);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void add(T object) {
        if (delegateIsArray) {
            ((ArrayAdapter<T>) delegate).add(object);
        } else if (delegateIsMutable) {
            ((MutableAdapter<T>) delegate).add(object);
        } else {
            throw new IllegalStateException(getImmutableStateMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addAll(Collection<? extends T> collection) {
        if (delegateIsArray) {
            ((ArrayAdapter<T>) delegate).addAll(collection);
        } else if (delegateIsMutable) {
            ((MutableAdapter<T>) delegate).addAll(collection);
        } else {
            throw new IllegalStateException(getImmutableStateMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addAll(T... items) {
        if (delegateIsArray) {
            ((ArrayAdapter<T>) delegate).addAll(items);
        } else if (delegateIsMutable) {
            ((MutableAdapter<T>) delegate).addAll(items);
        } else {
            throw new IllegalStateException(getImmutableStateMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insert(T object, int index) {
        checkArgument(index != 0, "Cannot insert on null value place");
        if (delegateIsArray) {
            ((ArrayAdapter<T>) delegate).insert(object, index - 1);
        } else if (delegateIsMutable) {
            ((MutableAdapter<T>) delegate).insert(object, index - 1);
        } else {
            throw new IllegalStateException(getImmutableStateMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void remove(T object) {
        checkArgument(object != null, "Cannot remove null");
        if (delegateIsArray) {
            ((ArrayAdapter<T>) delegate).remove(object);
        } else if (delegateIsMutable) {
            ((MutableAdapter<T>) delegate).remove(object);
        } else {
            throw new IllegalStateException(getImmutableStateMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void clear() {
        if (delegateIsArray) {
            ((ArrayAdapter<T>) delegate).clear();
        } else if (delegateIsMutable) {
            ((MutableAdapter<T>) delegate).clear();
        } else {
            throw new IllegalStateException(getImmutableStateMessage());
        }
    }

    @Override
    public boolean isMutable() {
        return delegateIsMutable || delegateIsArray;
    }

    private String getImmutableStateMessage() {
        return "Underlying adapter is immutable";
    }

    private final class NullViewProvider extends ViewProvidingBaseAdapter<String> {

        NullViewProvider(Context context, @LayoutRes int resource) {
            super(context, resource);
        }

        NullViewProvider(Context context, @LayoutRes int resource, @IdRes int fieldId) {
            super(context, resource, fieldId);
        }

        @Override
        public String getItem(int position) {
            checkElementIndex(position, 1);
            return resNullValStr.isPresent()
                    ? context.getResources().getString(resNullValStr.getAsInt())
                    : nullValStr.get();
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

    }

}
