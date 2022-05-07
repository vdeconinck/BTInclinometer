package info.deconinck.inclinometer.view;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static android.graphics.Color.YELLOW;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import info.deconinck.inclinometer.DataMonitorActivity;
import info.deconinck.inclinometer.util.SharedUtil;

public class InclinometerView extends View {
    public static final int MAX_ROLL = 42;
    public static final int MAX_TILT = 42;

    private Bitmap backgroundImage;

    private Paint staticLinePaint, staticTextPaint;
    private Paint dynamicRollLinePaint, dynamicRollTextPaint;
    private Paint dynamicTiltRectPaint, dynamicTiltTextPaint;
    private Paint debugTextPaint, connectionStatusTextPaint;
    private Paint bitmapPaint;

    private float[] angleArray;
    private String connectionStatus;
    private int orientation;
    private float centerX;
    private float centerY;
    private float radius;
    private float tiltCompensationAngle, rollCompensationAngle;
    private boolean showDebug = false;

    public InclinometerView(Context context) {
        this(context, null);
    }

    public InclinometerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        staticLinePaint = new Paint();
        staticLinePaint.setColor(Color.WHITE);
        staticLinePaint.setStyle(Paint.Style.STROKE);
        staticLinePaint.setAntiAlias(true);
        staticLinePaint.setDither(true);
        staticLinePaint.setStrokeWidth(2f);

        staticTextPaint = new Paint();
        staticTextPaint.setColor(Color.WHITE);
        staticTextPaint.setAntiAlias(true);
        staticTextPaint.setDither(true);


        dynamicRollLinePaint = new Paint();
        dynamicRollLinePaint.setStyle(Paint.Style.STROKE);
        dynamicRollLinePaint.setAntiAlias(true);
        dynamicRollLinePaint.setDither(true);
        dynamicRollLinePaint.setStrokeWidth(4f);

        dynamicRollTextPaint = new Paint();
        dynamicRollTextPaint.setAntiAlias(true);

        dynamicTiltRectPaint = new Paint();
        dynamicTiltRectPaint.setStyle(Paint.Style.FILL);
        dynamicTiltRectPaint.setAntiAlias(true);
        dynamicTiltRectPaint.setDither(true);
        dynamicTiltRectPaint.setStrokeWidth(2f);

        dynamicTiltTextPaint = new Paint();
        dynamicTiltTextPaint.setAntiAlias(true);

        debugTextPaint = new Paint();
        debugTextPaint.setTextSize(40);
        debugTextPaint.setColor(Color.YELLOW);
        debugTextPaint.setAntiAlias(true);

        connectionStatusTextPaint = new Paint();
        connectionStatusTextPaint.setTextSize(40);
        connectionStatusTextPaint.setColor(Color.LTGRAY);
        connectionStatusTextPaint.setAntiAlias(true);

        bitmapPaint = new Paint();
        bitmapPaint.setDither(true);
        bitmapPaint.setFilterBitmap(true);

