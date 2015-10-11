package ru.adios.budgeter.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Created by Michail Kulikov
 * 10/10/15
 */
public class SquareByHeightButton extends Button {

    public SquareByHeightButton(Context context) {
        super(context);
    }

    public SquareByHeightButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareByHeightButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int h = this.getMeasuredHeight();
        setMeasuredDimension(h, h);
    }


}
