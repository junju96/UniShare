package com.ushareit.core.utils.ui;

import android.content.Context;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.ushareit.core.Logger;

public class DirectionDetector implements OnTouchListener {

    public enum DirectionType {
        UP(0), DOWN(1), LEFT(2), RIGHT(3);

        public static final String TAG = "DirectionType";
        private int mValue;

        DirectionType(int value) {
            mValue = value;
        }

        private static SparseArray<DirectionType> mValues = new SparseArray<>();

        static {
            for (DirectionType item : DirectionType.values())
                mValues.put(item.mValue, item);
        }

        public static DirectionType fromInt(int value) {
            return mValues.get(Integer.valueOf(value));
        }

        public int toInt() {
            return mValue;
        }
    }

    public enum QuadrantType {
        FIRST(1), SECOND(2), THIRD(3), FORTH(4);

        public static final String TAG = "QuadrantType";
        private int mValue;

        QuadrantType(int value) {
            mValue = value;
        }

        private static SparseArray<QuadrantType> mValues = new SparseArray<>();

        static {
            for (QuadrantType item : QuadrantType.values())
                mValues.put(item.mValue, item);
        }

        public static QuadrantType fromInt(int value) {
            return mValues.get(Integer.valueOf(value));
        }

        public int toInt() {
            return mValue;
        }
    }

    public interface OnDirectionListener {

        void onDirection(DirectionType type);

        void onSingleTap(QuadrantType type);

        void onDoubleTap(QuadrantType type);
    }

    public static final int DIRECTION_MODE_UP_DOWN = 0;
    public static final int DIRECTION_MODE_LEFT_RIGHT = 1;
    public static final int DIRECTION_MODE_ALL = 2;

    private static final String TAG = "DirectionDetector";
    private static final int DIRECTION_MIN_DISTANCE = 50;

    private Context mContext;
    private int mDirectionMode;
    private int mUpPercentage = 50;
    private int mLeftPercentage = 50;
    private View mView;

    private GestureDetector mGestureDetector;
    private OnDirectionListener mListener;
    private boolean mIsPause;

    public DirectionDetector(Context context, int directionMode, View view) {
        mContext = context;
        mDirectionMode = directionMode;
        mView = view;
        mView.setOnTouchListener(this);
        mGestureDetector = new GestureDetector(mContext, mGestureListener);
        resume();
    }

    public void pause() {
        mIsPause = true;
    }

    public void resume() {
        mIsPause = false;
    }

    public void setDirectionListener(OnDirectionListener listener) {
        mListener = listener;
    }

    public void setQuadrantPercentage(int upPercentage, int leftPercentage) {
        if (upPercentage >= 0 && upPercentage <= 100)
            mUpPercentage = upPercentage;
        if (leftPercentage >= 0 && leftPercentage <= 100)
            mLeftPercentage = leftPercentage;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mView != v || mIsPause)
            return false;

        return mGestureDetector.onTouchEvent(event);
    }

    private DirectionType getDirectionType(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float deltaX = Math.abs(e2.getX() - e1.getX());
        float deltaY = Math.abs(e2.getY() - e1.getY());
        if (deltaX < DIRECTION_MIN_DISTANCE && deltaY < DIRECTION_MIN_DISTANCE) {
            Logger.d(TAG, "getDirectionType(): Fling distance is too short[deltaX = " + deltaX + ", deltaY = " + deltaY + "]");
            return null;
        }

        if (mDirectionMode == DIRECTION_MODE_UP_DOWN) {
            if (velocityY > 0)
                return DirectionType.DOWN;
            else
                return DirectionType.UP;
        } else if (mDirectionMode == DIRECTION_MODE_LEFT_RIGHT) {
            if (velocityX > 0)
                return DirectionType.RIGHT;
            else
                return DirectionType.LEFT;
        } else {
            if (Math.abs(velocityX) - Math.abs(velocityY) > 0) {
                if (velocityX > 0)
                    return DirectionType.RIGHT;
                else
                    return DirectionType.LEFT;
            } else {
                if (velocityY > 0)
                    return DirectionType.DOWN;
                else
                    return DirectionType.UP;
            }
        }
    }

    private QuadrantType getQuadrantType(float x, float y) {
        int centerX = mView.getWidth() * mLeftPercentage / 100;
        int centerY = mView.getHeight() * mUpPercentage / 100;

        if (x < centerX) {
            if (y < centerY)
                return QuadrantType.SECOND;
            else
                return QuadrantType.THIRD;
        } else {
            if (y < centerY)
                return QuadrantType.FIRST;
            else
                return QuadrantType.FORTH;
        }
    }

    private void notifyDirection(DirectionType type) {
        if (mListener != null)
            mListener.onDirection(type);
    }

    private void notifySingleTap(QuadrantType type) {
        if (mListener != null)
            mListener.onSingleTap(type);
    }

    private void notifyDoubleTap(QuadrantType type) {
        if (mListener != null)
            mListener.onDoubleTap(type);
    }

    private SimpleOnGestureListener mGestureListener = new SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Logger.d(TAG, "onSingleTapUp: " + e.getAction());
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            DirectionType type = getDirectionType(e1, e2, velocityX, velocityY);
            if (type == null)
                return true;

            Logger.d(TAG, "onFling(): DirectionType = " + type.toInt());
            notifyDirection(type);
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            QuadrantType type = getQuadrantType(e.getX(), e.getY());
            Logger.d(TAG, "onSingleTapConfirmed(): X:" + e.getX() + ", Y:" + e.getY() + ", Quadrant:" + type.toInt());
            notifySingleTap(type);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            QuadrantType type = getQuadrantType(e.getX(), e.getY());
            Logger.d(TAG, "onDoubleTap(): X:" + e.getX() + ", Y:" + e.getY() + ", Quadrant:" + type.toInt());
            notifyDoubleTap(type);
            return true;
        }
    };
}
