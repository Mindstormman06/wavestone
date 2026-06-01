package typhonic.wavestone.util;

/**
 * Defines the 15 logarithmically-spaced frequency bands spanning human hearing range.
 *
 * Log spacing means each band covers a perceptually equal "distance" in pitch —
 * the same way musical octaves work. Band 0 is deep sub-bass, band 14 is air/brilliance.
 *
 * Cutoff frequencies generated via: f(n) = 20 * (20000/20)^(n/15) Hz
 * which distributes 15 boundaries evenly on a log scale from 20Hz to 20kHz.
 */
public final class FrequencyBands {
    private FrequencyBands() {}

    public static final int BAND_COUNT = 15;

    /**
     * Lower edge of each band in Hz.
     * Band n spans from LOWER_HZ[n] to LOWER_HZ[n+1] (or 20000 for the last band).
     */
    public static final float[] LOWER_HZ = {
            20.0f,      // Band 0:  Sub-bass         20–40 Hz
            40.0f,      // Band 1:  Bass              40–80 Hz
            80.0f,      // Band 2:  Upper-bass        80–160 Hz
            160.0f,     // Band 3:  Low-mid           160–320 Hz
            320.0f,     // Band 4:  Mid               320–640 Hz
            640.0f,     // Band 5:  Upper-mid         640 Hz–1.28 kHz
            1280.0f,    // Band 6:  Presence          1.28–2.56 kHz
            2560.0f,    // Band 7:  Brilliance low    2.56–3.84 kHz
            3840.0f,    // Band 8:  Brilliance        3.84–5.12 kHz
            5120.0f,    // Band 9:  Brilliance high   5.12–6.8 kHz
            6800.0f,    // Band 10: Air low            6.8–9.1 kHz
            9100.0f,    // Band 11: Air               9.1–12.1 kHz
            12100.0f,   // Band 12: Air high          12.1–14.9 kHz
            14900.0f,   // Band 13: Ultra-high        14.9–17.6 kHz
            17600.0f    // Band 14: Extreme high      17.6–20 kHz
    };

    public static final float UPPER_LIMIT_HZ = 20000.0f;

    /**
     * Human-readable band names for debug/UI use.
     */
    public static final String[] BAND_NAMES = {
            "Sub-Bass", "Bass", "Upper Bass", "Low Mid", "Mid",
            "Upper Mid", "Presence", "Brilliance Low", "Brilliance",
            "Brilliance High", "Air Low", "Air", "Air High",
            "Ultra High", "Extreme High"
    };

    /**
     * Returns the lower bound in Hz for a given band index.
     */
    public static float getLowerHz(int band) {
        return LOWER_HZ[band];
    }

    /**
     * Returns the upper bound in Hz for a given band index.
     */
    public static float getUpperHz(int band) {
        return (band + 1 < BAND_COUNT) ? LOWER_HZ[band + 1] : UPPER_LIMIT_HZ;
    }
}
