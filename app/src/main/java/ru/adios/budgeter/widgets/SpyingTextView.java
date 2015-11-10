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
