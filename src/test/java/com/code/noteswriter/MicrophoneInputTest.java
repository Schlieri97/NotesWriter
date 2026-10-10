package com.code.noteswriter;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class MicrophoneInputTest {
    @Test void measuredCesDUsesCalibratedGripRegister() {
        assertEquals(86, FluteInstrument.CES_FLUTE.writtenMidi(1124, 440));
        int[] midi = {62, 63, 69, 72, 73, 74, 75, 81, 84, 85, 86, 87, 93};
        String[] bases = {"D", "Dis_Es", "A", "C1", "Cis_Des", "D", "Dis_Es", "A", "C1", "Cis_Des1", "D1", "Dis_Es1", "A1"};
        for (int i = 0; i < midi.length; i++) {
            double frequency = 440 * Math.pow(2, (midi[i] + 11 - 69) / 12.0);
            int mapped = FluteInstrument.CES_FLUTE.gripMidi(frequency, 440, -1);
            assertEquals(midi[i], mapped);
            var note = FlutePitchMapping.forMidi(mapped);
            assertEquals(bases[i], note.base());
            assertEquals(midi[i] <= 72 ? NoteModifier.OKTAV : NoteModifier.NORMAL, note.modifier());
        }
        double[] samples = new double[PitchDetector.FRAME_SIZE];
        double[] examples = {550, 1124, 2207};
        FlutePitchMapping.Note[] expected = {new FlutePitchMapping.Note("D", NoteModifier.OKTAV),
                new FlutePitchMapping.Note("D", NoteModifier.NORMAL), new FlutePitchMapping.Note("D1", NoteModifier.NORMAL)};
        for (int example = 0; example < examples.length; example++) {
            for (int i = 0; i < samples.length; i++) {
                double phase = 2 * Math.PI * examples[example] * i / PitchDetector.SAMPLE_RATE;
                samples[i] = 0.3 * Math.sin(phase) + 0.1 * Math.sin(2 * phase);
            }
            double measured = PitchDetector.frequency(samples, 0.01);
            assertEquals(expected[example],
                    FlutePitchMapping.forMidi(FluteInstrument.CES_FLUTE.gripMidi(measured, 440, -1)));
        }
    }

    @Test void calibratedCesFrequenciesUseTheCorrectFingerings() {
        int[] frequencies = {670, 720, 1350, 1400, 2888, 2972};
        int[] midi = {65, 66, 77, 78, 89, 91};
        for (int i = 0; i < frequencies.length; i++) {
            assertEquals(midi[i], FluteInstrument.CES_FLUTE.gripMidi(frequencies[i], 440, -1));
        }
        assertEquals(65, FluteInstrument.CES_FLUTE.gripMidi(694, 440, -1));
        assertEquals(66, FluteInstrument.CES_FLUTE.gripMidi(696, 440, -1));
        assertEquals(77, FluteInstrument.CES_FLUTE.gripMidi(1374, 440, -1));
        assertEquals(78, FluteInstrument.CES_FLUTE.gripMidi(1376, 440, -1));
    }

    @Test void nearbyOctaveFisAndGStayDistinct() {
        assertEquals(66, FluteInstrument.CES_FLUTE.gripMidi(718, 440, -1));
        assertEquals(66, FluteInstrument.CES_FLUTE.gripMidi(726.9, 440, -1));
        assertEquals(67, FluteInstrument.CES_FLUTE.gripMidi(727, 440, -1));
        assertEquals(new FlutePitchMapping.Note("G", NoteModifier.OKTAV),
                FlutePitchMapping.forMidi(FluteInstrument.CES_FLUTE.gripMidi(736, 440, -1)));
        for (double frequency : new double[]{718, 736}) {
            double[] samples = new double[PitchDetector.FRAME_SIZE];
            for (int i = 0; i < samples.length; i++) {
                double phase = 2 * Math.PI * frequency * i / PitchDetector.SAMPLE_RATE;
                samples[i] = 0.3 * Math.sin(phase) + 0.1 * Math.sin(2 * phase);
            }
            double measured = PitchDetector.frequency(samples, 0.01);
            assertEquals(frequency, measured, 3);
            assertEquals(frequency == 718 ? 66 : 67,
                    FluteInstrument.CES_FLUTE.gripMidi(measured, 440, -1));
        }
    }
    @Test void transposesCesAndPiccoloToWrittenPitch() {
        assertEquals(60, FluteInstrument.CES_FLUTE.writtenMidi(246.94, 440));
        assertEquals(67, FluteInstrument.CES_FLUTE.writtenMidi(369.994, 440));
        assertEquals(67, FluteInstrument.CES_SOPRANO.writtenMidi(739.989, 440));
        assertEquals(67, FluteInstrument.PICCOLO.writtenMidi(783.991, 440));
        assertEquals(67, FluteInstrument.C_FLUTE.writtenMidi(391.995, 440));
        double[] samples = new double[PitchDetector.FRAME_SIZE];
        for (int i = 0; i < samples.length; i++)
            samples[i] = 0.3 * Math.sin(2 * Math.PI * 246.94 * i / PitchDetector.SAMPLE_RATE);
        assertEquals(60, FluteInstrument.CES_FLUTE.writtenMidi(PitchDetector.frequency(samples, 0.01), 440));
    }
    @Test void detectsFluteAndPiccoloRangeWithHarmonicsAndSmallNoise() {
        Random random = new Random(42);
        for (int midi = 61; midi <= 105; midi++) {
            double frequency = 442 * Math.pow(2, (midi - 69) / 12.0);
            double[] samples = new double[PitchDetector.FRAME_SIZE];
            for (int i = 0; i < samples.length; i++) {
                double phase = 2 * Math.PI * frequency * i / PitchDetector.SAMPLE_RATE;
                samples[i] = 0.25 * Math.sin(phase) + 0.12 * Math.sin(2 * phase)
                        + 0.04 * Math.sin(3 * phase) + 0.005 * random.nextGaussian() + 0.1;
            }
            double measured = PitchDetector.frequency(samples, 0.01);
            assertTrue(Double.isFinite(measured), "Missing MIDI " + midi);
            assertEquals(midi, PitchDetector.midi(measured, 442), "Wrong pitch for MIDI " + midi);
        }
    }

    @Test void ignoresSilenceDcAndUnpitchedNoise() {
        double[] samples = new double[PitchDetector.FRAME_SIZE];
        assertTrue(Double.isNaN(PitchDetector.frequency(samples, 0.01)));
        java.util.Arrays.fill(samples, 0.2);
        assertTrue(Double.isNaN(PitchDetector.frequency(samples, 0.01)));
        Random random = new Random(17);
        for (int i = 0; i < samples.length; i++) samples[i] = random.nextGaussian() * 0.15;
        assertTrue(Double.isNaN(PitchDetector.frequency(samples, 0.01)));
    }

    @Test void heldNoteInsertsOnceAndPauseRearmsSameNote() {
        StableNoteGate gate = new StableNoteGate();
        assertFalse(gate.accept(67));
        assertFalse(gate.accept(67));
        assertTrue(gate.accept(67));
        for (int i = 0; i < 20; i++) assertFalse(gate.accept(67));
        assertFalse(gate.accept(-1));
        assertFalse(gate.accept(-1));
        assertFalse(gate.accept(67));
        assertFalse(gate.accept(67));
        assertTrue(gate.accept(67));
    }

    @Test void rejectsBriefPitchGlitchesButAllowsLegatoChange() {
        StableNoteGate gate = new StableNoteGate();
        gate.accept(67); gate.accept(67); assertTrue(gate.accept(67));
        assertFalse(gate.accept(79));
        assertFalse(gate.accept(67));
        assertFalse(gate.accept(-1));
        for (int i = 0; i < 4; i++) assertFalse(gate.accept(67));
        assertFalse(gate.accept(69));
        assertFalse(gate.accept(69));
        assertTrue(gate.accept(69));
    }

    @Test void mappingUsesExistingGripsAndRejectsMissingLowC() {
        assertEquals("G", FlutePitchMapping.forMidi(67).base());
        assertEquals("G", FlutePitchMapping.forMidi(PitchDetector.midi(783.99, 440) - 12).base());
        assertNull(FlutePitchMapping.forMidi(60));
        assertNull(FlutePitchMapping.forMidi(61));
        assertNull(FlutePitchMapping.forMidi(94));
        Model model = new Model();
        for (int midi = 62; midi <= 93; midi++) {
            var note = FlutePitchMapping.forMidi(midi);
            String key = note.base() + (note.modifier() == NoteModifier.OKTAV ? "_Oktav" : "");
            assertEquals(7, model.getGriff(key).length);
        }
    }

    @Test void lowerRegisterUsesOctaveGripsBeforeNormalAndHighGrips() {
        String[] middle = {"D", "Dis_Es", "E", "F", "Fis_Ges", "G", "Gis_As", "A", "Ais_B", "H", "C1", "Cis_Des1"};
        for (int i = 0; i < middle.length; i++) {
            assertEquals(new FlutePitchMapping.Note(middle[i], NoteModifier.NORMAL), FlutePitchMapping.forMidi(74 + i));
            if (i < 11) assertEquals(new FlutePitchMapping.Note(middle[i], NoteModifier.OKTAV), FlutePitchMapping.forMidi(62 + i));
        }
        assertEquals(new FlutePitchMapping.Note("Cis_Des", NoteModifier.NORMAL), FlutePitchMapping.forMidi(73));
        String[] high = {"D1", "Dis_Es1", "E1", "F1", "Fis_Ges1", "G1", "Gis_As1", "A1"};
        for (int i = 0; i < high.length; i++)
            assertEquals(new FlutePitchMapping.Note(high[i], NoteModifier.NORMAL), FlutePitchMapping.forMidi(86 + i));
        assertEquals(new FlutePitchMapping.Note("A", NoteModifier.NORMAL), FlutePitchMapping.forMidi(81));
        for (int midi : new int[]{FluteInstrument.CES_FLUTE.gripMidi(830.609, 440, -1),
                FluteInstrument.CES_SOPRANO.gripMidi(830.609, 440, 0)})
            assertEquals(new FlutePitchMapping.Note("A", NoteModifier.OKTAV), FlutePitchMapping.forMidi(midi));
    }
}
