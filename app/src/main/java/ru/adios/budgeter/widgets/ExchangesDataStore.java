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

package ru.adios.budgeter.widgets;

import android.content.res.Resources;
import android.support.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;

import javax.annotation.concurrent.ThreadSafe;

import java8.util.function.Function;
import java8.util.stream.Stream;
import ru.adios.budgeter.BundleProvider;
import ru.adios.budgeter.R;
import ru.adios.budgeter.api.RepoOption;
import ru.adios.budgeter.api.data.CurrencyExchangeEvent;
import ru.adios.budgeter.util.Formatting;

/**
 * Created by Michail Kulikov
 * 11/7/15
 */
@ThreadSafe
public class ExchangesDataStore extends AbstractDataStore<CurrencyExchangeEvent> {

    public ExchangesDataStore(final Resources resources) {
        super(
                ImmutableList.of(
                        resources.getString(R.string.ah_ops_table_col_time),
                        resources.getString(R.string.ah_exchanges_table_col_bought),
                        resources.getString(R.string.ah_exchanges_table_col_sold),
                        resources.getString(R.string.ah_exchanges_table_col_b_acc),
                        resources.getString(R.string.ah_exchanges_table_col_s_acc),
                        resources.getString(R.string.ah_exchanges_table_col_rate)
                ),
                new Function<CurrencyExchangeEvent, Iterable<String>>() {
                    @Override
                    public Iterable<String> apply(CurrencyExchangeEvent event) {
                        return ImmutableList.of(
                                Formatting.toStringRusDateTimeShort(event.timestamp),
                                Formatting.toStringMoneyUsingSign(event.bought, resources),
                                Formatting.toStringMoneyUsingSign(event.sold, resources),
                                event.boughtAccount.name,
                                event.soldAccount.name,
                                Formatting.toStringExchangeRate(event.rate)
                        );
                    }
                }
        );
    }

    public ExchangesDataStore(ImmutableList<String> header, Function<CurrencyExchangeEvent, Iterable<String>> function) {
        super(header, function);
    }

    @Override
    @WorkerThread
    protected Stream<CurrencyExchangeEvent> getLoadingStream(RepoOption[] options) {
         return BundleProvider.getBundle()
                .currencyExchangeEvents()
                .streamExchangeEvents(options);
    }

    @Override
    @WorkerThread
    public int count() {
        return BundleProvider.getBundle().currencyExchangeEvents().countExchangeEvents();
    }

}
