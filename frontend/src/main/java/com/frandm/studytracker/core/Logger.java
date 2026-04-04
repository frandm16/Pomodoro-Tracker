package com.frandm.studytracker.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOGGER_CLASS = Logger.class.getName();

    public enum Level {
        ERROR, WARN, INFO, DEBUG
    }

    public static void error(String message, Throwable throwable) {
        log(Level.ERROR, message, throwable);
    }

    public static void error(String message) {
        log(Level.ERROR, message, null);
    }

    public static void error(Throwable throwable) {
        log(Level.ERROR, throwable.getMessage(), throwable);
    }

    public static void warn(String message) {
        log(Level.WARN, message, null);
    }

    public static void info(String message) {
        log(Level.INFO, message, null);
    }

    public static void debug(String message) {
        log(Level.DEBUG, message, null);
    }

    private static void log(Level level, String message, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String caller = getCallerInfo();

        StringBuilder sb = new StringBuilder();
        sb.append(timestamp)
          .append(" ").append(level)
          .append(" ").append(caller)
          .append(" - ").append(message);

        if (throwable != null) {
            sb.append("\n");
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            sb.append(sw);
        }

        System.err.println(sb);
    }

    private static String getCallerInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (!className.equals(LOGGER_CLASS) && !className.equals("java.lang.Thread")) {
                String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
                String methodName = element.getMethodName();
                return "[" + simpleClassName + "] : [" + methodName + "]";
            }
        }

        return "[Unknown] : [Unknown]";
    }
}
