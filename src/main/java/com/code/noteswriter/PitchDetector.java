package com.code.noteswriter;

/** YIN difference function for one monophonic flute, without audio-file templates. */
final class PitchDetector {
    static final int SAMPLE_RATE = 44100;
    static final int FRAME_SIZE = 4096;

    static double frequency(double[] samples, double minimumRms) {
        double mean = 0;
        for (double sample : samples) mean += sample;
        mean /= samples.length;
        double energy = 0;
        for (double sample : samples) energy += (sample - mean) * (sample - mean);
        if (Math.sqrt(energy / samples.length) < minimumRms) return Double.NaN;
        int maxLag = Math.min(samples.length / 2, SAMPLE_RATE / 180);
        int minLag = SAMPLE_RATE / 5500;
        double[] difference = new double[maxLag + 1];
        double sum = 0;
        for (int lag = 1; lag <= maxLag; lag++) {
            double value = 0;
            for (int i = 0; i < samples.length - maxLag; i++) {
                double delta = samples[i] - samples[i + lag];
                value += delta * delta;
            }
            sum += value;
            difference[lag] = sum == 0 ? 1 : value * lag / sum;
        }
        for (int lag = minLag; lag < maxLag - 1; lag++) {
            if (difference[lag] >= 0.15) continue;
            while (lag + 1 < maxLag && difference[lag + 1] < difference[lag]) lag++;
            if (lag >= maxLag) return Double.NaN;
            double a = difference[lag - 1], b = difference[lag], c = difference[lag + 1];
            double denominator = a - 2 * b + c;
            double offset = Math.abs(denominator) < 1e-12 ? 0 : 0.5 * (a - c) / denominator;
            return SAMPLE_RATE / (lag + offset);
        }
        return Double.NaN;
    }

    static int midi(double frequency, double concertA) {
        return (int) Math.round(69 + 12 * Math.log(frequency / concertA) / Math.log(2));
    }
}
