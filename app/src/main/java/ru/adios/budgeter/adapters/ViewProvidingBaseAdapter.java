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
import android.support.annotation.AnyRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.adios.budgeter.util.UiUtils;

/**
 * Created by Michail Kulikov
 * 11/27/15
 */
@UiThread
public abstract class ViewProvidingBaseAdapter<T> extends BaseAdapter implements ThemedSpinnerAdapter, StringPresentingAdapter<T> {

    private static final Logger logger = LoggerFactory.getLogger(ViewProvidingBaseAdapter.class);

    final Context context;
    private final LayoutInflater inflater;
    private final Helper dropDownHelper;

    @LayoutRes
    private int resource;
    @LayoutRes
    private int dropDownViewResource;
    @IdRes
    private int fieldId;

    private StringPresenter<T> stringPresenter;

    public ViewProvidingBaseAdapter(Context context, @LayoutRes int resource) {
        this(context, resource, 0);
    }

    public ViewProvidingBaseAdapter(Context context, @LayoutRes int resource, @IdRes int fieldId) {
        this.context = context;
        this.resource = this.dropDownViewResource = resource;
        this.fieldId = fieldId;
        this.inflater = LayoutInflater.from(context);
        this.dropDownHelper = new Helper(context);
    }

    @Override
    public void setStringPresenter(StringPresenter<T> stringPresenter) {
        this.stringPresenter = stringPresenter;
        stringPresenter.registerAdapter(this);
    }

    @Override
    public abstract T getItem(int position);

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(inflater, position, convertView, parent, resource);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(dropDownHelper.getDropDownViewInflater(), position, convertView, parent, dropDownViewResource);
    }

    public void setDropDownViewResource(@LayoutRes int resource) {
        dropDownViewResource = resource;
    }

    public int getDropDownViewResource() {
        return dropDownViewResource;
    }

    @Nullable
    @Override
    public Resources.Theme getDropDownViewTheme() {
        return dropDownHelper.getDropDownViewTheme();
    }

    @Override
    public void setDropDownViewTheme(Resources.Theme theme) {
        dropDownHelper.setDropDownViewTheme(theme);
    }

    private View createViewFromResource(LayoutInflater inflater, int position, View convertView, ViewGroup parent, int resource) {
        return createViewFromResource(context, this, stringPresenter, inflater, position, convertView, parent, resource, fieldId);
    }

    static <T> View createViewFromResource(
            Context context,
            Adapter adapter,
            StringPresenter<T> presenter,
            LayoutInflater inflater,
            int position,
            View convertView,
            ViewGroup parent,
            int resource,
            int fieldId
    ) {
        final View view;
        final TextView text;
        if (convertView == null) {
            view = inflater.inflate(resource, parent, false);
        } else {
            view = convertView;
            resource = convertView.getId(); // for exception throwing
        }

        final Resources resources = context.getResources();
        try {
            final View casting;
            if (fieldId == 0) {
                // If no custom field is assigned, assume the whole resource is a TextView
                casting = view;
            } else {
                // Otherwise, find the TextView field within the view
                casting = view.findViewById(fieldId);
            }
            text = (TextView) casting;
            Preconditions.checkNotNull(text, "Resource {%s:%s} not found inside view {%s:%s}",
                    fieldId, new LazyResName(resources, fieldId), resource, new LazyResName(resources, resource));
        } catch (ClassCastException e) {
            logger.error("ViewProvidingBaseAdapter: convert view is not a TextView and field resource id wasn't provided");
            if (fieldId == 0) {
                throw new IllegalStateException("ViewProvidingBaseAdapter requires the resource {" + resource + ":"
                        + UiUtils.resolveResourceId(resources, resource) + "} to be a TextView or a fieldId provided", e);
            } else {
                throw new IllegalStateException("ViewProvidingBaseAdapter requires the fieldId resource {" + fieldId + ":"
                        + resources.getResourceName(fieldId) + "} to be a TextView or a fieldId provided", e);
            }
        }

        //noinspection unchecked
        T item = (T) adapter.getItem(position);
        if (presenter != null) {
            text.setText(presenter.getStringPresentation(item));
        } else {
            if (item instanceof CharSequence) {
                text.setText((CharSequence) item);
            } else {
                text.setText(item.toString());
            }
        }

        return view;
    }

    private static final class LazyResName {

        private final Resources resources;
        @AnyRes
        private final int resId;

        LazyResName(Resources resources, @AnyRes int resId) {
            this.resources = resources;
            this.resId = resId;
        }

        @Override
        public String toString() {
            return UiUtils.resolveResourceId(resources, resId);
        }

    }

}
