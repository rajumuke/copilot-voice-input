package com.rajumuke.voice;

import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

/** Energy-based voice activity detector that cuts a continuous 16 kHz mono 16-bit PCM stream into utterances. */
final class UtteranceSegmenter {
    static final int CHUNK_BYTES = 3200; // 100 ms
    private static final int PRE_ROLL_CHUNKS = 3;
    private static final int END_SILENCE_CHUNKS = 8;
    private static final int MIN_SPEECH_CHUNKS = 3;
    private static final int MAX_CHUNKS = 300;
    private static final double NOISE_MULTIPLE = 2.5;
    private static final int FLOOR_WINDOW_CHUNKS = 50;
    private static final int MIN_HISTORY_CHUNKS = 20;
    private static final double FLOOR_PERCENTILE = 0.2;

    private final double minRms;
    private final Deque<byte[]> preRoll = new ArrayDeque<>();
    // Recent chunk loudness; a low percentile approximates the steady background level even while speech is present
    private final double[] history = new double[FLOOR_WINDOW_CHUNKS];
    private int historySize;
    private int historyNext;
    private ByteArrayOutputStream utterance;
    private int speechChunks;
    private int silenceChunks;
    private int totalChunks;

    UtteranceSegmenter(double minRms) {
        this.minRms = minRms;
    }

    /** Feed one chunk; returns a finished utterance, or null while still waiting or collecting. */
    byte[] feed(byte[] chunk) {
        double rms = rms(chunk);
        boolean voiced = rms > Math.max(minRms, noiseFloor() * NOISE_MULTIPLE);
        record(rms);
        if (utterance == null) {
            if (!voiced) {
                preRoll.addLast(chunk);
                if (preRoll.size() > PRE_ROLL_CHUNKS) {
                    preRoll.pollFirst();
                }
                return null;
            }
            utterance = new ByteArrayOutputStream();
            preRoll.forEach(utterance::writeBytes);
            preRoll.clear();
            speechChunks = 0;
            silenceChunks = 0;
            totalChunks = 0;
        }
        utterance.writeBytes(chunk);
        totalChunks++;
        if (voiced) {
            speechChunks++;
            silenceChunks = 0;
        } else {
            silenceChunks++;
        }
        if (silenceChunks < END_SILENCE_CHUNKS && totalChunks < MAX_CHUNKS) {
            return null;
        }
        byte[] result = speechChunks >= MIN_SPEECH_CHUNKS ? utterance.toByteArray() : null;
        utterance = null;
        return result;
    }

    private double noiseFloor() {
        if (historySize < MIN_HISTORY_CHUNKS) {
            return 0; // not enough history yet; the floor would be computed from speech itself
        }
        double[] sorted = java.util.Arrays.copyOf(history, historySize);
        java.util.Arrays.sort(sorted);
        return sorted[(int) (FLOOR_PERCENTILE * (historySize - 1))];
    }

    private void record(double rms) {
        history[historyNext] = rms;
        historyNext = (historyNext + 1) % FLOOR_WINDOW_CHUNKS;
        historySize = Math.min(historySize + 1, FLOOR_WINDOW_CHUNKS);
    }

    static double rms(byte[] pcm) {
        int samples = pcm.length / 2;
        if (samples == 0) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < samples; i++) {
            short s = (short) ((pcm[2 * i] & 0xFF) | (pcm[2 * i + 1] << 8));
            double v = s / 32768.0;
            sum += v * v;
        }
        return Math.sqrt(sum / samples);
    }
}
