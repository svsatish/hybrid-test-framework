package com.framework.logging;

import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;

/**
 * Logging utility that writes to both Log4j2 (console + file) and Allure report.
 */
public class Log {

    private static Logger getLogger() {
        // Walk the stack to find the calling class
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        // Index 0=getStackTrace, 1=getLogger, 2=log method, 3=caller
        String callerClass = stack.length > 3 ? stack[3].getClassName() : Log.class.getName();
        return LogManager.getLogger(callerClass);
    }

    public static void info(String message) {
        getLogger().info(message);
        Allure.step("[INFO] " + message);
    }

    public static void info(String message, Object... args) {
        getLogger().info(message, args);
        Allure.step("[INFO] " + formatMessage(message, args));
    }

    public static void debug(String message) {
        getLogger().debug(message);
    }

    public static void debug(String message, Object... args) {
        getLogger().debug(message, args);
    }

    public static void warn(String message) {
        getLogger().warn(message);
        Allure.step("[WARN] " + message);
    }

    public static void warn(String message, Object... args) {
        getLogger().warn(message, args);
        Allure.step("[WARN] " + formatMessage(message, args));
    }

    public static void error(String message) {
        getLogger().error(message);
        Allure.step("[ERROR] " + message);
    }

    public static void error(String message, Throwable t) {
        getLogger().error(message, t);
        Allure.step("[ERROR] " + message + " — " + t.getMessage());
    }

    private static String formatMessage(String pattern, Object... args) {
        String result = pattern;
        for (Object arg : args) {
            result = result.replaceFirst("\\{}", Matcher.quoteReplacement(String.valueOf(arg)));
        }
        return result;
    }
}

