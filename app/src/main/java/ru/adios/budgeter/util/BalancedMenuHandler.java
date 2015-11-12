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

package ru.adios.budgeter.util;

import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.UiThread;
import android.view.Menu;

import org.joda.money.Money;

import java8.util.Optional;
import java8.util.function.Consumer;
import ru.adios.budgeter.BalancesUiThreadState;

/**
 * Created by Michail Kulikov
 * 10/14/15
 */
@UiThread
public final class BalancedMenuHandler {

    private Money tbSnap;
    private Consumer<BalancesUiThreadState.Pair> balancesListener;

    private final Resources resources;

    private final Optional<Consumer<BalancesUiThreadState.Pair>> innerListener;

    public BalancedMenuHandler(Resources resources, Consumer<BalancesUiThreadState.Pair> innerListener) {
        this.innerListener = Optional.ofNullable(innerListener);
        this.resources = resources;
    }

    public BalancedMenuHandler(Resources resources) {
        this(resources, null);
    }

    public void init(final Activity activity) {
        balancesListener = new Consumer<BalancesUiThreadState.Pair>() {
            @Override
            public void accept(BalancesUiThreadState.Pair pair) {
                if (innerListener.isPresent()) {
                    innerListener.get().accept(pair);
                }

                tbSnap = pair.totalBalance;
                activity.invalidateOptionsMenu();
            }
        };
        BalancesUiThreadState.registerListener(balancesListener);
    }

    public void onCreateMenu(Menu menu) {
        if (tbSnap == null) {
            tbSnap = BalancesUiThreadState.totalBalance;
        }
        UiUtils.fillStandardMenu(menu, tbSnap, resources);
    }

    public void updateMenu(Activity activity) {
        final Money newSnap = BalancesUiThreadState.totalBalance;
        if (!newSnap.equals(tbSnap)) {
            tbSnap = newSnap;
            activity.invalidateOptionsMenu();
        }
    }

    public void destroy() {
        BalancesUiThreadState.removeListener(balancesListener);
    }

}
