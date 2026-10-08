package com.code.noteswriter;

final class FlutePitchMapping {
    record Note(String base, NoteModifier modifier) {
        @Override public String toString() {
            return base.replace('_', '/') + (modifier == NoteModifier.OKTAV ? " (Oktav)" : "");
        }
    }
    private static final String[] BASE = {"Cis_Des", "D", "Dis_Es", "E", "F", "Fis_Ges", "G",
            "Gis_As", "A", "Ais_B", "H", "C1", "Cis_Des1", "D1", "Dis_Es1", "E1", "F1",
            "Fis_Ges1", "G1", "Gis_As1", "A1"};

    static Note forMidi(int midi) {
        // In this chart "Oktav" marks the LOWER register, confirmed by the
        // measured Ces pitches: D_Oktav ~550 Hz, D ~1124 Hz, D1 ~2207 Hz.
        // Instrument transposition and grip-register correction happen upstream.
        if (midi >= 62 && midi <= 72) return new Note(BASE[midi - 61], NoteModifier.OKTAV);
        if (midi == 73) return new Note("Cis_Des", NoteModifier.NORMAL);
        if (midi >= 74 && midi <= 85) return new Note(BASE[midi - 73], NoteModifier.NORMAL);
        if (midi >= 86 && midi <= 93) return new Note(BASE[midi - 73], NoteModifier.NORMAL);
        return null;
    }

    static String name(int midi) {
        String[] names = {"C", "Cis/Des", "D", "Dis/Es", "E", "F", "Fis/Ges", "G", "Gis/As", "A", "Ais/B", "H"};
        return names[Math.floorMod(midi, 12)] + (Math.floorDiv(midi, 12) - 1);
    }
}
