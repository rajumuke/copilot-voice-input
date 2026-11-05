# Copilot Voice Input (IntelliJ IDEA)

Speak to GitHub Copilot Chat instead of typing. Audio is recorded and transcribed locally with
[whisper.cpp](https://github.com/ggml-org/whisper.cpp); nothing is sent over the network except the final text you
send to Copilot.

## Requirements
- macOS (Apple Silicon), IntelliJ IDEA 2026.2+ (build 262+) with the GitHub Copilot plugin
- `brew install whisper-cpp` (provides `/opt/homebrew/bin/whisper-cli`)
- A whisper model in `~/.cache/whisper/` (the plugin uses `ggml-small.en.bin`, falling back to `ggml-base.en.bin`):
  ```
  mkdir -p ~/.cache/whisper && cd ~/.cache/whisper
  curl -LO https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.en.bin
  ```
- Allow IntelliJ to use the microphone (System Settings → Privacy & Security → Microphone)

## Install
Build the zip (needs IntelliJ IDEA installed at `/Applications/IntelliJ IDEA.app`):
```
gradle buildPlugin
```
Then in IntelliJ: Settings → Plugins → ⚙ → Install Plugin from Disk… → `build/distributions/copilot-voice-input-0.1.0.zip`, and restart.

## Use
- Mic button in the Copilot Chat title: dictate into the chat input. Shortcuts: ⌘⌥V (dictate), ⌘⌥⇧V (dictate and send).
- Hands-free button: say **"turn on the mic"**, speak, then say **"done"** to send, **"cancel"** to discard, or
  **"turn off the mic"** to stop dictating while hands-free keeps listening.
- Tools → "Voice: Select Microphone…" picks another input device. To capture meeting audio, route it through a
  loopback device such as BlackHole.

## Configuration (environment variables)
`COPILOT_VOICE_WHISPER_CLI`, `COPILOT_VOICE_VAD_THRESHOLD` (default 0.015), `COPILOT_VOICE_DENOISE=on`.

## Known limitations
Tested with synthetic speech and unit tests; behaviour with real microphones and accents may vary.
The mic icon is from [Lucide](https://lucide.dev) (ISC); see `NOTICE.md`.
