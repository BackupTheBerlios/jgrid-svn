package org.jgrid.test.jobs;

import java.io.Serializable;

/**
 * Input for the MonteCarloPi service.
 * <br>User: Joshua Davis
 * Date: Jan 15, 2006
 * Time: 4:24:31 PM
 */
public class MonteCarloPiInput implements Serializable {
    private long seed;
    private int iterations;
    private double d = 2.0;
    private double l = 1.0;

    public MonteCarloPiInput(long seed, int iterations) {
        this.seed = seed;
        this.iterations = iterations;
    }

    public long getSeed() {
        return seed;
    }

    public int getIterations() {
        return iterations;
    }

    public double getDistance() {
        return d;
    }

    public double getLength() {
        return l;
    }
}
