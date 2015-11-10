package ru.adios.budgeter.util;

import android.text.Editable;
import android.text.TextWatcher;

import javax.annotation.concurrent.Immutable;

/**
 * Default empty implementations.
 *
 * Created by adios on 30.09.15.
 */
@Immutable
public abstract class EmptyTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {}
}
