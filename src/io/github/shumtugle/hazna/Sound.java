package io.github.shumtugle.hazna;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Three short sounds, synthesised once into the cache and played from a
 * pool. No audio ships with the game.
 *
 *  drop — a stone lands: a falling triangle tone.
 *  back — a seal is torn or a move is wound back: a rising sine.
 *  seal — a sack closes: a low falling square.
 *
 * Tones are band-limited (a sum of harmonics below the hearing limit) so the
 * square does not alias into a buzz. Pitch and volume fall exponentially.
 */
final class Sound {

    static final int DROP = 0, BACK = 1, SEAL = 2;
    private static final int RATE = 44100;
    private static final int VERSION = 1;

    private final SoundPool pool;
    private final int[] ids = new int[3];
    private boolean on = true;

    Sound(Context c) {
        pool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        File dir = new File(c.getCacheDir(), "sound" + VERSION);
        dir.mkdirs();
        ids[DROP] = load(dir, "drop", 1, 560, 280, .09, .08, .12, .13);
        ids[BACK] = load(dir, "back", 0, 240, 430, .10, .05, .13, .14);
        ids[SEAL] = load(dir, "seal", 2, 180, 90, .16, .09, .22, .23);
    }

    void setOn(boolean on) {
        this.on = on;
    }

    void play(int which) {
        if (on && ids[which] != 0) pool.play(ids[which], 1f, 1f, 1, 0, 1f);
    }

    void release() {
        pool.release();
    }

    /** Synthesised only when the cache does not have it yet. */
    private int load(File dir, String name, int shape, double f0, double f1, double glide,
                     double g0, double fade, double len) {
        File f = new File(dir, name + ".wav");
        try {
            if (!f.exists()) writeWav(f, wave(shape, f0, f1, glide, g0, fade, len));
            return pool.load(f.getPath(), 1);
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * shape: 0 sine, 1 triangle, 2 square. The pitch glides from f0 to f1
     * over glide seconds, the gain from g0 to a thousandth over fade seconds;
     * the sound lasts len seconds.
     */
    static short[] wave(int shape, double f0, double f1, double glide, double g0, double fade, double len) {
        int n = (int) (len * RATE);
        short[] out = new short[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double f = t < glide ? f0 * Math.pow(f1 / f0, t / glide) : f1;
            double g = t < fade ? g0 * Math.pow(0.001 / g0, t / fade) : 0.001;
            phase += 2 * Math.PI * f / RATE;
            double v = 0;
            if (shape == 0) {
                v = Math.sin(phase);
            } else {
                for (int k = 1; k * f < 16000; k += 2) {
                    if (shape == 1) v += ((k / 2) % 2 == 0 ? 1 : -1) * Math.sin(k * phase) / (k * (double) k);
                    else v += Math.sin(k * phase) / k;
                }
                v *= shape == 1 ? 8 / (Math.PI * Math.PI) : 4 / Math.PI;
            }
            // the last few milliseconds fade to silence so the end does not click
            double tail = Math.min(1, (len - t) / 0.004);
            out[i] = (short) Math.max(-32767, Math.min(32767, Math.round(v * g * tail * 32767)));
        }
        return out;
    }

    private static void writeWav(File f, short[] pcm) throws IOException {
        int data = pcm.length * 2;
        byte[] b = new byte[44 + data];
        put(b, 0, "RIFF");
        le32(b, 4, 36 + data);
        put(b, 8, "WAVEfmt ");
        le32(b, 16, 16);
        le16(b, 20, 1);
        le16(b, 22, 1);
        le32(b, 24, RATE);
        le32(b, 28, RATE * 2);
        le16(b, 32, 2);
        le16(b, 34, 16);
        put(b, 36, "data");
        le32(b, 40, data);
        for (int i = 0; i < pcm.length; i++) le16(b, 44 + i * 2, pcm[i]);
        File tmp = new File(f.getPath() + ".part");
        FileOutputStream o = new FileOutputStream(tmp);
        try {
            o.write(b);
        } finally {
            o.close();
        }
        if (!tmp.renameTo(f)) throw new IOException("rename");
    }

    private static void put(byte[] b, int at, String s) {
        for (int i = 0; i < s.length(); i++) b[at + i] = (byte) s.charAt(i);
    }

    private static void le16(byte[] b, int at, int v) {
        b[at] = (byte) v;
        b[at + 1] = (byte) (v >> 8);
    }

    private static void le32(byte[] b, int at, int v) {
        le16(b, at, v);
        le16(b, at + 2, v >> 16);
    }
}
