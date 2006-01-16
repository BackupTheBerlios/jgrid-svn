package eg.mcpi;

import java.io.Serializable;

/**
 * The result of the MonteCarloPiService
 * <br>User: Joshua Davis
 * Date: Jan 15, 2006
 * Time: 4:24:48 PM
 */
public class Result implements Serializable {
    private int hits;
    private int iterations;
    private double d = 2.0;
    private double l = 1.0;

    public Result(int hits, int iterations,double d, double l) {
        this.hits = hits;
        this.iterations = iterations;
        this.d = d;
        this.l = l;
    }

    public int getHits() {
        return hits;
    }

    public int getIterations() {
        return iterations;
    }

    public double getApproximation() {
        // pi = n/m * (2*l)/d
        // where:
        // n is the total number of trials
        // m is the number of times the needle crosses the line
        // l is the length of the needle
        // d is the distance between the lines
        return ((double)iterations)/((double)hits) * (2.0 * l) / d;
    }

    public void aggregate(Result other) {
        if (this.d != other.d || this.l != other.l) {
            throw new IllegalArgumentException("Length and distance must be the same in order to aggregate!");
        }
        this.hits += other.getHits();
        this.iterations += other.getIterations();
    }

}
