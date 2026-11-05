package com.rajumuke.voice;

import java.util.ArrayList;
import java.util.List;

/** Hands-free state machine: IDLE until the wake phrase, then DICTATING until "done" (send) or "cancel" (discard). */
final class HandsFreeSession {
    interface Sink {
        void started();

        void send(String text);

        void cancelled(String reason);

        void turnedOff();
    }

    private final Sink sink;
    private final List<String> parts = new ArrayList<>();
    private boolean dictating;

    HandsFreeSession(Sink sink) {
        this.sink = sink;
    }

    synchronized void handle(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.isEmpty()) {
            return;
        }
        if (CommandParser.isOff(cleaned)) {
            if (dictating) {
                dictating = false;
                parts.clear();
                sink.turnedOff();
            }
            return;
        }
        if (!dictating) {
            if (CommandParser.isWake(cleaned)) {
                dictating = true;
                parts.clear();
                sink.started();
            }
            return;
        }
        String beforeDone = CommandParser.isDone(cleaned) ? "" : CommandParser.textBeforeTrailingDone(cleaned);
        if (beforeDone != null) {
            dictating = false;
            if (!beforeDone.isEmpty()) {
                parts.add(beforeDone);
            }
            String prompt = String.join(" ", parts).trim();
            parts.clear();
            if (prompt.isEmpty()) {
                sink.cancelled("Nothing was dictated.");
            } else {
                sink.send(prompt);
            }
        } else if (CommandParser.isCancel(cleaned)) {
            dictating = false;
            parts.clear();
            sink.cancelled("Dictation cancelled.");
        } else {
            parts.add(cleaned);
        }
    }

    synchronized boolean isDictating() {
        return dictating;
    }
}
