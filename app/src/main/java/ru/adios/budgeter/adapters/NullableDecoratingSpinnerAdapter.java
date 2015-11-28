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
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.Collection;

import java8.util.Optional;
import java8.util.OptionalInt;

import static com.google.common.base.Preconditions.checkElementIndex;

/**
 * Created by Michail Kulikov
 * 11/28/15
 */
@UiThread
public class NullableDecoratingSpinnerAdapter<AdapterType extends SpinnerAdapter & ListAdapter & ThemedSpinnerAdapter, T>
        implements SpinnerAdapter, ListAdapter, ThemedSpinnerAdapter, MutableAdapter<T>, StringPresentingAdapter<T> {

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

    public NullableDecoratingSpinnerAdapter(Context context, AdapterType delegate, @StringRes int resNullValStr, @LayoutRes int resource, @IdRes OptionalInt fieldId) {
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

    public NullableDecoratingSpinnerAdapter(Context context, AdapterType delegate, String nullValStr, @LayoutRes int resource, @IdRes OptionalInt fieldId) {
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
    public void setStringPresenter(StringPresenter<T> stringPresenter) {
        this.stringPresenter = stringPresenter;
        if (delegate instanceof StringPresentingAdapter) {
            //noinspection unchecked
            ((StringPresentingAdapter<T>) delegate).setStringPresenter(stringPresenter);
        }
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return delegate.getDropDownView(position, convertView, parent);
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
        if (!(delegate instanceof StringPresentingAdapter) && stringPresenter != null) {
            TextView text;
            try {
                if (fieldId.isPresent()) {
                    text = (TextView) view.findViewById(fieldId.getAsInt());
                } else {
                    text = (TextView) view;
                }
            } catch (ClassCastException e) {
                throw new IllegalStateException("NullableDecoratingSpinnerAdapter requires the resource ID to be a TextView", e);
            }
            text.setText(stringPresenter.getStringPresentation(getItem(position - 1)));
        }

        return view;
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
        delegate.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
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
