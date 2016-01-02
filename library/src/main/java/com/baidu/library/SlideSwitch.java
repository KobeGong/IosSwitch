package com.baidu.library;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;

/**
 * Created by gonggaofeng on 15/12/24.
 *
 */
public class SlideSwitch extends View {

    public static final int SHAPE_RECT = 1;
    public static final int SHAPE_CIRCLE = 2;

    private int onColor;
    private int offColor;
    private int thumbColor;
    private int onThumbColor;
    private int offThumbColor;
    private boolean checked;
    private int shape;
    private Paint paint;
    private RectF offStateRect;
    private RectF thumbRect;
    private RectF onStateRect;
    private int alpha;
    private int mMaxLeftOffset;
    private int mLeftOffset;
    private int eventStartX;
    private boolean mSlide = true;
    private static final float sStrokeWidth = Resources.getSystem().getDisplayMetrics().density;
    private SlideListener listener;
    private static float sScaledTouchSlot;

    public interface SlideListener {
        void open(SlideSwitch slideSwitch);

        void close(SlideSwitch slideSwitch);
    }

    public SlideSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        listener = null;
        paint = new Paint();
        paint.setAntiAlias(true);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SlideSwitch);
        int onColor = a.getColor(R.styleable.SlideSwitch_onColor,
                Color.parseColor("#FF00EE00"));
        setOnColor(onColor);
        int offColor = a.getColor(R.styleable.SlideSwitch_offColor, Color.GRAY);
        setOffColor(offColor);
        int onThumbColor = a.getColor(R.styleable.SlideSwitch_onThumbColor, Color.WHITE);
        setOnThumbColor(onThumbColor);
        offThumbColor = a.getColor(R.styleable.SlideSwitch_offThumbColor, Color.WHITE);
        setOffThumbColor(offThumbColor);
        boolean checked = a.getBoolean(R.styleable.SlideSwitch_checked, false);
        setChecked(checked);
        boolean slide = a.getBoolean(R.styleable.SlideSwitch_slide, true);
        setSlide(slide);
        int shape = a.getInt(R.styleable.SlideSwitch_shape, SHAPE_RECT);
        setShapeType(shape);
        a.recycle();
        sScaledTouchSlot = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public SlideSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideSwitch(Context context) {
        this(context, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = measureDimension(280, widthMeasureSpec);
        int height = measureDimension(140, heightMeasureSpec);
        if (shape == SHAPE_CIRCLE) {
            if (width < height)
                width = height * 2;
        }
        setMeasuredDimension(width, height);
        initDrawingVal();
    }

    public void initDrawingVal() {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        RectF viewRect = new RectF(0, 0, width, height);
        onStateRect = new RectF(viewRect);
        thumbRect = new RectF(viewRect);
        offStateRect = new RectF(viewRect);
        float halfStrokeWidth = sStrokeWidth / 2;
        offStateRect.inset(halfStrokeWidth, halfStrokeWidth);
        if (shape == SHAPE_RECT) {
            mMaxLeftOffset = width / 2;
        } else {
            mMaxLeftOffset = (int) (width - viewRect.height());
        }
        if (checked) {
            mLeftOffset = mMaxLeftOffset;
            alpha = 255;
            thumbColor = onThumbColor;
        } else {
            mLeftOffset = 0;
            alpha = 0;
            thumbColor = offThumbColor;
        }
    }

    public int measureDimension(int defaultSize, int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = defaultSize;
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float inset = sStrokeWidth * 2;
        if (shape == SHAPE_RECT) {
            paint.setColor(offColor);
            offStateRect.set(onStateRect);
            canvas.drawRect(offStateRect, paint);
            paint.setColor(onColor);
            paint.setAlpha(alpha);
            canvas.drawRect(onStateRect, paint);
            thumbRect.set(mLeftOffset, 0, mLeftOffset + getWidth() / 2, getHeight());
            thumbRect.inset(inset, inset);
            paint.setColor(thumbColor);
            canvas.drawRect(thumbRect, paint);
        } else {
            float radius;
            radius = offStateRect.height() / 2;
            paint.setColor(offColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(sStrokeWidth);
            canvas.drawRoundRect(offStateRect, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(onColor);
            paint.setAlpha(alpha);
            canvas.drawRoundRect(onStateRect, radius, radius, paint);
            float right = getHeight();
            float bottom = getHeight();
            thumbRect.set(0, 0, right, bottom);
            thumbRect.offset(mLeftOffset, 0);
            thumbRect.inset(inset, inset);
            radius = thumbRect.height() / 2;
            paint.setColor(thumbColor);
            canvas.drawRoundRect(thumbRect, radius, radius, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mSlide)
            return super.onTouchEvent(event);
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                eventStartX = (int) event.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                int eventLastX = (int) event.getRawX();
                int diffX = eventLastX - eventStartX;
                int tempX = diffX + mLeftOffset;
                tempX = (tempX > mMaxLeftOffset ? mMaxLeftOffset : tempX);
                tempX = (tempX < 0 ? 0 : tempX);
                if (tempX >= 0 && tempX <= mMaxLeftOffset) {
                    mLeftOffset = tempX;
                    alpha = (int) (255 * (float) tempX / (float) mMaxLeftOffset);
                    computeThumbColor();
                    invalidateView();
                }
                break;
            case MotionEvent.ACTION_UP:
                int wholeX = (int) (event.getRawX() - eventStartX);
                boolean toRight;
                toRight = mLeftOffset > mMaxLeftOffset / 2;
                if (Math.abs(wholeX) < sScaledTouchSlot) {
                    toRight = !toRight;
                }
                moveToDest(toRight);
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * draw again
     */
    private void invalidateView() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }


    public void setSlideListener(SlideListener listener) {
        this.listener = listener;
    }

    public void moveToDest(final boolean toRight) {

        ValueAnimator toDestAnim = ValueAnimator.ofInt(mLeftOffset,
                toRight ? mMaxLeftOffset : 0);
        toDestAnim.setDuration(100);
        toDestAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        toDestAnim.start();
        toDestAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mLeftOffset = (Integer) animation.getAnimatedValue();
                alpha = (int) (255 * (mLeftOffset * 1f / mMaxLeftOffset));
                computeThumbColor();
                invalidateView();
            }
        });
        toDestAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (toRight) {
                    checked = true;
                    if (listener != null)
                        listener.open(SlideSwitch.this);
                } else {
                    checked = false;
                    if (listener != null)
                        listener.close(SlideSwitch.this);
                }
            }
        });
    }

    private void computeThumbColor() {
        int startColor = offThumbColor;
        int endColor = onThumbColor;
        int difAlpha = Color.alpha(endColor) - Color.alpha(startColor);
        int difRed = Color.red(endColor) - Color.red(startColor);
        int difGreen = Color.green(endColor) - Color.green(startColor);
        int difBlue = Color.blue(endColor) - Color.blue(startColor);
        float colorFraction = alpha * 1f / 255;
        int thumbAlpha = (int) (Color.alpha(startColor) + colorFraction * difAlpha);
        int thumbRed = (int) (Color.red(startColor) + colorFraction * difRed);
        int thumbGreen = (int) (Color.green(startColor) + colorFraction * difGreen);
        int thumbBlue = (int) (Color.blue(startColor) + colorFraction * difBlue);
        thumbColor = Color.argb(thumbAlpha, thumbRed, thumbGreen, thumbBlue);
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        initDrawingVal();
        invalidateView();
        if (listener != null)
            if (checked) {
                listener.open(SlideSwitch.this);
            } else {
                listener.close(SlideSwitch.this);
            }
    }

    public void setOnColor(int onColor) {
        this.onColor = onColor;
        invalidateView();
    }

    public void setOffColor(int offColor) {
        this.offColor = offColor;
        invalidateView();
    }

    public void setOnThumbColor(int onThumbColor) {
        this.onThumbColor = onThumbColor;
        invalidateView();
    }

    public void setOffThumbColor(int offThumbColor) {
        this.offThumbColor = offThumbColor;
        invalidateView();
    }

    public void setShapeType(int shapeType) {
        this.shape = shapeType;
        initDrawingVal();
        invalidateView();
    }

    public void setSlide(boolean mSlide) {
        this.mSlide = mSlide;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            this.checked = bundle.getBoolean("checked");
            state = bundle.getParcelable("instanceState");
        }
        super.onRestoreInstanceState(state);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putBoolean("checked", this.checked);
        return bundle;
    }
}