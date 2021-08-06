package info.deconinck.bt901.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.Nullable;

public class InclinometerView extends View {
    private Bitmap myBitmap;
    private Canvas myCanvas;
    private Paint staticLinePaint, dynamicLinePaint, dynamicTextPaint, dynamicRectPaint;
    private Paint bitmapPaint;

    private float[] angleArray;

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
        
        dynamicLinePaint = new Paint();
        dynamicLinePaint.setColor(Color.GREEN);
        dynamicLinePaint.setStyle(Paint.Style.STROKE);
        dynamicLinePaint.setAntiAlias(true);
        dynamicLinePaint.setDither(true);
        dynamicLinePaint.setStrokeWidth(2f);

        dynamicTextPaint = new Paint();
        dynamicTextPaint.setTextSize(40);
        dynamicTextPaint.setColor(Color.GREEN);
        dynamicTextPaint.setAntiAlias(true);

        dynamicRectPaint = new Paint();
        dynamicRectPaint.setColor(Color.GREEN);
        dynamicRectPaint.setStyle(Paint.Style.FILL);
        dynamicRectPaint.setAntiAlias(true);
        dynamicRectPaint.setDither(true);
        dynamicRectPaint.setStrokeWidth(2f);

        bitmapPaint = new Paint(Paint.DITHER_FLAG);
    }

    public void init(DisplayMetrics metrics) {
        myBitmap = Bitmap.createBitmap(metrics.widthPixels, metrics.heightPixels, Bitmap.Config.ARGB_8888);
        myCanvas = new Canvas(myBitmap);
    }

    public void setAngleArray(float[] angleArray) {
        this.angleArray = angleArray;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO compute background once and keep it
        float centerX = canvas.getWidth() / 2f;
        float centerY = canvas.getHeight() / 2f;
        float radius = 0.45f * Math.min(canvas.getWidth(), canvas.getHeight());

        canvas.drawColor(Color.BLACK);
        canvas.drawCircle(centerX, centerY, radius, staticLinePaint);
        canvas.drawLine(centerX - radius,centerY,centerX - radius/2,centerY, staticLinePaint);
        canvas.drawLine(centerX + radius/2,centerY,centerX + radius,centerY, staticLinePaint);

        if (angleArray != null) {
            // Angle values have been received, draw dynamic contents
            float cosX = (float) Math.cos(Math.toRadians(-angleArray[0]));
            float sinX = (float) Math.sin(Math.toRadians(-angleArray[0]));
            float sinY = (float) Math.sin(Math.toRadians(angleArray[1]));
            
            canvas.drawLine(
                    centerX - radius/2 * cosX,
                    centerY - radius/2 * sinX,
                    centerX - radius * cosX,
                    centerY - radius * sinX,
                    dynamicLinePaint
            );
            canvas.drawLine(
                    centerX + radius/2 * cosX,
                    centerY + radius/2 * sinX,
                    centerX + radius * cosX,
                    centerY + radius * sinX,
                    dynamicLinePaint
            );
            
            canvas.drawRect(
                    centerX - radius/3, 
                    centerY, 
                    centerX + radius/3, 
                    centerY + radius * sinY,
                    dynamicRectPaint);
                    
            canvas.drawText("" + angleArray[0] + " - " + angleArray[1] + " - " + angleArray[2], 0, 60, dynamicTextPaint);
        }

// double-buffering ?
//        canvas.save();
//        myCanvas.drawColor(Color.BLACK);
//        myCanvas.drawLine(0,0,100,100, linePaint);
//        canvas.drawBitmap(myBitmap, 0, 0, bitmapPaint);
//        canvas.restore();
    }
}
