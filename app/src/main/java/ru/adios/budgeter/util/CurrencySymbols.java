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

import android.content.res.Resources;

import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.concurrent.ThreadSafe;

import java8.util.Optional;

/**
 * Created by Michail Kulikov
 * 11/4/15
 */
@ThreadSafe
public final class CurrencySymbols {

    private static final class MapLazySingletonHolder {

        private static final ConcurrentHashMap<CurrencyUnit, SymInfo> INFO_MAP = constructInfoMap();

    }

    private static volatile Resources res;
    private static final Logger logger = LoggerFactory.getLogger(CurrencySymbols.class);


    public static Optional<SymInfo> getSymbolicInfo(CurrencyUnit unit, Resources resources) {
        if (res == null) {
            res = resources;
        }

        return Optional.ofNullable(MapLazySingletonHolder.INFO_MAP.get(unit));
    }

    private static ConcurrentHashMap<CurrencyUnit, SymInfo> constructInfoMap() {
        final ConcurrentHashMap<CurrencyUnit, SymInfo> map = new ConcurrentHashMap<>(CurrencyUnit.registeredCurrencies().size() + 1, 1.0f, 1);

        if (res == null) {
            logger.error("No resources provided");
        } else {
            InputStream in = null;
            try {
                in = res.getAssets().open(INFO_CSV_ASSET);
                if (in == null) {
                    throw new FileNotFoundException("Data file " + INFO_CSV_ASSET + " not found");
                }

                final BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = REGEX_LINE.matcher(line);
                    if (matcher.matches()) {
                        final String currencyCode = matcher.group(1);

                        final CurrencyUnit of = CurrencyUnit.of(currencyCode);
                        if (of == null) {
                            continue;
                        }

                        final String sym = matcher.group(2);
                        final boolean afAm = matcher.group(3).equals("1");

                        map.put(of, new SymInfo(sym, afAm));
                    }
                }
            } catch (Exception ex) {
                logger.error("Exception while filling up currencies symbols map", ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException lgd) {
                        logger.error("Exception while closing asset stream for currencies symbols map", lgd);
                    }
                }
            }
        }

        return map;
    }

    private static final Pattern REGEX_LINE = Pattern.compile("([A-Z]{3}),(.{1,4}),([01])#?.*");
    private static final String INFO_CSV_ASSET = "currencies_symbolic.csv";

    public static final class SymInfo {

        public final String sym;
        public final boolean afterAmount;

        private SymInfo(String sym, boolean afterAmount) {
            this.afterAmount = afterAmount;
            this.sym = sym;
        }

    }

    private CurrencySymbols() { }

}
