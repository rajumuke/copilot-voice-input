package com.rajumuke.voice;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

final class Recorder {
    // 16 kHz mono 16-bit PCM is what whisper.cpp expects
    static final AudioFormat FORMAT = new AudioFormat(16000f, 16, 1, true, false);

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private TargetDataLine line;
    private Thread thread;

    synchronized void start() throws LineUnavailableException {
        line = MicSelector.open();
        line.start();
        buffer.reset();
        TargetDataLine active = line;
        thread = new Thread(() -> {
            byte[] chunk = new byte[4096];
            while (active.isOpen()) {
                int n = active.read(chunk, 0, chunk.length);
                if (n > 0) {
                    synchronized (buffer) {
                        buffer.write(chunk, 0, n);
                    }
                }
            }
        }, "copilot-voice-recorder");
        thread.setDaemon(true);
        thread.start();
    }

    synchronized byte[] stop() throws InterruptedException {
        line.stop();
        line.close();
        thread.join(2000);
        synchronized (buffer) {
            return buffer.toByteArray();
        }
    }

    static void writeWav(byte[] pcm, Path file) throws IOException {
        long frames = pcm.length / FORMAT.getFrameSize();
        try (AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(pcm), FORMAT, frames)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, file.toFile());
        }
    }
}
