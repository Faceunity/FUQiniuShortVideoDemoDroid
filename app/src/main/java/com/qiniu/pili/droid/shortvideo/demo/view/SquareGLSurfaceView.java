package com.qiniu.pili.droid.shortvideo.demo.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

public class SquareGLSurfaceView extends GLSurfaceView {
    private static final String TAG = "MatchWidthView";

    public SquareGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        Log.i(TAG, "specify width mode:" + MeasureSpec.toString(widthMeasureSpec) + " size:" + width);
        Log.i(TAG, "specify height mode:" + MeasureSpec.toString(heightMeasureSpec) + " size:" + height);

        int finalWidth = 0;
        int finalHeight = 0;

        // width
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            finalWidth = 0;
        }
        if (widthMode == MeasureSpec.AT_MOST) {
            finalWidth = width;
        }
        if (widthMode == MeasureSpec.EXACTLY) {
            finalWidth = width;
        }

        // height
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            finalHeight = 0;
        }
        if (heightMode == MeasureSpec.AT_MOST) {
            finalHeight = Math.min(finalWidth, height);
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            finalHeight = height;
        }

        Log.i(TAG, "final width:" + finalWidth + " height:" + finalHeight);
        setMeasuredDimension(finalWidth, finalHeight);
    }
}
