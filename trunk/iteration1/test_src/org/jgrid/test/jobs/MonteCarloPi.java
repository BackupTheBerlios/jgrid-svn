package org.jgrid.test.jobs;

import org.jgrid.Service;
import org.apache.log4j.Logger;

import java.util.Random;
import java.io.Serializable;

/**
 * Examples of compting pi using monte carlo methods.   This simple algorithm shows how to start
 * a monte-carlo simulation in parallel.
 * <br>User: Joshua Davis
 * Date: Dec 31, 2005
 * Time: 5:39:01 PM
 */
public class MonteCarloPi implements Service {
    private static Logger log = Logger.getLogger(MonteCarloPi.class);

    private static final int ITERATIONS = 100000000;
    private static final double RADIUS = 1.0;

    public static void main(String[] args) {
        try {
            new MonteCarloPi().run(args);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private void run(String[] args) {
        buffonsNeedle();
        badDartPlayer();
    }

    private boolean showProgress(int i) {
        return (i > 0) && (i % 10000000 == 0);
    }

    private void showBadDartPlayerApproximation(int i, int m) {
        double approximatepi = 4.0 * ((double)m)/((double)i);
        log.info("i = " + i + " approximate pi = " + approximatepi);
    }

    private void buffonsNeedle() {
        // Simulate a needle of length 'l' being dropped at random between two parallel lines separated by
        // distance 'd'.  The needle is smaller than the distance between the lines (d > l).
        // On each random trial, one end of the needle will fall distance 'A' between the lines, so
        // 'A' is a random number between 0 and 'd'.   The angle of the needle, theta, will also be random
        // from zero to pi radians.
        // The needle will cross the line if 'A' < 'l' * sin(theta)
        System.out.println("Buffon's needle:");
        double d = 2.0;
        double l = 1.0;
        int n = MonteCarloPi.ITERATIONS;
        Random r = new Random(31);
        int hits = 0;
        for (int i = 0; i < n; i++)
        {
            double theta = r.nextDouble() * Math.PI;
            double a = r.nextDouble() * d;
            double x = l * Math.sin(theta);
            if (a < x)
                hits++;
            if (showProgress(i))
                showBuffonApproximation(i, hits, l, d);
        }
        showBuffonApproximation(n, hits, l, d);
    }

    private void showBuffonApproximation(int i, int hits, double l, double d) {
        // pi = n/m * (2*l)/d
        // where:
        // n is the total number of trials
        // m is the number of times the needle crosses the line
        // l is the length of the needle
        // d is the distance between the lines
        double approximatepi = calculateNeedlePi(i, hits, l, d);
        log.info("i = " + i + " approximate pi = " + approximatepi);
    }

    private double calculateNeedlePi(int i, int hits, double l, double d)
    {
        return ((double)i)/((double)hits) * (2.0 * l) / d;
    }

    private void badDartPlayer() {
        // Simulate a very bad dart player throwing darts at a square target with one quarter of a circle as
        // the target area.  The dartboard is a 1x1 square and the 1/4 circle is of radius 1.
        // A dart hits the target if the distance from (0,0) is less than or equal to 1.
        // Pi is approximately 4 * hits / attempts
        System.out.println("Bad dart player:");
        Random r = new Random(31);
        int n = MonteCarloPi.ITERATIONS;
        int m = 0;
        // Pre-compute the square of the radius so we don't have to call Math.sqrt().
        double radiusSquared = MonteCarloPi.RADIUS * MonteCarloPi.RADIUS;
        for(int i = 0; i < n ; i++)
        {
            double x = r.nextDouble();
            double y = r.nextDouble();
            double d = x * x + y * y;
            if (d <= radiusSquared)
                m++;
            if (showProgress(i))
                showBadDartPlayerApproximation(i,m);
        }
        showBadDartPlayerApproximation(n,m);
    }

    public Serializable execute(Serializable input)
    {
        MonteCarloPiInput in = (MonteCarloPiInput) input;
        // Simulate a needle of length 'l' being dropped at random between two parallel lines separated by
        // distance 'd'.  The needle is smaller than the distance between the lines (d > l).
        // On each random trial, one end of the needle will fall distance 'A' between the lines, so
        // 'A' is a random number between 0 and 'd'.   The angle of the needle, theta, will also be random
        // from zero to pi radians.
        // The needle will cross the line if 'A' < 'l' * sin(theta)
        double d = in.getDistance();
        double l = in.getLength();
        int n = MonteCarloPi.ITERATIONS;
        Random r = new Random(in.getSeed());
        int hits = 0;
        for (int i = 0; i < n; i++)
        {
            double theta = r.nextDouble() * Math.PI;
            double a = r.nextDouble() * d;
            double x = l * Math.sin(theta);
            if (a < x)
                hits++;
            if (showProgress(i))
                showBuffonApproximation(i, hits, l, d);
        }
        showBuffonApproximation(n, hits, l, d);
        return new MonteCarloPiOutput(n,hits,l,d);
    }
}
