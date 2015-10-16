/*
 * File: Log.java
 * Author: Mathias Siede
 * 
 * Property of ${company}
 * 
 * Copyright: 2012
 */

package de.thecode.android.tazreader.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;

import de.thecode.android.tazreader.BuildConfig;

/**
 * The Class Log.<br>
 * Please watch out for the methods
 * <ol>
 * <li>logAllClasses</li>
 * <li>logClass</li>
 * <li>muteClass</li>
 */
public final class Log {

    public static final int VERBOSE = android.util.Log.VERBOSE;
    public static final int DEBUG = android.util.Log.DEBUG;
    public static final int INFO = android.util.Log.INFO;
    public static final int WARN = android.util.Log.WARN;
    public static final int ERROR = android.util.Log.ERROR;
    private static final int TRACE = 16;

    private static int activeloglevel = 0;
    private static boolean logInitalized = false;

    private static String logtag = "not initialized";
    private static String versionName = "not initialized";
    private static int versionCode = 0;

    private static ArrayList<Class<?>> logOnClasses = new ArrayList<Class<?>>();
    private static ArrayList<Class<?>> muteOnClasses = new ArrayList<Class<?>>();
    private static boolean LOG_ALL = true;


    public static void init(Context context, String logtag) {
        if (BuildConfig.DEBUG) init(context, logtag, Log.VERBOSE);
        else init(context, logtag, Log.WARN);
    }

    public static void init(Context context, String logtag, int logLevel) {
        if (!logInitalized) {
            PackageInfo pinfo;
            try {
                pinfo = context.getPackageManager()
                               .getPackageInfo(context.getPackageName(), 0);
                versionName = pinfo.versionName;
                versionCode = Integer.parseInt(String.valueOf(pinfo.versionCode)
                                                     .substring(1));
            } catch (NameNotFoundException e) {
                versionName = "ERROR";
            }
            Log.logtag = logtag;
            android.util.Log.i(logtag, "-----------------------");
            android.util.Log.i(logtag, "Version: " + versionName + " (" + versionCode + ")");
            android.util.Log.i(logtag, "BuildConfig.DEBUG:" + BuildConfig.DEBUG);
            setLogLevel(logLevel);
            android.util.Log.i(logtag, "-----------------------");
        }
    }

    public static int getVersionCode() {
        return versionCode;
    }


    public static String getVersionName() {
        return versionName;
    }

    public static void setLogLevel(int loglevel) {
        String name = "unknown";
        switch (loglevel) {
            case VERBOSE:
                name = "VERBOSE";
                break;
            case DEBUG:
                name = "DEBUG";
                break;
            case INFO:
                name = "INFO";
                break;
            case WARN:
                name = "WARN";
                break;
            case ERROR:
                name = "ERROR";
                break;
        }
        activeloglevel = loglevel;
        android.util.Log.i(logtag, "setting logLevel to " + name);
        logInitalized = true;
    }

    /**
     * Add class to the whitelist of logging-output. If Log.logAllClasses(true) is, this method removes a class from the blacklist.
     *
     * @param cls the class to be added
     */
    public static void logClass(Class<?> cls) {
        if (!logOnClasses.contains(cls)) {
            Log.i(cls.getSimpleName() + " will be logged");
            logOnClasses.add(cls);
        }
        muteOnClasses.remove(cls);
    }

    /**
     * Add class to the blacklist of logging-output, but just if Log.logAllClasses(true) is set! If not, it returns a class from the
     * whitelist.
     *
     * @param cls the class to be added
     */
    public static void muteClass(Class<?> cls) {
        logOnClasses.remove(cls);
        if (!muteOnClasses.contains(cls)) {
            muteOnClasses.add(cls);
            Log.i(cls.getSimpleName() + " will not be logged");
        }
    }

    /**
     * Switch between whitelist and blacklist in logging. Default is true (which means blacklist)!
     *
     * @param bool switch true/false
     */
    public static void logAllClasses(boolean bool) {
        LOG_ALL = bool;
    }

