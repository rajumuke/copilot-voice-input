package com.rajumuke.voice;

import java.util.regex.Pattern;

final class TranscriptCleaner {
    // whisper emits non-speech markers such as [BLANK_AUDIO], (music), *clicking*
    private static final Pattern NON_SPEECH = Pattern.compile("\\[[^\\]]*]|\\([^)]*\\)|\\*[^*]*\\*");

    private TranscriptCleaner() {
    }

    static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        return NON_SPEECH.matcher(raw).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }
}
