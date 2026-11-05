package com.rajumuke.voice;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

final class VoiceInput {
    private enum State { IDLE, RECORDING, TRANSCRIBING }

    private static final Logger LOG = Logger.getInstance(VoiceInput.class);
    private static final AtomicReference<State> STATE = new AtomicReference<>(State.IDLE);
    private static final Recorder RECORDER = new Recorder();

    private VoiceInput() {
    }

    static boolean isRecording() {
        return STATE.get() == State.RECORDING;
    }

    static void toggle(Project project, boolean send) {
        if (HandsFree.isArmed()) {
            notify(project, "Hands-free mode is on. Say \"turn on the mic\", or switch it off first.", NotificationType.INFORMATION);
            return;
        }
        if (STATE.compareAndSet(State.IDLE, State.RECORDING)) {
            start(project);
        } else if (STATE.compareAndSet(State.RECORDING, State.TRANSCRIBING)) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> finish(project, send));
        } else {
            notify(project, "Still transcribing, please wait.", NotificationType.INFORMATION);
        }
    }

    private static void start(Project project) {
        try {
            RECORDER.start();
            notify(project, "Listening... trigger the shortcut again to stop.", NotificationType.INFORMATION);
        } catch (Exception e) {
            STATE.set(State.IDLE);
            LOG.warn("Cannot start recording", e);
            notify(project, "Cannot open microphone: " + e.getMessage(), NotificationType.ERROR);
        }
    }

    private static void finish(Project project, boolean send) {
        Path wav = null;
        Path cleaned = null;
        try {
            byte[] pcm = RECORDER.stop();
            wav = Files.createTempFile("copilot-voice", ".wav");
            Recorder.writeWav(pcm, wav);
            cleaned = Denoiser.apply(wav);
            String text = Transcriber.transcribe(cleaned);
            if (text.isEmpty()) {
                notify(project, "No speech detected.", NotificationType.WARNING);
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!TextInserter.insert(text, send)) {
                    notify(project, "No focused input to type into.", NotificationType.WARNING);
                }
            });
        } catch (Exception e) {
            LOG.warn("Voice transcription failed", e);
            notify(project, "Voice input failed: " + e.getMessage(), NotificationType.ERROR);
        } finally {
            deleteQuietly(wav);
            if (cleaned != wav) {
                deleteQuietly(cleaned);
            }
            STATE.set(State.IDLE);
        }
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (Exception e) {
            LOG.debug("Could not delete " + file, e);
        }
    }

    static void notify(Project project, String message, NotificationType type) {
        NotificationGroupManager.getInstance().getNotificationGroup("Copilot Voice Input")
                .createNotification(message, type).notify(project);
    }
}
