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

package ru.adios.budgeter;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import java8.util.function.Consumer;
import ru.adios.budgeter.util.BalancedMenuHandler;

/**
 * Created by Michail Kulikov
 * 11/7/15
 */
@UiThread
public abstract class FundsAwareMenuActivity extends AppCompatActivity {

    private BalancedMenuHandler menuHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getLayoutId());
        setSupportActionBar((Toolbar) findViewById(R.id.budgeter_toolbar));
        initMenuHandler();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (menuHandler == null) {
            initMenuHandler();
            onResumeOrRestart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (menuHandler == null) {
            initMenuHandler();
            onResumeOrRestart();
        } else {
            menuHandler.updateMenu(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(getMenuResource(), menu);
        if (menuHandler == null) {
            initMenuHandler();
        }
        menuHandler.onCreateMenu(menu);
        return true;
    }

    @LayoutRes
    protected abstract int getLayoutId();

    @MenuRes
    protected abstract int getMenuResource();

    @Override
    protected void onPause() {
        super.onPause();
        menuHandler.destroy();
        menuHandler = null;
    }

    private void initMenuHandler() {
        menuHandler = new BalancedMenuHandler(getResources(), getMenuHandlerListener());
        menuHandler.init(this);
    }

    protected Consumer<BalancesUiThreadState.Pair> getMenuHandlerListener() {
        return null;
    }

    protected void onResumeOrRestart() {}

}
