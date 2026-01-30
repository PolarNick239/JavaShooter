import javax.sound.sampled.*;
import java.util.Random;

public class SoundManager {
    private static final int SAMPLE_RATE = 44100;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final Random RNG = new Random();

    private static final byte[] SHOOT = tone(1200, 0.05, 0.35);
    private static final byte[] HIT = tone(420, 0.06, 0.4);
    private static final byte[] ENEMY_DEATH = mix(
            noise(0.18, 0.45),
            tone(180, 0.18, 0.3),
            0.8, 0.6
    );
    private static final byte[] GRENADE_THROW = tone(220, 0.05, 0.35);
    private static final byte[] GRENADE_EXPLOSION = mix(
            noise(0.35, 0.6),
            tone(80, 0.35, 0.45),
            0.9, 0.7
    );
    private static final byte[] BONUS = concat(
            tone(900, 0.05, 0.4),
            tone(1200, 0.07, 0.35)
    );
    private static final byte[] LEVEL_UP = concat(
            tone(500, 0.08, 0.35),
            tone(700, 0.08, 0.35),
            tone(900, 0.1, 0.35)
    );
    private static final byte[] PLAYER_HIT = tone(140, 0.08, 0.45);
    private static final byte[] BOSS_SPAWN = mix(
            tone(90, 0.25, 0.4),
            noise(0.2, 0.25),
            0.8, 0.6
    );
    private static final byte[] BOSS_SHOT = mix(
            tone(180, 0.12, 0.45),
            noise(0.12, 0.25),
            0.8, 0.5
    );
    private static final byte[] BOSS_DASH = noise(0.25, 0.35);
    private static final byte[] DROP = tone(160, 0.06, 0.35);
    private static final byte[] BOSS_DEATH = mix(
            noise(0.4, 0.6),
            tone(120, 0.25, 0.4),
            0.9, 0.7
    );

    public static void playShoot() { play(SHOOT); }
    public static void playHit() { play(HIT); }
    public static void playEnemyDeath() { play(ENEMY_DEATH); }
    public static void playGrenadeThrow() { play(GRENADE_THROW); }
    public static void playGrenadeExplosion() { play(GRENADE_EXPLOSION); }
    public static void playBonus() { play(BONUS); }
    public static void playLevelUp() { play(LEVEL_UP); }
    public static void playPlayerHit() { play(PLAYER_HIT); }
    public static void playBossSpawn() { play(BOSS_SPAWN); }
    public static void playBossShot() { play(BOSS_SHOT); }
    public static void playBossDash() { play(BOSS_DASH); }
    public static void playDrop() { play(DROP); }
    public static void playBossDeath() { play(BOSS_DEATH); }

    private static void play(byte[] data) {
        if (data == null) return;
        Thread thread = new Thread(() -> {
            try {
                SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT);
                line.open(FORMAT, data.length);
                line.start();
                line.write(data, 0, data.length);
                line.drain();
                line.stop();
                line.close();
            } catch (Exception ignored) {
            }
        }, "SoundPlayer");
        thread.setDaemon(true);
        thread.start();
    }

    private static byte[] tone(double freq, double durationSeconds, double volume) {
        int samples = (int) (durationSeconds * SAMPLE_RATE);
        byte[] data = new byte[samples * 2];
        double attack = Math.min(0.01, durationSeconds * 0.2);
        for (int i = 0; i < samples; i++) {
            double t = i / (double) SAMPLE_RATE;
            double env = envelope(t, durationSeconds, attack);
            double value = Math.sin(2.0 * Math.PI * freq * t) * volume * env;
            short sample = (short) (value * Short.MAX_VALUE);
            int idx = i * 2;
            data[idx] = (byte) (sample & 0xFF);
            data[idx + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return data;
    }

    private static byte[] noise(double durationSeconds, double volume) {
        int samples = (int) (durationSeconds * SAMPLE_RATE);
        byte[] data = new byte[samples * 2];
        double attack = Math.min(0.02, durationSeconds * 0.2);
        for (int i = 0; i < samples; i++) {
            double t = i / (double) SAMPLE_RATE;
            double env = envelope(t, durationSeconds, attack);
            double value = (RNG.nextDouble() * 2.0 - 1.0) * volume * env;
            short sample = (short) (value * Short.MAX_VALUE);
            int idx = i * 2;
            data[idx] = (byte) (sample & 0xFF);
            data[idx + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return data;
    }

    private static double envelope(double t, double duration, double attack) {
        double decay = duration;
        double attackFactor = attack > 0 ? Math.min(1.0, t / attack) : 1.0;
        double decayFactor = Math.max(0.0, 1.0 - t / decay);
        return attackFactor * decayFactor;
    }

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] part : parts) {
            total += part.length;
        }
        byte[] out = new byte[total];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, out, offset, part.length);
            offset += part.length;
        }
        return out;
    }

    private static byte[] mix(byte[] a, byte[] b, double gainA, double gainB) {
        int max = Math.max(a.length, b.length);
        byte[] out = new byte[max];
        for (int i = 0; i < max; i += 2) {
            int sa = 0;
            int sb = 0;
            if (i + 1 < a.length) {
                sa = (short) ((a[i + 1] << 8) | (a[i] & 0xFF));
            }
            if (i + 1 < b.length) {
                sb = (short) ((b[i + 1] << 8) | (b[i] & 0xFF));
            }
            int mixed = (int) (sa * gainA + sb * gainB);
            if (mixed > Short.MAX_VALUE) mixed = Short.MAX_VALUE;
            if (mixed < Short.MIN_VALUE) mixed = Short.MIN_VALUE;
            out[i] = (byte) (mixed & 0xFF);
            if (i + 1 < out.length) {
                out[i + 1] = (byte) ((mixed >> 8) & 0xFF);
            }
        }
        return out;
    }
}
