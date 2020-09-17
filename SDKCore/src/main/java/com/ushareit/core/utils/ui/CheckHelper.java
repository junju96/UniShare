package com.ushareit.core.utils.ui;

import com.ushareit.core.lang.ObjectExtras;

public final class CheckHelper {
    private static final String CONTENT_EXTRA_CHECK_TYPE = "check_type";

    public static final int CHECK_TYPE_UNCHECK = 0;
    public static final int CHECK_TYPE_CHECKED = 1;

    private static final String CONTENT_EXTRA_CHECK_ENABLE = "check_enable";

    private CheckHelper() {
    }

    public static void setChecked(ObjectExtras object, boolean checked) {
        object.putExtra(CONTENT_EXTRA_CHECK_TYPE, checked ? CHECK_TYPE_CHECKED : CHECK_TYPE_UNCHECK);
    }

    public static boolean isChecked(ObjectExtras object) {
        return (object.getIntExtra(CONTENT_EXTRA_CHECK_TYPE, CHECK_TYPE_UNCHECK) == CHECK_TYPE_CHECKED);
    }

    public static void enableCheck(ObjectExtras object, boolean enabled) {
        object.putExtra(CONTENT_EXTRA_CHECK_ENABLE, enabled);
    }

    public static boolean isCheckEnable(ObjectExtras object) {
        return object.getBooleanExtra(CONTENT_EXTRA_CHECK_ENABLE, true);
    }
}