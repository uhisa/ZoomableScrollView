package com.uhisa.zoomablescrollview.utils;

import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by uhisa on 2018/04/02.
 */

public class Logger {
    private static final int MAX_TAG_LENGTH = 23;
    private static final int CALL_STACK_INDEX = 1;
    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");

    private static boolean mEnabled = false;

    public static void d(String format, Object... args) {
        if (mEnabled) {
            Log.d(getTag(), String.format(format, args));
        }
    }

    public static void dd(String tag, String format, Object... args) {
        if (mEnabled) {
            Log.d(tag, String.format(format, args));
        }
    }

    public static void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    private static String getTag() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length <= CALL_STACK_INDEX) {
            throw new IllegalStateException(
                    "Synthetic stacktrace didn't have enough elements: are you using proguard?");
        }
        return createStackElementTag(stackTrace[CALL_STACK_INDEX]);
    }

    private static String createStackElementTag(StackTraceElement element) {
        String tag = element.getClassName();
        Matcher m = ANONYMOUS_CLASS.matcher(tag);
        if (m.find()) {
            tag = m.replaceAll("");
        }
        tag = tag.substring(tag.lastIndexOf('.') + 1);
        return tag.length() > MAX_TAG_LENGTH ? tag.substring(0, MAX_TAG_LENGTH) : tag;
    }
}
