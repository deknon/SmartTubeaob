package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.misc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * YouTube's "Most replayed" graph, drawn above the seek bar and spanning the same width.
 */
public class HeatmapView extends View {
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();
    private float[] mIntensities;

    public HeatmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setColor(Color.argb(0x80, 0xFF, 0xFF, 0xFF));
        mPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * @param intensities equally spaced buckets ordered by time, each in [0, 1]; null clears the graph
     */
    public void setIntensities(float[] intensities) {
        mIntensities = intensities;
        updatePath();
    }

    public boolean hasIntensities() {
        return mIntensities != null && mIntensities.length > 1;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updatePath();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (hasIntensities()) {
            canvas.drawPath(mPath, mPaint);
        }
    }

    /**
     * Curve through the bucket centers, smoothed with cubic segments whose control points sit halfway
     * between neighbours, so peaks stay rounded instead of spiky.
     */
    private void updatePath() {
        mPath.reset();

        if (!hasIntensities() || getWidth() == 0 || getHeight() == 0) {
            invalidate();
            return;
        }

        int count = mIntensities.length;
        float width = getWidth();
        float height = getHeight();
        float bucketWidth = width / count;

        mPath.moveTo(0, height);
        mPath.lineTo(0, toY(mIntensities[0], height));

        float prevX = bucketWidth / 2;
        float prevY = toY(mIntensities[0], height);
        mPath.lineTo(prevX, prevY);

        for (int i = 1; i < count; i++) {
            float x = bucketWidth * i + bucketWidth / 2;
            float y = toY(mIntensities[i], height);
            float midX = (prevX + x) / 2;
            mPath.cubicTo(midX, prevY, midX, y, x, y);
            prevX = x;
            prevY = y;
        }

        mPath.lineTo(width, prevY);
        mPath.lineTo(width, height);
        mPath.close();

        invalidate();
    }

    private static float toY(float intensity, float height) {
        return height - Math.max(0, Math.min(intensity, 1)) * height;
    }
}
