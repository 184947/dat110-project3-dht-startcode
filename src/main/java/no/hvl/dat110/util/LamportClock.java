package no.hvl.dat110.util;

import java.io.Serializable;

public class LamportClock implements Serializable {

    private static final long serialVersionUID = 5030947794470613310L;

    private int clock = 0;

    public void increment() {
        clock++;
    }

    public void adjust(int receivedClock) {
        clock = Math.max(clock, receivedClock) + 1;
    }

    public int getClock() {
        return clock;
    }
}
