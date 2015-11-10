package ru.adios.budgeter.adapters;

import android.content.Context;
import android.support.annotation.UiThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.adios.budgeter.R;

/**
 * Auto complete text view doing dynamic requests.
 *
 * Created by Michail Kulikov
 * 10/15/15
 */
@UiThread
public abstract class RequestingAutoCompleteAdapter<T> extends BaseAdapter implements Filterable {

    public interface StringPresenter<T> {

        String getStringPresentation(T item);

    }

    private final Context context;
    private final StringPresenter<T> presenter;

    private List<T> resultsCache = new ArrayList<>();

    public RequestingAutoCompleteAdapter(Context context) {
        this.context = context;
        this.presenter = new DefPresenter<>();
    }

    public RequestingAutoCompleteAdapter(Context context, StringPresenter<T> presenter) {
        this.context = context;
        this.presenter = presenter;
    }

    @Override
    public int getCount() {
        return resultsCache.size();
    }

    @Override
    public T getItem(int position) {
        return resultsCache.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.simple_dropdown_item_1line, parent, false);
        }
        ((TextView) convertView.findViewById(R.id.text1)).setText(presenter.getStringPresentation(getItem(position)));

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
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
        };
    }

    protected abstract List<T> doRequest(String constraint);

    private static final class DefPresenter<T> implements StringPresenter<T> {
        @Override
        public String getStringPresentation(T item) {
            return item.toString();
        }
    }

}
