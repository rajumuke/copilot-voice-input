package com.rajumuke.voice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Opt-in (COPILOT_VOICE_DENOISE=on) ffmpeg pre-filter. Off by default: RNNoise did not help steady noise and hurt when other voices were present. */
final class Denoiser {
    private static final String DEFAULT_FFMPEG = "/opt/homebrew/bin/ffmpeg";
    private static final long TIMEOUT_SECONDS = 30;

    private Denoiser() {
    }

    static List<String> command(String ffmpeg, String rnnnModel, Path in, Path out) {
        String filter = "highpass=f=100,arnndn=m=" + rnnnModel + ",dynaudnorm";
        return List.of(ffmpeg, "-loglevel", "error", "-y", "-i", in.toString(), "-af", filter, "-ar", "16000", "-ac", "1", out.toString());
    }

    /** Returns the cleaned file, or the original when denoising is disabled or unavailable. */
    static Path apply(Path wav) throws IOException, InterruptedException {
        if (!"on".equalsIgnoreCase(System.getenv("COPILOT_VOICE_DENOISE"))) {
            return wav;
        }
        String ffmpeg = env("COPILOT_VOICE_FFMPEG", DEFAULT_FFMPEG);
        String model = env("COPILOT_VOICE_RNNN_MODEL", System.getProperty("user.home") + "/.cache/whisper/sh.rnnn");
        if (!Files.isExecutable(Path.of(ffmpeg)) || !Files.isReadable(Path.of(model))) {
            return wav;
        }
        Path out = Files.createTempFile("copilot-voice-clean", ".wav");
        Process process = new ProcessBuilder(command(ffmpeg, model, wav, out))
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
        }
        if (process.isAlive() || process.exitValue() != 0 || Files.size(out) == 0) {
            Files.deleteIfExists(out);
            return wav;
        }
        return out;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
