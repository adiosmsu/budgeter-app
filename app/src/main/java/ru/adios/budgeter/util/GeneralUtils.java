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

import org.threeten.bp.Clock;
import org.threeten.bp.ZoneOffset;

import java.lang.reflect.Array;
import java.util.List;

import javax.annotation.concurrent.Immutable;

/**
 * Created by Michail Kulikov
 * 10/8/15
 */
@Immutable
public final class GeneralUtils {

    public static <T> T[] arrayPlusValue(T[] original, T newValue) {
        final int l = original.length;
        final T[] ts = newArrayInstance(newValue, l + 1);
        System.arraycopy(original, 0, ts, 0, l);
        ts[l] = newValue;
        return ts;
    }

    public static <T> T[] listPlusValueAsArray(List<T> original, T newValue) {
        final int s = original.size();
        final T[] ts = original.toArray(newArrayInstance(newValue, s + 1));
        ts[s] = newValue;
        return ts;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] newArrayInstance(Class<T> arrayClass, int length) {
        if (arrayClass.equals(String.class)) { //expand if needed
            return (T[]) new String[length];
        } else {
            return (T[]) Array.newInstance(arrayClass, length); // heavy JNI call
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] newArrayInstance(T object, int length) {
        return newArrayInstance((Class<T>) object.getClass(), length);
    }

    public static ZoneOffset getLocalZoneOffset() {
        return ZoneOffset.systemDefault().getRules().getOffset(Clock.systemDefaultZone().instant());
    }

    private GeneralUtils() {}

}
