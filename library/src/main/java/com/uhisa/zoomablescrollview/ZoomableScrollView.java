package com.uhisa.zoomablescrollview;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.OverScroller;

import com.uhisa.zoomablescrollview.utils.Logger;
import com.uhisa.zoomablescrollview.utils.MotionEventUtil;

/**
 * Created by uhisa on 2018/04/02.
 */
public class ZoomableScrollView extends ViewGroup {

    private static final int INVALID_POINTER_ID = -1;

    private float mMaxScaleFactor = 3f;
    private boolean mCenter;

    private View mTargetView;
    private int mActivePointerId = INVALID_POINTER_ID;
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mFlingDetector;
    private float mScaleFactor = 1.f;
    private int mDefaultX;
    private int mDefaultY;
    private int mMaxX;
    private int mMinX;
    private int mMaxY;
    private int mMinY;
    private float mFirstTouchX;
    private float mFirstTouchY;
    private float mLastTouchX;
    private float mLastTouchY;
    private boolean mTouchIntercepted;
    private int mPosX;
    private int mPosY;
    private OverScroller mOverScroller;

    public ZoomableScrollView(Context context) {
        super(context);
        init(context, null);
    }

    public ZoomableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ZoomableScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ZoomableScrollView);
            mCenter = a.getBoolean(R.styleable.ZoomableScrollView_gravity_center, false);
            a.recycle();
        }

        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                Logger.d("onScale scaleF:%f, x:%f, y:%f",
                        detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());

                float scaleFactor = mScaleFactor * detector.getScaleFactor();
                if (scaleFactor < 1f || mMaxScaleFactor < scaleFactor) {
                    return false;
                }

                if (scaleFactor < 1.05f && mScaleFactor > scaleFactor) {
                    scaleFactor = 1f;
                } else if (scaleFactor > mMaxScaleFactor - 0.05f && mScaleFactor < scaleFactor) {
                    scaleFactor = mMaxScaleFactor;
                }
                mScaleFactor = Math.min(mMaxScaleFactor, Math.max(1f, scaleFactor));

                configurePosition();

                mTargetView.setPivotX(0);
                mTargetView.setPivotY(0);
                mTargetView.setScaleX(mScaleFactor);
                mTargetView.setScaleY(mScaleFactor);

                float x = detector.getFocusX() * (detector.getScaleFactor() - 1f);
                float y = detector.getFocusY() * (detector.getScaleFactor() - 1f);
                doLayout(mPosX * detector.getScaleFactor() - x, mPosY * detector.getScaleFactor() - y);

                return true;
            }
        });
        mFlingDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Logger.d("onFling x:%d, y:%d, vx:%f, vy:%f", mPosX, mPosY, velocityX, velocityY);
                mOverScroller.fling(mPosX, mPosY, (int) velocityX, (int) velocityY, mMinX, mMaxX, mMinY, mMaxY);
                ViewCompat.postInvalidateOnAnimation(ZoomableScrollView.this);
                return true;
            }
        });
        mOverScroller = new OverScroller(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (getChildCount() > 1) {
            throw new IllegalStateException("ZoomableScrollView can host only one direct child");
        }
        mTargetView = getChildAt(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(w, h);

        int wms = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        if (mTargetView.getLayoutParams().width == LayoutParams.MATCH_PARENT ||
                mTargetView.getLayoutParams().width == LayoutParams.FILL_PARENT) {
            wms = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
        }
        int hms = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        if (mTargetView.getLayoutParams().height == LayoutParams.MATCH_PARENT ||
                mTargetView.getLayoutParams().height == LayoutParams.FILL_PARENT) {
            hms = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
        }
        mTargetView.measure(wms, hms);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        configurePosition();
        doLayout(mDefaultX, mDefaultY);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean handled = false;

        mScaleDetector.onTouchEvent(ev);
        mFlingDetector.onTouchEvent(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                Logger.d("onTouchEvent[ACTION_DOWN]");

                mLastTouchX = ev.getX();
                mLastTouchY = ev.getY();
                mFirstTouchX = ev.getX();
                mFirstTouchY = ev.getY();
                mTouchIntercepted = false;

                mActivePointerId = ev.getPointerId(0);
                if (! mOverScroller.isFinished()) {
                    mOverScroller.abortAnimation();
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mActivePointerId == INVALID_POINTER_ID) {
                    break;
                }
                int pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex == -1) {
//                    Logger.d("Invalid pointerId=" + pointerIndex
//                            + " in onInterceptTouchEvent");
                    break;
                }
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);

                doLayout(mPosX - (mLastTouchX - x), mPosY - (mLastTouchY - y));

                mLastTouchX = x;
                mLastTouchY = y;

                if (mTouchIntercepted ||
                        (mFirstTouchX - 25 > x || mFirstTouchX + 25 < x ||
                                mFirstTouchY - 25 > y || mFirstTouchY + 25 < y)) {
                    mTouchIntercepted = handled = true;
                }
                break;
            }
        }
        Logger.d("onInterceptTouchEvent[%b] act:%s, x:%f, y:%f",
                handled, MotionEventUtil.actionToString(ev.getAction()),
                ev.getX(), ev.getY());
        return handled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Logger.d("onTouchEvent act:%s, x:%f, y:%f",
                MotionEventUtil.actionToString(ev.getAction()),
                ev.getX(), ev.getY());

        if (ev.getActionMasked() != MotionEvent.ACTION_DOWN) {
            mScaleDetector.onTouchEvent(ev);
            mFlingDetector.onTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
//            case MotionEvent.ACTION_DOWN: {
//                Logger.d("onTouchEvent[ACTION_DOWN]");
//                mLastTouchX = ev.getX();
//                mLastTouchY = ev.getY();
//                mActivePointerId = ev.getPointerId(0);
//                if (! mOverScroller.isFinished()) {
//                    mOverScroller.abortAnimation();
//                }
//                break;
//            }

            case MotionEvent.ACTION_MOVE: {
                if (mActivePointerId == INVALID_POINTER_ID) {
                    break;
                }
                int pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex == -1) {
//                    Logger.d("Invalid pointerId=" + pointerIndex
//                            + " in onInterceptTouchEvent");
                    break;
                }
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);

                doLayout(mPosX - (mLastTouchX - x), mPosY - (mLastTouchY - y));

                mLastTouchX = x;
                mLastTouchY = y;
                break;
            }

            case MotionEvent.ACTION_UP: {
                Logger.d("onTouchEvent[ACTION_UP]");
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                Logger.d("onTouchEvent[ACTION_CANCEL]");
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                Logger.d("onTouchEvent[ACTION_POINTER_UP]");
                final int pointerIndex = ev.getActionIndex();
                if (ev.getPointerId(pointerIndex) == mActivePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (mOverScroller.computeScrollOffset()) {
            Logger.d("computeScroll[computed] x:%d, y:%d", mOverScroller.getCurrX(), mOverScroller.getCurrY());
            doLayout(mOverScroller.getCurrX(), mOverScroller.getCurrY());
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private void doLayout(float x, float y) {
        mPosX = (int) Math.min(mMaxX, Math.max(mMinX, x));
        mPosY = (int) Math.min(mMaxY, Math.max(mMinY, y));
        Logger.d("doLayout s:%f, x:%f, y:%f ==> x:%d, y:%d", mScaleFactor, x, y, mPosX, mPosY);
        mTargetView.layout(mPosX, mPosY,
                mPosX + mTargetView.getMeasuredWidth(), mPosY + mTargetView.getMeasuredHeight());
    }

    private void configurePosition() {
        float width = getMeasuredWidth() - mTargetView.getMeasuredWidth() * mScaleFactor;
        float height = getMeasuredHeight() - mTargetView.getMeasuredHeight() * mScaleFactor;
        if (mCenter) {
            mDefaultX = (int) (width / 2);
            mDefaultY = (int) (height / 2);
            mMinX = (int) (-Math.max(0, -width) / 2) + mDefaultX;
            mMaxX = (int) (Math.max(0, -width) / 2) + mDefaultX;
            mMinY = (int) (-Math.max(0, -height) / 2) + mDefaultY;
            mMaxY = (int) (Math.max(0, -height) / 2) + mDefaultY;
        } else {
            mDefaultX = 0;
            mDefaultY = 0;
            mMinX = (int) (Math.min(0, width)) + mDefaultX;
            mMaxX = mDefaultX;
            mMinY = (int) (Math.min(0, height)) + mDefaultY;
            mMaxY = mDefaultY;
        }
        Logger.d("configurePosition min_x:%d, max_x:%d, min_y:%d, max_y:%d", mMinX, mMaxX, mMinY, mMaxY);
    }

    public void setMaxScaleFactor(float maxScaleFactor) {
        mMaxScaleFactor = maxScaleFactor;
    }
}
