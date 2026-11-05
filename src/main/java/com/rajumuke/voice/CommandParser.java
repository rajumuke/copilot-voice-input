package com.rajumuke.voice;

import java.util.Set;
import java.util.regex.Pattern;

/** Recognises the spoken control phrases. Stop/cancel must be a whole utterance so ordinary sentences containing "done" are not hijacked. */
final class CommandParser {
    // Whisper often hears "mic" as make/mike/mick/mac, so accept those too
    private static final Pattern WAKE = Pattern.compile(
            "\\b(turn on|start|enable) (the )?(mic|mike|mick|mack|make|microphone)\\b|\\bstart dictation\\b");
    private static final Pattern OFF = Pattern.compile(
            "\\b(turn off|stop|disable|shut off) (the )?(mic|mike|mick|mack|make|microphone)\\b|\\bstop listening\\b");
    private static final Set<String> DONE = Set.of("done", "dun", "dunn", "send", "send it", "im done", "thats all");
    private static final Set<String> CANCEL = Set.of("cancel", "cancel that", "never mind", "nevermind", "discard");

    private CommandParser() {
    }

    static String normalize(String text) {
        return text == null ? "" : text.toLowerCase().replace("'", "").replaceAll("[^a-z0-9]+", " ").trim();
    }

    static boolean isWake(String text) {
        return WAKE.matcher(normalize(text)).find();
    }

    static boolean isOff(String text) {
        return OFF.matcher(normalize(text)).find();
    }

    static boolean isDone(String text) {
        return DONE.contains(normalize(text));
    }

    private static final Pattern TRAILING_DONE = Pattern.compile(
            "^(.+?)[\\s,.;:!?-]+(?:done|that'?s all|i'?m done|send it)[\\s.!?]*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** Dictated text before a trailing stop word ("fix the tests, done"), or null if the utterance does not end in one. */
    static String textBeforeTrailingDone(String text) {
        java.util.regex.Matcher m = TRAILING_DONE.matcher(text == null ? "" : text.trim());
        return m.matches() ? m.group(1).trim() : null;
    }

    static boolean isCancel(String text) {
        return CANCEL.contains(normalize(text));
    }
}
