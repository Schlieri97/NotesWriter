package com.code.noteswriter;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** All capture/analysis runs off the JavaFX thread. Each start owns a fresh instance. */
final class MicrophoneInput implements AutoCloseable {
    static final AudioFormat FORMAT = new AudioFormat(PitchDetector.SAMPLE_RATE, 16, 1, true, false);
    record Device(String name, Mixer.Info mixer) {
        @Override public String toString() { return name; }
    }
    record Reading(double frequency, int midi, boolean insert) { }
    private volatile boolean closed;
    private volatile TargetDataLine line;

    static List<Device> devices() {
        List<Device> result = new ArrayList<>();
        result.add(new Device("Standardmikrofon", null));
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        for (Mixer.Info mixer : AudioSystem.getMixerInfo()) {
            if (AudioSystem.getMixer(mixer).isLineSupported(info))
                result.add(new Device(mixer.getName(), mixer));
        }
        return result;
    }

    void start(Device device, FluteInstrument instrument, double concertA, int octaveCorrection, double minimumRms,
               Consumer<Reading> listener, Consumer<String> error) {
        Thread worker = new Thread(() -> {
            TargetDataLine capture = null;
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
                capture = (TargetDataLine) (device.mixer() == null ? AudioSystem.getLine(info)
                        : AudioSystem.getMixer(device.mixer()).getLine(info));
                synchronized (this) {
                    if (closed) return;
                    capture.open(FORMAT, PitchDetector.FRAME_SIZE * 4);
                    line = capture;
                    capture.start();
                }
                byte[] bytes = new byte[PitchDetector.FRAME_SIZE * 2];
                double[] samples = new double[PitchDetector.FRAME_SIZE];
                StableNoteGate gate = new StableNoteGate();
                while (!closed) {
                    int offset = 0;
                    while (offset < bytes.length && !closed) {
                        int read = capture.read(bytes, offset, bytes.length - offset);
                        if (read <= 0) throw new IllegalStateException("Keine Audiodaten empfangen.");
                        offset += read;
                    }
                    if (closed) break;
                    for (int i = 0; i < samples.length; i++)
                        samples[i] = (short) ((bytes[i * 2] & 255) | (bytes[i * 2 + 1] << 8)) / 32768.0;
                    double hz = PitchDetector.frequency(samples, minimumRms);
                    int note = Double.isFinite(hz) ? instrument.gripMidi(hz, concertA, octaveCorrection) : -1;
                    listener.accept(new Reading(hz, note, gate.accept(note)));
                }
            } catch (Exception ex) {
                if (!closed) error.accept("Mikrofon nicht verfügbar. Gerät und Mikrofonfreigabe in den Systemeinstellungen prüfen. "
                        + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            } finally {
                if (capture != null) capture.close();
            }
        }, "flute-microphone");
        worker.setDaemon(true);
        worker.start();
    }

    @Override public synchronized void close() {
        closed = true;
        if (line != null) {
            line.stop();
            line.close();
        }
    }
}
