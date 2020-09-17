package com.ushareit.core.utils.ui;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.ushareit.core.lang.ObjectStore;

public class SoftKeyboardUtils {

    /**
     * 显示软键盘
     * @param mContext
     * @param mEditView 目标编辑框
     */
    public static void showSoftKeyboardView(Context mContext, EditText mEditView) {
        if (mContext == null || mEditView == null)
            return;
        mEditView.requestFocus();
        InputMethodManager imm = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditView, 0);
    }

    /**
     * 隐藏软键盘
     * @param mContext
     * @param mEditView 目标编辑框
     */
    public static void hideSoftKeyboardView(Context mContext, EditText mEditView) {
        if (mContext == null || mEditView == null)
            return;
        InputMethodManager imm = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            mEditView.clearFocus();
            imm.hideSoftInputFromWindow(mEditView.getWindowToken(), 0);
        }
    }

    /**
     * 显示软键盘
     * @param view 目标View
     */
    public static void showSoftKeyBoard(View view) {
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) ObjectStore.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(view, 2);
        }
    }

    /**
     * 隐藏软键盘
     * @param view 目标View
     */
    public static void hideSoftKeyBoard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) ObjectStore.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
