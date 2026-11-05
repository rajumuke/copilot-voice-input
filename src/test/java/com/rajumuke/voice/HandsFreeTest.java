package com.rajumuke.voice;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandsFreeTest {

    private static byte[] chunk(double amplitude) {
        byte[] pcm = new byte[UtteranceSegmenter.CHUNK_BYTES];
        for (int i = 0; i < pcm.length / 2; i++) {
            short s = (short) (amplitude * 32767 * Math.sin(2 * Math.PI * 440 * i / 16000.0));
            pcm[2 * i] = (byte) s;
            pcm[2 * i + 1] = (byte) (s >> 8);
        }
        return pcm;
    }

    private static byte[] feedAll(UtteranceSegmenter seg, int loud, int quiet) {
        byte[] result = null;
        for (int i = 0; i < loud; i++) {
            assertNull(seg.feed(chunk(0.3)));
        }
        for (int i = 0; i < quiet; i++) {
            byte[] out = seg.feed(chunk(0.0));
            if (out != null) {
                result = out;
            }
        }
        return result;
    }

    @Test
    void segmenterEmitsUtteranceAfterTrailingSilence() {
        UtteranceSegmenter seg = new UtteranceSegmenter(0.015);
        byte[] out = feedAll(seg, 10, 8);
        assertNotNull(out);
        assertEquals(18 * UtteranceSegmenter.CHUNK_BYTES, out.length);
    }

    @Test
    void segmenterIgnoresSilenceAndShortBlips() {
        UtteranceSegmenter seg = new UtteranceSegmenter(0.015);
        for (int i = 0; i < 30; i++) {
            assertNull(seg.feed(chunk(0.0)));
        }
        assertNull(feedAll(seg, 1, 8));
    }

    @Test
    void segmenterAdaptsToSteadyBackgroundNoise() {
        UtteranceSegmenter seg = new UtteranceSegmenter(0.015);
        for (int i = 0; i < 80; i++) {
            seg.feed(chunk(0.05)); // steady noise, louder than the minimum threshold
        }
        byte[] out = null;
        for (int i = 0; i < 10; i++) {
            seg.feed(chunk(0.5));
        }
        for (int i = 0; i < 10 && out == null; i++) {
            out = seg.feed(chunk(0.05));
        }
        assertNotNull(out);
    }

    @Test
    void micSelectionFallsBackToDefaultWhenDeviceIsGone() {
        List<String> devices = List.of("System default", "MacBook Pro Microphone", "USB Mic");
        assertEquals("USB Mic", MicSelector.resolve(devices, "USB Mic"));
        assertNull(MicSelector.resolve(devices, "Unplugged Headset"));
        assertNull(MicSelector.resolve(devices, "System default"));
    }

    @Test
    void parserRecognisesWakeDoneAndCancel() {
        assertTrue(CommandParser.isWake("Turn on the mic."));
        assertTrue(CommandParser.isWake("Okay, turn on the microphone please"));
        assertTrue(CommandParser.isWake("Start dictation"));
        assertTrue(CommandParser.isWake("Turn on the make."));
        assertFalse(CommandParser.isWake("Please turn off the mic"));
        assertTrue(CommandParser.isDone(" Done. "));
        assertTrue(CommandParser.isDone("I'm done!"));
        assertFalse(CommandParser.isDone("The build is done"));
        assertTrue(CommandParser.isCancel("Never mind."));
    }

    private static class Recorded implements HandsFreeSession.Sink {
        final List<String> events = new ArrayList<>();

        @Override
        public void started() {
            events.add("started");
        }

        @Override
        public void send(String text) {
            events.add("send:" + text);
        }

        @Override
        public void cancelled(String reason) {
            events.add("cancelled");
        }

        @Override
        public void turnedOff() {
            events.add("off");
        }
    }

    @Test
    void turnOffWhileDictatingDiscardsAndKeepsListening() {
        Recorded sink = new Recorded();
        HandsFreeSession session = new HandsFreeSession(sink);
        session.handle("Turn off the mic.");
        session.handle("turn on the mic");
        session.handle("half a prompt");
        session.handle("Turn off the microphone");
        session.handle("done");
        session.handle("turn on the mic");
        session.handle("second prompt");
        session.handle("done");
        assertEquals(List.of("started", "off", "started", "send:second prompt"), sink.events);
        assertFalse(CommandParser.isOff("Please turn on the mic"));
    }

    @Test
    void sessionIgnoresSpeechUntilWakeThenSendsOnDone() {
        Recorded sink = new Recorded();
        HandsFreeSession session = new HandsFreeSession(sink);
        session.handle("What a nice day.");
        session.handle("Turn on the mic.");
        session.handle("Fix the failing tests.");
        session.handle("And add a regression test.");
        session.handle("Done.");
        session.handle("This is ignored again.");
        assertEquals(List.of("started", "send:Fix the failing tests. And add a regression test."), sink.events);
        assertFalse(session.isDictating());
    }

    @Test
    void doneSpokenWithoutPauseStillSends() {
        Recorded sink = new Recorded();
        HandsFreeSession session = new HandsFreeSession(sink);
        session.handle("turn on the mic");
        session.handle("Fix the failing tests. Done.");
        assertEquals(List.of("started", "send:Fix the failing tests"), sink.events);
        assertEquals("Add logging", CommandParser.textBeforeTrailingDone("Add logging, done"));
        assertEquals(null, CommandParser.textBeforeTrailingDone("Done is a word"));
    }

    @Test
    void sessionCancelAndEmptyDoneDoNotSend() {
        Recorded sink = new Recorded();
        HandsFreeSession session = new HandsFreeSession(sink);
        session.handle("turn on the mic");
        session.handle("wrong prompt");
        session.handle("cancel");
        session.handle("turn on the mic");
        session.handle("done");
        assertEquals(List.of("started", "cancelled", "started", "cancelled"), sink.events);
    }
}
