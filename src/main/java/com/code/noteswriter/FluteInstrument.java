package com.code.noteswriter;

/** Semitones added to sounding MIDI pitch to obtain the written note. */
enum FluteInstrument {
    C_FLUTE("C-Querflöte", 0),
    CES_FLUTE("Ces-Querflöte (Halbton tiefer)", 1),
    PICCOLO("C-Piccolo (Oktave höher)", -12),
    CES_SOPRANO("Ces-Sopranflöte (Oktave höher, Halbton tiefer)", -11);

    private final String label;
    private final int writtenOffset;

    FluteInstrument(String label, int writtenOffset) {
        this.label = label;
        this.writtenOffset = writtenOffset;
    }

    int writtenMidi(double frequency, double concertA) {
        return PitchDetector.midi(frequency, concertA) + writtenOffset;
    }

    int gripMidi(double frequency, double concertA, int octaveCorrection) {
        int corrected = writtenMidi(frequency, concertA) + 12 * octaveCorrection;
        if (this == CES_FLUTE && octaveCorrection == -1 && Double.isFinite(frequency)) {
            // Measured calibration points of the user's Ces flute. The midpoint
            // of each pair is used as the transition so small pitch drift does
            // not move a tone into the neighbouring fingering.
            if (frequency >= 630 && frequency < 695) return 65;   // F_Oktav, ~670 Hz
            if (frequency >= 695 && frequency < 727) return 66;   // Fis/Ges_Oktav, ~718 Hz
            if (frequency >= 727 && frequency < 762) return 67;   // G_Oktav, ~736 Hz
            if (frequency >= 1315 && frequency < 1375) return 77; // F, ~1350 Hz
            if (frequency >= 1375 && frequency < 1450) return 78; // Fis/Ges, ~1400 Hz
            if (frequency >= 2800 && frequency < 2930) return 89; // F1, ~2888 Hz
            if (frequency >= 2930 && frequency < 3050) return 91; // G1, ~2972 Hz
        }
        return corrected;
    }

    @Override public String toString() { return label; }
}
