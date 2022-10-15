package dev.cvaugh.discordbot;

public final class Logger {
    public static void info(String s) {
        System.out.printf("[info] %s%n", s);
    }

    public static void info(String s, Object... args) {
        info(String.format(s, args));
    }

    public static void warn(String s) {
        System.out.printf("[warn] %s%n", s);
    }

    public static void warn(String s, Object... args) {
        warn(String.format(s, args));
    }

    public static void error(String s) {
        System.err.printf("[error] %s%n", s);
    }

    public static void error(String s, Object... args) {
        warn(String.format(s, args));
    }
}
