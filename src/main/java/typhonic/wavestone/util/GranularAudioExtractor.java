package typhonic.wavestone.util;

import net.minecraft.client.sounds.JOrbisAudioStream;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Granular audio FFT extractor.
 *
 * Uses JOrbisAudioStream.readChunk(FloatConsumer) to stream decoded float
 * samples directly — no manual PCM byte decoding needed. Processes one tick
 * at a time so the full track is never held in memory simultaneously.
 *
 * FFT: iterative Cooley-Tukey radix-2 DIT — no external dependencies.
 * Working arrays are allocated once and reused across windows.
 */
public final class GranularAudioExtractor {
    private GranularAudioExtractor() {}

    private static final double TICK_DURATION_SECONDS = 0.05;
    private static final int    FFT_SIZE               = 2048;

    /**
     * Main entry point. Reads from oggStream chunk by chunk, accumulates
     * samples per tick, runs FFT, and emits one byte[15] per tick.
     * The stream is fully consumed and closed.
     */
    public static List<byte[]> extractGranularTimeline(JOrbisAudioStream oggStream, AudioFormat format)
            throws IOException {

        float sampleRate   = format.getSampleRate();
        int   channels     = format.getChannels();
        int   samplesPerTick = (int) Math.round(sampleRate * TICK_DURATION_SECONDS);
        // readChunk yields interleaved samples for all channels; one "mono frame" per tick
        int   framesPerTick = samplesPerTick * channels;

        int[]    binToBand = buildBinToBandMap(FFT_SIZE, sampleRate);
        double[] re        = new double[FFT_SIZE];
        double[] im        = new double[FFT_SIZE];
        double[] window    = buildHannWindow(FFT_SIZE);

        List<byte[]> timeline = new ArrayList<>(4096);

        // Ring buffer of raw interleaved float samples from the decoder
        float[] frameBuf  = new float[framesPerTick];
        int[]   fillPtr   = {0}; // int[] so the lambda can write to it

        // readChunk calls our consumer once per decoded float sample (interleaved channels).
        // We accumulate into frameBuf; each time it fills we process one tick.
        boolean[] more = {true};
        while (more[0]) {
            more[0] = oggStream.readChunk(sample -> {
                frameBuf[fillPtr[0]++] = sample;

                if (fillPtr[0] == framesPerTick) {
                    // Downmix interleaved channels to mono, then run FFT
                    float[] mono = channels == 1 ? frameBuf : stereoToMono(frameBuf, channels, samplesPerTick);
                    timeline.add(runFFTWindow(mono, samplesPerTick, re, im, window, binToBand));
                    fillPtr[0] = 0;
                }
            });
        }

        // Flush any remaining samples as a zero-padded final tick
        if (fillPtr[0] > 0) {
            int filled = fillPtr[0] / channels; // mono frames we actually have
            float[] mono = channels == 1
                    ? frameBuf
                    : stereoToMono(frameBuf, channels, filled);
            // zero-pad to samplesPerTick
            float[] padded = new float[samplesPerTick];
            System.arraycopy(mono, 0, padded, 0, Math.min(filled, samplesPerTick));
            timeline.add(runFFTWindow(padded, samplesPerTick, re, im, window, binToBand));
        }

        oggStream.close();
        return timeline;
    }

    // -------------------------------------------------------------------------
    // Downmix interleaved multi-channel floats to mono
    // -------------------------------------------------------------------------

    private static float[] stereoToMono(float[] interleaved, int channels, int monoFrames) {
        float[] mono = new float[monoFrames];
        for (int i = 0; i < monoFrames; i++) {
            float sum = 0f;
            for (int c = 0; c < channels; c++) {
                sum += interleaved[i * channels + c];
            }
            mono[i] = sum / channels;
        }
        return mono;
    }

    // -------------------------------------------------------------------------
    // Per-tick FFT window
    // -------------------------------------------------------------------------

