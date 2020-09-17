package com.ushareit.core.lang;

import android.content.Context;

import com.ushareit.core.Assert;
import com.ushareit.core.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

// TODO use Reflector to replace all reflect calls elsewhere

public final class Reflector {
    private static final String TAG = "Reflector";

    private Reflector() {}

    public static Object getFieldValue(Object bean, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Assert.notNull(bean);
        Assert.notNEWS(fieldName);

        Field field = bean.getClass().getDeclaredField(fieldName);
        boolean originAccessible = field.isAccessible();
        try {
            if (!originAccessible)
                field.setAccessible(true);
            return field.get(bean);
        } finally {
            field.setAccessible(originAccessible);
        }
    }

    public static Object getStaticFieldValue(Class bean, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Assert.notNull(bean);
        Assert.notNEWS(fieldName);

        Field field = bean.getDeclaredField(fieldName);
        boolean originAccessible = field.isAccessible();
        try {
            if (!originAccessible)
                field.setAccessible(true);
            return field.get(bean);
        } finally {
            field.setAccessible(originAccessible);
        }
    }

    public static Object getFieldValueQuietly(Object bean, String fieldName) {
        Assert.notNull(bean);
        Assert.notNEWS(fieldName);

        try {
            return getFieldValue(bean, fieldName);
        } catch (Exception e) {
            Logger.w(TAG, e);
            return null;
        }
    }

    public static boolean setFieldValue(Object bean, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Assert.notNull(bean);
        Assert.notNEWS(fieldName);

        Field field = bean.getClass().getDeclaredField(fieldName);
        boolean originAccessible = field.isAccessible();
        try {
            if (!originAccessible)
                field.setAccessible(true);
            field.set(bean, value);
            return true;
        } finally {
            field.setAccessible(originAccessible);
        }
    }

    public static boolean setFieldValueQuietly(Object bean, String fieldName, Object value) {
        Assert.notNull(bean);
        Assert.notNEWS(fieldName);

        try {
            return setFieldValue(bean, fieldName, value);
        } catch (Exception e) {
            Logger.w(TAG, e);
            return false;
        }
    }

    public static Object invokeMethod(Object bean, String methodName, Class<?>[] paramTypes, Object[] args)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Assert.notNull(bean);
        Assert.notNEWS(methodName);

        Method method = bean.getClass().getDeclaredMethod(methodName, paramTypes);
        boolean originAccessible = method.isAccessible();
        try {
            if (!originAccessible)
                method.setAccessible(true);
            return method.invoke(bean, args);
        } finally {
            method.setAccessible(originAccessible);
        }
    }

    public static Object invokeMethodQuietly(Object bean, String methodName, Class<?>[] paramTypes, Object[] args) {
        Assert.notNull(bean);
        Assert.notNEWS(methodName);

        try {
            return invokeMethod(bean, methodName, paramTypes, args);
        } catch (Exception e) {
            Logger.w(TAG, e);
            return null;
        }
    }

    public static boolean hasMethod(Object bean, String methodName, Class<?>... parameterTypes) {
        try {
            bean.getClass().getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            return false;
        }
        return true;
    }

    /**
     * get class template from dex/zip/jar/apk with reflection.
     * @param context - the context of the application
     * @param dexpath - the file-path of dex/zip/jar/apk file (bytecode file format must be dex!)
     * @param classpath - the package-path of the class (like "com.lenovo.dynamic.MyClass")
     * @param output - the output directory of the bytecode file
     * 
     * @return
     *         the class-template object, if loads class successfully;
     *         null, if loads class failed.
     */
    public static Class<?> getClassTemplateFromPack(Context context, String dexpath, String classpath, String output) {
        ClassLoader loader = new DexClassLoader(dexpath, output, null, context.getClassLoader());
        try {
            return loader.loadClass(classpath);
        } catch (Exception e) {
            Logger.d(TAG, "Load class exception:" + e.toString());
        }
        return null;
    }

    /**
     * create instance of the class which gets from getClassTemplateFromPack().
     * @param c - the class-object which gets from getClassTemplateFromPack()
     * @param args - the array of arguments to the constructor of class 'c' or null
     * @param argstypes - the types of arguments or null
     * 
     * @return
     *         the instance of the class 'c', if no exception throws;
     *         null, if any exception throw.
     */
    public static Object createInstanceOfClass(Class<?> c, Object[] args, Class<?>... argtypes) {
        if (c == null)
            return null;

        Object ret = null;
        try {
            if (args == null)
                ret = c.newInstance();
            else {
                Constructor<?> con = c.getConstructor(argtypes); // never return null
                ret = con.newInstance(args);
            }
        } catch (Exception e) {}
        return ret;
    }

    public static Object invokeStaticMethod(Class clazz, String methodName, Class<?>[] paramTypes, Object[] args)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        Method method = clazz.getDeclaredMethod(methodName, paramTypes);
        boolean originAccessible = method.isAccessible();
        try {
            if (!originAccessible)
                method.setAccessible(true);
            return method.invoke(clazz, args);
        } finally {
            method.setAccessible(originAccessible);
        }
    }
}