    @SuppressLint("NewApi")
    private static void log(int logLevel, Object[] o, Throwable tr) {
        if (!logInitalized) {
            android.util.Log.e(logtag, "Log not initialized, please use Log.init(...)");
            activeloglevel = Log.VERBOSE;
        }
        if (logLevel >= activeloglevel) {
            StringBuilder message = new StringBuilder();
            try {
                throw new Exception();
            } catch (Exception e) {
                String className = e.getStackTrace()[2].getClassName();

                try {
                    Class<?> cls = Class.forName(className);
                    if (!LOG_ALL) {
                        if (cls != null) {
                            if (!logOnClasses.contains(cls)) return;
                        }
                    } else {
                        if (cls != null) {
                            if (muteOnClasses.contains(cls)) return;
                        }
                    }
                } catch (ClassNotFoundException e1) {

                }


                String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
                String methodName = e.getStackTrace()[2].getMethodName();
                int lineNumber = e.getStackTrace()[2].getLineNumber();
                message.append(simpleClassName);
                if (methodName != null) {
                    message.append(".");
                    message.append(methodName);
                }
                if (lineNumber > 0) {
                    message.append("[");
                    message.append(lineNumber);

                    String threadName = Thread.currentThread()
                                              .getName();
                    if (threadName != null) message.append("|")
                                                   .append(threadName);

                    message.append("]");
                }
                if (o != null) {
                    for (Object object : o) {
                        message.append(" ");
                        message.append(object);
                    }
                }
            }

            if (message.length() == 0 && tr != null) message.append("\n");
            if (tr != null) message.append(android.util.Log.getStackTraceString(tr));

            switch (logLevel) {
                case VERBOSE:
                    android.util.Log.println(VERBOSE, logtag, message.toString());
                    break;
                case DEBUG:
                    android.util.Log.println(DEBUG, logtag, message.toString());
                    break;
                case INFO:
                    Crashlytics.getInstance().core.log(INFO, logtag, message.toString());
                    break;
                case ERROR:
                    Crashlytics.getInstance().core.log(ERROR, logtag, message.toString());
                    break;
                case WARN:
                    Crashlytics.getInstance().core.log(WARN, logtag, message.toString());
                    break;
                case TRACE:
                    Crashlytics.getInstance().core.log(INFO, logtag, message.toString());
                    break;
            }
        }
    }

    public static void d() {
        log(android.util.Log.DEBUG, null, null);
    }

    public static void d(Object... o) {
        log(android.util.Log.DEBUG, o, null);
    }

    public static void e(Object... o) {
        log(android.util.Log.ERROR, o, null);
    }

    public static void e(Object o, Throwable tr) {
        log(android.util.Log.ERROR, makeArray(o), tr);
    }

    public static void e(Throwable tr) {
        log(android.util.Log.ERROR, null, tr);
    }

    public static void i() {
        log(android.util.Log.INFO, null, null);
    }

    public static void i(Object... o) {
        log(android.util.Log.INFO, o, null);
    }

    public static void i(Object o, Throwable tr) {

        log(android.util.Log.INFO, makeArray(o), tr);
    }

    public static void v(Object... o) {
        log(android.util.Log.VERBOSE, o, null);
    }

    public static void v(Object o, Throwable tr) {
        log(android.util.Log.VERBOSE, makeArray(o), tr);
    }

    public static void w(Object... o) {
        log(android.util.Log.WARN, o, null);
    }

    public static void w(Object o, Throwable tr) {
        log(android.util.Log.WARN, makeArray(o), tr);
    }

    public static void w(Throwable tr) {
        log(android.util.Log.WARN, null, tr);
    }

    public static void wtf(Object... o) {
        log(android.util.Log.ASSERT, o, null);
    }

    public static void wtf(Object o, Throwable tr) {
        log(android.util.Log.ASSERT, makeArray(o), tr);
    }

    public static void wtf(Throwable tr) {
        log(android.util.Log.ASSERT, null, tr);
    }

    public static void t(Object... o) {
        log(TRACE, o, null);
    }

    public static void t(Object o, Throwable tr) {
        log(TRACE, makeArray(o), tr);
    }

    public static void sendExceptionWithCrashlytics(Throwable tr) {
        Crashlytics.getInstance().core.logException(tr);
    }


    private static Object[] makeArray(Object o) {
        Object[] array = new Object[1];
        array[0] = o;
        return array;
    }


//    public static String toString(Object object, String fieldDelimiter, String lineDelimiter) {
//        Class<?> noparams[] = {};
//        StringBuilder result = new StringBuilder();
//        Field[] fields = object.getClass()
//                               .getDeclaredFields();
//        if (fields != null) {
//            for (Field field : fields) {
//                if (!Modifier.isStatic(field.getModifiers())) {
//                    String fieldName = field.getName();
//                    String methodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
//                    result.append(fieldName);
//                    result.append(fieldDelimiter);
//                    field.setAccessible(true);
//
//                    try {
//                        result.append(field.get(object)
//                                           .toString());
//                    } catch (Exception e) {
//                        result.append("[");
//                        try {
//                            Method method = object.getClass()
//                                                  .getMethod(methodName, noparams);
//                            result.append(method.invoke(object, (Object[]) null)
//                                                .toString());
//                        }
//                        catch (Exception e2) {
//
//                            Throwable tr;
//                            if (e2.getCause() != null)
//                                tr=e2.getCause();
//                            else
//                                tr=e2;
//
//                            result.append(tr.getClass()
//                                            .getSimpleName());
//                            if (tr.getMessage() != null) result.append(":")
//                                                               .append(tr.getMessage());
//                        }
//                        result.append("]");
//                    }
//                    result.append(lineDelimiter);
//                }
//            }
//        }
//        return result.toString();
//    }



}
