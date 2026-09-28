package com.minj;

import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public class SoundManager {
    private static final int SAMPLE_RATE = 22050;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);

    public static void playBlockBreak() {
        playNoise(0.12f, 150, 40, 0.45f);
    }

    public static void playBlockPlace() {
        playPop(0.08f, 220, 110, 0.4f);
    }

    public static void playStep() {
        playNoise(0.04f, 180, 90, 0.15f);
    }

    public static void playJump() {
        playPop(0.08f, 140, 280, 0.22f);
    }

    public static void playSplash() {
        playWaterSplash(0.25f, 0.35f);
    }

    private static void playWaterSplash(float durationSeconds, float volume) {
        new Thread(() -> {
            try {
                int samples = (int) (SAMPLE_RATE * durationSeconds);
                byte[] buffer = new byte[samples];
                Random rng = new Random();

                float lowPass = 0;
                for (int i = 0; i < samples; i++) {
                    float t = (float) i / samples;
                    float env = (float) (Math.sin(t * Math.PI) * (1.0f - t * 0.5f));
                    float white = rng.nextFloat() * 2.0f - 1.0f;
                    lowPass += (white - lowPass) * 0.25f; // Low-pass filter for liquid sound
                    float sample = lowPass * env * volume;
                    buffer[i] = (byte) (Math.max(-1.0f, Math.min(1.0f, sample)) * 127);
                }

                SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT);
                line.open(FORMAT, buffer.length);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
                line.close();
            } catch (Exception ignored) {
            }
        }).start();
    }

    private static void playNoise(float durationSeconds, float startFreq, float endFreq, float volume) {
        new Thread(() -> {
            try {
                int samples = (int) (SAMPLE_RATE * durationSeconds);
                byte[] buffer = new byte[samples];
                Random rng = new Random();

                for (int i = 0; i < samples; i++) {
                    float t = (float) i / samples;
                    float freq = startFreq + (endFreq - startFreq) * t;
                    float env = (1.0f - t);
                    float noise = (rng.nextFloat() * 2.0f - 1.0f);
                    float sin = (float) Math.sin(2.0 * Math.PI * freq * (i / (double) SAMPLE_RATE));
                    float sample = (noise * 0.7f + sin * 0.3f) * env * volume;
                    buffer[i] = (byte) (Math.max(-1.0f, Math.min(1.0f, sample)) * 127);
                }

                SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT);
                line.open(FORMAT, buffer.length);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
                line.close();
            } catch (Exception ignored) {
            }
        }).start();
    }

    private static void playPop(float durationSeconds, float startFreq, float endFreq, float volume) {
        new Thread(() -> {
            try {
                int samples = (int) (SAMPLE_RATE * durationSeconds);
                byte[] buffer = new byte[samples];

                for (int i = 0; i < samples; i++) {
                    float t = (float) i / samples;
                    float freq = startFreq + (endFreq - startFreq) * t;
                    float env = (1.0f - t) * (1.0f - t);
                    float sin = (float) Math.sin(2.0 * Math.PI * freq * (i / (double) SAMPLE_RATE));
                    float sample = sin * env * volume;
                    buffer[i] = (byte) (Math.max(-1.0f, Math.min(1.0f, sample)) * 127);
                }

                SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT);
                line.open(FORMAT, buffer.length);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
                line.close();
            } catch (Exception ignored) {
            }
        }).start();
    }
}
