package com.code.noteswriter;

/** Each frame is about 93 ms: three stable frames insert, two silent frames rearm. */
final class StableNoteGate {
    private int candidate = -1, count, last = -1, silent;

    boolean accept(int note) {
        if (note < 0) {
            candidate = -1;
            count = 0;
            if (++silent >= 2) last = -1;
            return false;
        }
        silent = 0;
        if (candidate != note) {
            candidate = note;
            count = 0;
        }
        if (++count >= 3 && last != note) {
            last = note;
            return true;
        }
        return false;
    }
}