        try {
            rollCompensationAngle = SharedUtil.getFloat(DataMonitorActivity.ROLL_COMPENSATION_ANGLE_KEY);
        }
        catch (Exception e) {
            rollCompensationAngle = 0;
        }
        try {
            tiltCompensationAngle = SharedUtil.getFloat(DataMonitorActivity.TILT_COMPENSATION_ANGLE_KEY);
        }
        catch (Exception e) {
            tiltCompensationAngle = 0;
        }
    }

    public void setAngleArray(float[] angleArray) {
        this.angleArray = angleArray;
        invalidate();
    }

    public void setTiltCompensationAngle(float tiltCompensationAngle) {
        this.tiltCompensationAngle = tiltCompensationAngle;
    }

    public void setRollCompensationAngle(float rollCompensationAngle) {
        this.rollCompensationAngle = rollCompensationAngle;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
        invalidate();
    }

    public boolean isShowDebug() {
        return showDebug;
    }

    public void setShowDebug(boolean showDebug) {
        this.showDebug = showDebug;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Remember orientation
            orientation = newConfig.orientation;
            // Force computing of new background
            backgroundImage = null;
            // Force redraw
            invalidate();
        }
    }

    private void initBackground(Canvas template) {
        // Compute some values
        int width = template.getWidth();
        int height = template.getHeight();
        centerX = width / 2f;
        centerY = height / 2f;
        radius = 0.45f * Math.min(width, height);

        // Also prepare angle font sizes (relative to circle size)
        staticTextPaint.setTextSize(radius / 10);
        dynamicRollTextPaint.setTextSize(radius / 5);
        dynamicTiltTextPaint.setTextSize(radius / 5);

        // Prepare a new background image
        backgroundImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // Take its Canvas
        Canvas backgroundCanvas = new Canvas(backgroundImage);
        // And draw the static parts on it
        // Fill background
        backgroundCanvas.drawColor(Color.BLACK);

        // Remember orientation
        backgroundCanvas.save();

        // Shift
        backgroundCanvas.translate(centerX, centerY);

        // Main circle
        backgroundCanvas.drawCircle(0, 0, radius, staticLinePaint);
        // 0° reference
        backgroundCanvas.drawLine(-radius / 2, 0, -radius, 0, staticLinePaint);
        backgroundCanvas.drawLine(radius / 2, 0, radius, 0, staticLinePaint);

        // Angle ticks
        int startAngle = -50;
        int angleStep = 5;
        int stopAngle = 50;
        backgroundCanvas.rotate(startAngle);

        for (int currentAngle = startAngle; currentAngle <= stopAngle; currentAngle += angleStep) {
            backgroundCanvas.drawLine(-radius * 0.9f, 0, -radius, 0, staticLinePaint);
            backgroundCanvas.drawLine(radius * 0.9f, 0, radius, 0, staticLinePaint);
            if (currentAngle % 10 == 0) {
                String text = Math.abs(currentAngle) + "°";
                // Text dimensions: See https://stackoverflow.com/a/42091739
                Rect bounds = new Rect();
                staticTextPaint.getTextBounds(text, 0, text.length(), bounds);
                backgroundCanvas.drawText(text, -radius * 0.85f, bounds.height() / 2f, staticTextPaint);
                backgroundCanvas.drawText(text, radius * 0.85f - bounds.width(), bounds.height() / 2f, staticTextPaint);
            }
            backgroundCanvas.rotate(angleStep);
        }

        // Restore orientation
        backgroundCanvas.restore();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw the static background, (re-)creating it if needed
        if (backgroundImage == null) {
            initBackground(canvas);
        }
        canvas.drawBitmap(backgroundImage, 0, 0, bitmapPaint);

        if (angleArray != null) {
            // Angle values have been received, draw dynamic contents
            float roll = angleArray[0] - rollCompensationAngle;
            float tilt = angleArray[1] - tiltCompensationAngle;

            // Roll color
            int rollColor = GREEN;
            if (Math.abs(roll) > MAX_ROLL * .7f) {
                rollColor = YELLOW;
                if (Math.abs(roll) > MAX_ROLL * .9f) {
                    rollColor = RED;
                }
            }
            dynamicRollLinePaint.setColor(rollColor);
            dynamicRollTextPaint.setColor(rollColor);

            // Tilt color
            int tiltColor = GREEN;
            if (Math.abs(tilt) > MAX_TILT * .7f) {
                tiltColor = YELLOW;
                if (Math.abs(tilt) > MAX_TILT * .9f) {
                    tiltColor = RED;
                }
            }
            dynamicTiltTextPaint.setColor(tiltColor);
            dynamicTiltRectPaint.setColor(tiltColor);


            // 1. Draw roll
            // Remember orientation
            canvas.save();

            canvas.translate(centerX, centerY);
            canvas.rotate(roll);

            // Draw current roll lines
            canvas.drawLine(-radius / 2, 0, -radius, 0, dynamicRollLinePaint);
            canvas.drawLine(radius / 2, 0, radius, 0, dynamicRollLinePaint);

            // Draw current roll value
            String text = Math.round(Math.abs(roll)) + "°";
            // Text dimensions: See https://stackoverflow.com/a/42091739
            Rect bounds = new Rect();
            dynamicRollTextPaint.getTextBounds(text, 0, text.length(), bounds);
            if (roll > 0) {
                canvas.drawText(text, -radius * 0.7f, -bounds.height() * 0.1f, dynamicRollTextPaint);
            }
            else {
                canvas.drawText(text, radius * 0.7f - bounds.width(), -bounds.height() * 0.1f, dynamicRollTextPaint);
            }

            // Restore orientation
            canvas.restore();

            // 2. Draw tilt
            // Remember orientation
            canvas.save();

            canvas.translate(centerX, centerY);

            float sinY = (float) Math.sin(Math.toRadians(tilt));
            canvas.drawRect(-radius / 3, 0, radius / 3, radius * sinY, dynamicTiltRectPaint);

            // Draw current tilt value
            text = Math.round(Math.abs(tilt)) + "°";
            // Text dimensions: See https://stackoverflow.com/a/42091739
            dynamicRollTextPaint.getTextBounds(text, 0, text.length(), bounds);
            if (tilt > 0) {
                canvas.drawText(text, -bounds.width() / 2f, -bounds.height() * 0.1f, dynamicTiltTextPaint);
            }
            else {
                canvas.drawText(text, -bounds.width() / 2f, bounds.height() * 1.1f, dynamicTiltTextPaint);
            }

            // Restore orientation
            canvas.restore();

            // 3. Connection status
            connectionStatusTextPaint.getTextBounds(connectionStatus, 0, connectionStatus.length(), bounds);
            canvas.drawText(connectionStatus, canvas.getWidth() - bounds.width() - 5, canvas.getHeight() - 5, connectionStatusTextPaint);

            if (showDebug) {
                // 4. Debug
                canvas.drawText("" + roll + " ; " + tilt + " ; " + angleArray[2], 0, canvas.getHeight() - 5, debugTextPaint);
            }
        }

// double-buffering ?
//        canvas.save();
//        myCanvas.drawColor(Color.BLACK);
//        myCanvas.drawLine(0,0,100,100, linePaint);
//        canvas.drawBitmap(myBitmap, 0, 0, bitmapPaint);
//        canvas.restore();
    }
}
