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

import android.os.Handler;

import com.google.common.collect.ImmutableMap;

import org.joda.money.CurrencyUnit;

import javax.annotation.concurrent.Immutable;

import ru.adios.budgeter.api.Accounter;
import ru.adios.budgeter.api.Units;
import ru.adios.budgeter.services.RatesDelegatingBackgroundService;

/**
 * Created by Michail Kulikov
 * 9/24/15
 */
@Immutable
public final class Constants {

    public static final Handler MAIN_HANDLER = new Handler();

    public static final RatesDelegatingBackgroundService CURRENCIES_EXCHANGE_SERVICE;

    public static final Accounter ACCOUNTER = BundleProvider.getBundle().accounter();

    public static final String[] CURRENCIES_DROPDOWN = new String[] {
            Units.RUB.getCode(),
            CurrencyUnit.USD.getCode(),
            CurrencyUnit.EUR.getCode(),
            Units.BTC.getCode()
    };
    private static final ImmutableMap<String, Integer> CUR_DROP_POSITIONS;

    static {
        final ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        for (int i = 0; i < CURRENCIES_DROPDOWN.length; i++) {
            final String str = CURRENCIES_DROPDOWN[i];
            builder.put(str, i);
        }
        CUR_DROP_POSITIONS = builder.build();

        final CurrenciesExchangeService delegate = new CurrenciesExchangeService(
                BundleProvider.getBundle().getTransactionalSupport(),
                BundleProvider.getBundle().currencyRates(),
                BundleProvider.getBundle().accounter(),
                BundleProvider.getBundle().treasury(),
                ExchangeRatesLoader.createBtcLoader(BundleProvider.getBundle().treasury()),
                ExchangeRatesLoader.createCbrLoader(BundleProvider.getBundle().treasury())
        );
        delegate.executeInSameThread();
        CURRENCIES_EXCHANGE_SERVICE = new RatesDelegatingBackgroundService(delegate);
    }

    public static int getCurrencyDropdownPosition(CurrencyUnit unit) {
        final Integer pos = CUR_DROP_POSITIONS.get(unit.getCode());
        if (pos == null) {
            throw new IllegalArgumentException("Unit " + unit + " is not from currencies dropdown list");
        }
        return pos;
    }

    private Constants() {}

}
