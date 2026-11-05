package com.rajumuke.voice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoiceInputTest {

    @Test
    void cleanerRemovesNonSpeechMarkersAndCollapsesWhitespace() {
        assertEquals("Please review this.", TranscriptCleaner.clean(" [BLANK_AUDIO] Please  (music) review\nthis. *click*\n"));
    }

    @Test
    void cleanerHandlesNull() {
        assertEquals("", TranscriptCleaner.clean(null));
    }

    @Test
    void commandPassesArgumentsWithoutShell() {
        List<String> cmd = Transcriber.command("/bin/whisper-cli", "/m/model.bin", Path.of("/tmp/a b; rm -rf.wav"), "dev terms");
        assertEquals(List.of("/bin/whisper-cli", "-m", "/m/model.bin", "-f", "/tmp/a b; rm -rf.wav", "-nt", "-np", "--prompt", "dev terms"), cmd);
    }

    @Test
    void denoiseCommandUsesRnnoiseWithoutShell() {
        List<String> cmd = Denoiser.command("/bin/ffmpeg", "/m/sh.rnnn", Path.of("/tmp/in; x.wav"), Path.of("/tmp/out.wav"));
        assertEquals("highpass=f=100,arnndn=m=/m/sh.rnnn,dynaudnorm", cmd.get(cmd.indexOf("-af") + 1));
        assertEquals("/tmp/in; x.wav", cmd.get(cmd.indexOf("-i") + 1));
    }

    @Test
    void wavIsWrittenAs16kMono16Bit(@TempDir Path dir) throws Exception {
        Path wav = dir.resolve("x.wav");
        Recorder.writeWav(new byte[32000], wav);
        try (AudioInputStream in = AudioSystem.getAudioInputStream(wav.toFile())) {
            assertEquals(16000f, in.getFormat().getSampleRate());
            assertEquals(1, in.getFormat().getChannels());
            assertEquals(16, in.getFormat().getSampleSizeInBits());
            assertEquals(16000, in.getFrameLength());
        }
    }
}
