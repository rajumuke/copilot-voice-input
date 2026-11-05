package com.rajumuke.voice;

import com.intellij.ide.util.PropertiesComponent;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.List;

/** Chooses which input device to record from; the choice is persisted and falls back to the system default. */
final class MicSelector {
    static final String SYSTEM_DEFAULT = "System default";
    private static final String KEY = "copilotVoice.microphone";
    private static final DataLine.Info CAPTURE = new DataLine.Info(TargetDataLine.class, Recorder.FORMAT);

    private MicSelector() {
    }

    static List<String> availableInputs() {
        List<String> names = new ArrayList<>();
        names.add(SYSTEM_DEFAULT);
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            String name = info.getName();
            if (!name.startsWith("Port ") && !name.equals("Default Audio Device") && AudioSystem.getMixer(info).isLineSupported(CAPTURE)) {
                names.add(name);
            }
        }
        return names;
    }

    static String selected() {
        return PropertiesComponent.getInstance().getValue(KEY, SYSTEM_DEFAULT);
    }

    static void select(String name) {
        PropertiesComponent.getInstance().setValue(KEY, name, SYSTEM_DEFAULT);
    }

    /** The saved device if it is still connected, otherwise null (meaning use the system default). */
    static String resolve(List<String> available, String saved) {
        return !SYSTEM_DEFAULT.equals(saved) && available.contains(saved) ? saved : null;
    }

    static TargetDataLine open() throws LineUnavailableException {
        String device = resolve(availableInputs(), selected());
        TargetDataLine line = null;
        if (device != null) {
            for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                if (info.getName().equals(device) && AudioSystem.getMixer(info).isLineSupported(CAPTURE)) {
                    line = (TargetDataLine) AudioSystem.getMixer(info).getLine(CAPTURE);
                    break;
                }
            }
        }
        if (line == null) {
            line = AudioSystem.getTargetDataLine(Recorder.FORMAT);
        }
        line.open(Recorder.FORMAT);
        return line;
    }
}