    private static byte[] runFFTWindow(float[] samples, int count,
                                       double[] re, double[] im, double[] window,
                                       int[] binToBand) {
        for (int i = 0; i < FFT_SIZE; i++) {
            double s = (i < count) ? samples[i] : 0.0;
            re[i] = s * window[i];
            im[i] = 0.0;
        }

        fftInPlace(re, im, FFT_SIZE);

        double[] bandPower = new double[FrequencyBands.BAND_COUNT];
        int[]    bandCount = new int[FrequencyBands.BAND_COUNT];

        double norm = FFT_SIZE / 4.0;
        double normSquared = norm * norm;

        int halfSize = FFT_SIZE / 2;
        for (int bin = 1; bin < halfSize; bin++) {
            int band = binToBand[bin];
            if (band >= 0) {
                double mag = (re[bin] * re[bin] + im[bin] * im[bin]) / normSquared;
                bandPower[band] += mag;
                bandCount[band]++;
            }
        }

        byte[] tickBands = new byte[FrequencyBands.BAND_COUNT];
        for (int b = 0; b < FrequencyBands.BAND_COUNT; b++) {
            double avg = (bandCount[b] > 0) ? bandPower[b] / bandCount[b] : 0.0;
            tickBands[b] = (byte) powerToByte(avg);
        }
        return tickBands;
    }

    // -------------------------------------------------------------------------
    // FFT — iterative Cooley-Tukey radix-2 DIT, in-place, O(N log N)
    // -------------------------------------------------------------------------

    private static void fftInPlace(double[] re, double[] im, int n) {
        int j = 0;
        for (int i = 1; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                double tmpR = re[i]; re[i] = re[j]; re[j] = tmpR;
                double tmpI = im[i]; im[i] = im[j]; im[j] = tmpI;
            }
        }

        for (int len = 2; len <= n; len <<= 1) {
            double angle = -2.0 * Math.PI / len;
            double wRe = Math.cos(angle);
            double wIm = Math.sin(angle);

            for (int i = 0; i < n; i += len) {
                double curRe = 1.0, curIm = 0.0;
                for (int k = 0; k < len / 2; k++) {
                    int u = i + k;
                    int v = i + k + len / 2;

                    double tRe = curRe * re[v] - curIm * im[v];
                    double tIm = curRe * im[v] + curIm * re[v];

                    re[v] = re[u] - tRe;
                    im[v] = im[u] - tIm;
                    re[u] += tRe;
                    im[u] += tIm;

                    double nextRe = curRe * wRe - curIm * wIm;
                    curIm = curRe * wIm + curIm * wRe;
                    curRe = nextRe;
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Hann window
    // -------------------------------------------------------------------------

    private static double[] buildHannWindow(int size) {
        double[] w = new double[size];
        for (int i = 0; i < size; i++) {
            w[i] = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / (size - 1)));
        }
        return w;
    }

    // -------------------------------------------------------------------------
    // Bin → Band mapping
    // -------------------------------------------------------------------------

    private static int[] buildBinToBandMap(int fftSize, float sampleRate) {
        double hzPerBin = sampleRate / (double) fftSize;
        int[] map = new int[fftSize / 2];
        for (int bin = 0; bin < fftSize / 2; bin++) {
            double binHz = bin * hzPerBin;
            map[bin] = -1;
            for (int b = 0; b < FrequencyBands.BAND_COUNT; b++) {
                if (binHz >= FrequencyBands.getLowerHz(b) && binHz < FrequencyBands.getUpperHz(b)) {
                    map[bin] = b;
                    break;
                }
            }
        }
        return map;
    }

    // -------------------------------------------------------------------------
    // Power → Byte scale (0–255)
    // -------------------------------------------------------------------------

    private static int powerToByte(double avgMagSquared) {
        if (avgMagSquared <= 0.0) return 0;
        double db = 10.0 * Math.log10(avgMagSquared + 1e-12);
        double scaled = (db + 60.0) / 60.0 * 255.0;
        return (int) Math.max(0, Math.min(255, scaled));
    }
}
