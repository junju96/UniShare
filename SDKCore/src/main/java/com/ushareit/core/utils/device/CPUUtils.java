package com.ushareit.core.utils.device;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import com.ushareit.core.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Locale;

public class CPUUtils {
    public static final String CPU_ARCHITECTURE_TYPE_32 = "32";
    public static final String CPU_ARCHITECTURE_TYPE_64_PROP = "64_prop";
    public static final String CPU_ARCHITECTURE_TYPE_64_CPUINFO = "64_cpu_info";

    private static final String CPU_ARCHITECTURE_KEY_64 = "ro.product.cpu.abilist64";

    private static final String PROC_CPU_INFO_PATH = "/proc/cpuinfo";

    public enum CPUType {
        X86("x86"), ARM("arm");

        private String mValue;
        CPUType(String value) {
            mValue = value;
        }

        @SuppressLint("DefaultLocale")
        public static CPUType fromString(String value) {
            if (!TextUtils.isEmpty(value)) {
                for (CPUType type : CPUType.values()) {
                    if (type.mValue.equals(value.toLowerCase()))
                        return type;
                }
            }
            return ARM;
        }
        @Override
        public String toString() {
            return mValue;
        }
    }

    public enum CPUArchType {
        A32("32"), A64("64");

        private String mValue;
        CPUArchType(String value) {
            mValue = value;
        }

        @SuppressLint("DefaultLocale")
        public static CPUArchType fromString(String value) {
            if (!TextUtils.isEmpty(value)) {
                for (CPUArchType type : CPUArchType.values()) {
                    if (type.mValue.equals(value.toLowerCase()))
                        return type;
                }
            }
            return A32;
        }
        @Override
        public String toString() {
            return mValue;
        }
        }

    /**
     * Check if the CPU architecture is x86
     */
    public static CPUType getCpuType() {
        return (getSystemProperty("ro.product.cpu.abi", "arm").contains("x86")) ? CPUType.X86 : CPUType.ARM;
    }

    /**
     * Get the CPU arch type: x32 or x64
     */
    public static String getArchTypeDetail(Context context) {
        if (getSystemProperty(CPU_ARCHITECTURE_KEY_64, "").length() > 0) {
            return CPU_ARCHITECTURE_TYPE_64_PROP;
        } else if (isCPUInfo64()) {
            return CPU_ARCHITECTURE_TYPE_64_CPUINFO;
        } else {
            return CPU_ARCHITECTURE_TYPE_32;
        }
    }

    public static CPUArchType getArchType(Context context) {
        if (getSystemProperty(CPU_ARCHITECTURE_KEY_64, "").length() > 0) {
            return CPUArchType.A64;
        } else if (isCPUInfo64()) {
            return CPUArchType.A64;
        } else {
            return CPUArchType.A32;
        }
    }

    public static int getProcesserCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    private static String getSystemProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> clazz= Class.forName("android.os.SystemProperties");
            Method get = clazz.getMethod("get", String.class, String.class);
            value = (String)(get.invoke(clazz, key, ""));
        } catch (Exception e) {}

        return value;
    }

    /**
     * Read the first line of "/proc/cpuinfo" file, and check if it is 64 bit.
     */
    private static boolean isCPUInfo64() {
        File cpuInfo = new File(PROC_CPU_INFO_PATH);
        if (cpuInfo != null && cpuInfo.exists()) {
            InputStream inputStream = null;
            BufferedReader bufferedReader = null;
            try {
                inputStream = new FileInputStream(cpuInfo);
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 512);
                String line = bufferedReader.readLine();
                if (line != null && line.length() > 0 && line.toLowerCase(Locale.US).contains("arch64")) {
                    return true;
                }
            } catch (Throwable t) {
            } finally {
                Utils.close(bufferedReader);
                Utils.close(inputStream);
            }
        }
        return false;
    }
}
