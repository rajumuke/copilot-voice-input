package com.rajumuke.voice;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import javax.sound.sampled.TargetDataLine;
import java.awt.Toolkit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Always-on listener (local only): wake phrase starts dictation, "done" sends it, "cancel" discards it. */
final class HandsFree {
    private static final Logger LOG = Logger.getInstance(HandsFree.class);
    private static final double DEFAULT_VAD_THRESHOLD = 0.015;
    private static HandsFree active;

    private final Project project;
    private final HandsFreeSession session;
    private final UtteranceSegmenter segmenter;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "copilot-voice-transcriber");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean running;
    private TargetDataLine line;

    private HandsFree(Project project) {
        this.project = project;
        this.segmenter = new UtteranceSegmenter(threshold());
        this.session = new HandsFreeSession(new HandsFreeSession.Sink() {
            @Override
            public void started() {
                Toolkit.getDefaultToolkit().beep();
                VoiceInput.notify(project, "Mic on. Speak your prompt, then say \"done\" to send or \"cancel\" to discard.",
                        NotificationType.INFORMATION);
            }

            @Override
            public void send(String text) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!TextInserter.insert(text, true)) {
                        VoiceInput.notify(project, "No focused input to type into.", NotificationType.WARNING);
                    }
                });
                Toolkit.getDefaultToolkit().beep();
            }

            @Override
            public void cancelled(String reason) {
                VoiceInput.notify(project, reason + " Say \"turn on the mic\" to start again.", NotificationType.INFORMATION);
            }

            @Override
            public void turnedOff() {
                Toolkit.getDefaultToolkit().beep();
                VoiceInput.notify(project, "Mic off. Still listening for \"turn on the mic\".", NotificationType.INFORMATION);
            }
        });
    }

    static synchronized boolean isArmed() {
        return active != null;
    }

    static synchronized void arm(Project project) {
        if (active != null) {
            return;
        }
        HandsFree handsFree = new HandsFree(project);
        try {
            handsFree.start();
            active = handsFree;
            VoiceInput.notify(project, "Hands-free on. Say \"turn on the mic\" to begin.", NotificationType.INFORMATION);
        } catch (Exception e) {
            LOG.warn("Cannot start hands-free listening", e);
            VoiceInput.notify(project, "Cannot open microphone: " + e.getMessage(), NotificationType.ERROR);
        }
    }

    static synchronized void disarm() {
        if (active != null) {
            active.stop();
            active = null;
        }
    }

    private void start() throws Exception {
        line = MicSelector.open();
        line.start();
        running = true;
        Thread thread = new Thread(this::captureLoop, "copilot-voice-handsfree");
        thread.setDaemon(true);
        thread.start();
    }

    private void stop() {
        running = false;
        line.stop();
        line.close();
        worker.shutdownNow();
    }

    private void captureLoop() {
        int filled = 0;
        byte[] chunk = new byte[UtteranceSegmenter.CHUNK_BYTES];
        while (running) {
            int n = line.read(chunk, filled, chunk.length - filled);
            if (n < 0) {
                break;
            }
            filled += n;
            if (filled == chunk.length) {
                byte[] utterance = segmenter.feed(chunk);
                if (utterance != null) {
                    worker.submit(() -> process(utterance));
                }
                chunk = new byte[UtteranceSegmenter.CHUNK_BYTES];
                filled = 0;
            }
        }
    }

    private void process(byte[] pcm) {
        Path wav = null;
        try {
            wav = Files.createTempFile("copilot-voice-hf", ".wav");
            Recorder.writeWav(pcm, wav);
            String transcript = Transcriber.transcribe(wav);
            LOG.info("Hands-free heard (" + transcript.length() + " chars): "
                    + (session.isDictating() ? "[dictating]" : transcript));
            session.handle(transcript);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.warn("Hands-free transcription failed", e);
            VoiceInput.notify(project, "Hands-free stopped: " + e.getMessage(), NotificationType.ERROR);
            disarm();
        } finally {
            if (wav != null) {
                try {
                    Files.deleteIfExists(wav);
                } catch (Exception e) {
                    LOG.debug("Could not delete " + wav, e);
                }
            }
        }
    }

    private static double threshold() {
        String value = System.getenv("COPILOT_VOICE_VAD_THRESHOLD");
        try {
            return value == null || value.isBlank() ? DEFAULT_VAD_THRESHOLD : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return DEFAULT_VAD_THRESHOLD;
        }
    }
}
