package com.rajumuke.voice;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class Transcriber {
    private static final String DEFAULT_CLI = "/opt/homebrew/bin/whisper-cli";
    // Biases decoding toward developer vocabulary; override with COPILOT_VOICE_PROMPT
    private static final String DEFAULT_PROMPT =
            "Software engineering dictation about code, Java, Gradle, Kafka, Spring Boot, pull requests and bug fixes. "
                    + "Voice commands: turn on the mic, turn off the mic, done, cancel.";
    private static final long TIMEOUT_SECONDS = 60;

    private Transcriber() {
    }

    static List<String> command(String cli, String model, Path wav, String prompt) {
        return List.of(cli, "-m", model, "-f", wav.toString(), "-nt", "-np", "--prompt", prompt);
    }

    static String transcribe(Path wav) throws IOException, InterruptedException {
        String cli = env("COPILOT_VOICE_WHISPER_CLI", DEFAULT_CLI);
        String model = env("COPILOT_VOICE_MODEL", defaultModel());
        if (!Files.isExecutable(Path.of(cli))) {
            throw new IOException("whisper-cli not found at " + cli + " (brew install whisper-cpp or set COPILOT_VOICE_WHISPER_CLI)");
        }
        if (!Files.isReadable(Path.of(model))) {
            throw new IOException("Whisper model not found at " + model + " (set COPILOT_VOICE_MODEL)");
        }
        Process process = new ProcessBuilder(command(cli, model, wav, env("COPILOT_VOICE_PROMPT", DEFAULT_PROMPT))).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("whisper-cli timed out after " + TIMEOUT_SECONDS + "s");
        }
        if (process.exitValue() != 0) {
            throw new IOException("whisper-cli exited with code " + process.exitValue());
        }
        return TranscriptCleaner.clean(output);
    }

    // small.en is markedly more noise-robust than base.en; fall back to base.en if it is not downloaded
    private static String defaultModel() {
        Path dir = Path.of(System.getProperty("user.home"), ".cache", "whisper");
        Path small = dir.resolve("ggml-small.en.bin");
        return (Files.isReadable(small) ? small : dir.resolve("ggml-base.en.bin")).toString();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
