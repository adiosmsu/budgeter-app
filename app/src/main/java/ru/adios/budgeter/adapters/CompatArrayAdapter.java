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
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Created by Michail Kulikov
 * 11/29/15
 */
@UiThread
public class CompatArrayAdapter<T> extends ArrayAdapter<T> implements ThemedSpinnerAdapter, MutableAdapter<T> {

    private final ThemedSpinnerAdapter.Helper dropDownHelper;
    private final Context context;
    private final int resource;
    private final int textViewResourceId;

    public CompatArrayAdapter(Context context, int resource) {
        super(context, resource);
        this.context = context;
        this.resource = resource;
        this.textViewResourceId = 0;
        dropDownHelper = new ThemedSpinnerAdapter.Helper(context);
    }

    public CompatArrayAdapter(Context context, int resource, List<T> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.textViewResourceId = 0;
        dropDownHelper = new ThemedSpinnerAdapter.Helper(context);
    }

    public CompatArrayAdapter(Context context, int resource, T[] objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.textViewResourceId = 0;
        dropDownHelper = new ThemedSpinnerAdapter.Helper(context);
    }

    public CompatArrayAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
        this.context = context;
        this.resource = resource;
        this.textViewResourceId = textViewResourceId;
        dropDownHelper = new ThemedSpinnerAdapter.Helper(context);
    }

    public CompatArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects) {
        super(context, resource, textViewResourceId, objects);
        this.context = context;
        this.resource = resource;
        this.textViewResourceId = textViewResourceId;
        dropDownHelper = new ThemedSpinnerAdapter.Helper(context);
    }

    public CompatArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
        super(context, resource, textViewResourceId, objects);
        this.context = context;
        this.resource = resource;
        this.textViewResourceId = textViewResourceId;
        dropDownHelper = new ThemedSpinnerAdapter.Helper(context);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return ViewProvidingBaseAdapter.createViewFromResource(
                context, this, null, dropDownHelper.getDropDownViewInflater(), position, convertView, parent, resource, textViewResourceId
        );
    }

    @Override
    public void setDropDownViewTheme(@Nullable Resources.Theme theme) {
        dropDownHelper.setDropDownViewTheme(theme);
    }

    @Nullable
    public Resources.Theme getDropDownViewTheme() {
        return dropDownHelper.getDropDownViewTheme();
    }

}
