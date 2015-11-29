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
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import ru.adios.budgeter.R;

/**
 * Auto complete text view doing dynamic requests.
 *
 * Created by Michail Kulikov
 * 10/15/15
 */
@NotThreadSafe
public abstract class RequestingAutoCompleteAdapter<T> extends BaseAdapter implements Filterable, StringPresentingAdapter<T> {

    private final LayoutInflater layoutInflater;

    private StringPresenter<T> presenter = new DefPresenter<>();

    private List<T> resultsCache = new ArrayList<>();

    public RequestingAutoCompleteAdapter(Context context) {
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public void setStringPresenter(StringPresenter<T> presenter) {
        this.presenter = presenter;
        presenter.registerAdapter(this);
    }

    @Override
    @UiThread
    public int getCount() {
        return resultsCache.size();
    }

    @Override
    @UiThread
    public T getItem(int position) {
        return resultsCache.get(position);
    }

    @Override
    @UiThread
    public long getItemId(int position) {
        return position;
    }

    @Override
    @UiThread
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.simple_dropdown_item_1line, parent, false);
        }
        ((TextView) convertView.findViewById(R.id.text1)).setText(presenter.getStringPresentation(getItem(position)));

        return convertView;
    }

    @Override
    @UiThread
    public Filter getFilter() {
        return new Filter() {
            @Override
            @WorkerThread
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults filterResults = new FilterResults();

                if (constraint != null) {
                    final List<T> items = doRequest(constraint.toString());
                    // Assign the data to the FilterResults
                    filterResults.values = items;
                    filterResults.count = items.size();
                }

                return filterResults;
            }

            @Override
            @UiThread
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    //noinspection unchecked
                    resultsCache = (List<T>) results.values;
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            @UiThread
            public CharSequence convertResultToString(Object resultValue) {
                return presenter.getStringPresentation((T) resultValue);
            }
        };
    }

    @WorkerThread
    protected abstract List<T> doRequest(String constraint);

    private static final class DefPresenter<T> extends UnchangingStringPresenter<T> {
        @Override
        public String getStringPresentation(T item) {
            return item.toString();
        }
    }

}
