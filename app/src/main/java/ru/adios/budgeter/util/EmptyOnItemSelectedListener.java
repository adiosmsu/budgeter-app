package ru.adios.budgeter.util;

import android.widget.AdapterView;

/**
 * Created by Michail Kulikov
 * 11/11/15
 */
public abstract class EmptyOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

}
