package com.ushareit.core.utils.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;

public class AnimationUtils {

    private static final int DEFAULT_DURATION = 200;

    /**
     * 缓慢显示动画
     * @param view  动画目标View
     * @param duration 动画时长
     * @param listener 动画监听器
     */
    public static void fadeShow(final View view, int duration, AnimatorListenerAdapter listener) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        animator.setDuration(duration);
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
            }
        });
        if (listener != null)
            animator.addListener(listener);
        animator.start();
    }

    public static void fadeHide(View view, AnimatorListenerAdapter l) {
        fadeHide(view, DEFAULT_DURATION, l);
    }

    /**
     * 缓慢隐藏动画
     * @param view  动画目标View
     * @param duration 动画时长
     * @param listener 动画监听器
     */
    public static void fadeHide(final View view, int duration, AnimatorListenerAdapter listener) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        animator.setDuration(duration);
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.INVISIBLE);
            }
        });
        if (listener != null)
            animator.addListener(listener);
        animator.start();
    }

    /**
     * 旋转动画
     * @param view 动画目标View
     * @param value 每次旋转的角度
     */
    public static void rotate(View view, float value) {
        view.animate().setDuration(500).rotationBy(value);
    }

}
