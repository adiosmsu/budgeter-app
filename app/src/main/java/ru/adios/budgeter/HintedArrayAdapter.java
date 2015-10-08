package ru.adios.budgeter;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import ru.adios.budgeter.util.GeneralUtils;

/**
 * Adapter for Spinner to display a hint.
 *
 * Created by Michail Kulikov
 * 10/8/15
 */
public final class HintedArrayAdapter extends ArrayAdapter<String> {

    public static void adaptStandardSpinner(Spinner spinner, Context context, String[] values) {
        final HintedArrayAdapter adapter = new HintedArrayAdapter(context, android.R.layout.simple_spinner_item, values, spinner.getPrompt().toString());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(adapter.getCount());
    }


    public HintedArrayAdapter(Context context, @LayoutRes int resource, String[] values, String hint) {
        super(context, resource, GeneralUtils.arrayPlusValue(values, hint));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View v = super.getView(position, convertView, parent);

        if (position == getCount()) {
            final TextView textView = (TextView) v.findViewById(android.R.id.text1);
            textView.setText("");
            textView.setHint(getItem(getCount())); //"Hint to be displayed"
        }

        return v;
    }

    @Override
    public int getCount() {
        return super.getCount() - 1; // you don't display last item. It is used as a hint.
    }

}
