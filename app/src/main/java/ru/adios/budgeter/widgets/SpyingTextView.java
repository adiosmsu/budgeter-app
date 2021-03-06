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

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.UiThread;
import android.widget.TextView;

/**
 * Created by Michail Kulikov
 * 10/25/15
 */
@UiThread
public class SpyingTextView extends TextView {

    private Runnable heightRunnable;

    public boolean heightCatch = false;

    public SpyingTextView(Context context) {
        super(context);
    }

    public void setHeightRunnable(Runnable heightRunnable) {
        this.heightRunnable = heightRunnable;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        listenForHeight();
        super.onDraw(canvas);
    }

    private void listenForHeight() {
        if (heightCatch) {
            final int heightView = getHeight();
            if (heightView > 0) {
                if (heightRunnable != null) {
                    heightRunnable.run();
                }
                heightCatch = false;
            }
        }
    }

}
