package ru.adios.budgeter.util;

import android.content.Context;
import android.widget.TextView;

/**
 * Created by Michail Kulikov
 * 10/25/15
 */
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

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
