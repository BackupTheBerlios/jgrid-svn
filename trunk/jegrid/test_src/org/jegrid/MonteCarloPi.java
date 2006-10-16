package org.jegrid;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Random;

/**
 * Examples of compting pi using monte carlo methods.   This simple algorithm shows how to start
 * a monte-carlo simulation in parallel.
 * <br>User: Joshua Davis
 * Date: Dec 31, 2005
 * Time: 5:39:01 PM
 */
public class MonteCarloPi implements TaskRunnable
{
    private static Logger log = Logger.getLogger(MonteCarloPi.class);

    private static final int ITERATIONS = 10000000;
    private static final double RADIUS = 5.0;

    public static void main(String[] args)
    {
        try
        {
            GridConfiguration config = new GridConfiguration();
            config.setGridName("test");
            config.setType(Grid.TYPE_CLIENT);
            Grid grid = config.configure();
            grid.connect();
            Client client = grid.getClient();
            Task task = client.createTask();
            for (int i = 0; i < 10; i++)
                task.addInput(new MonteCarloPi.Input(17 * i + 1, 10000));
            final MonteCarloPi.Output output = new MonteCarloPi.Output();
            Aggregator aggregator = new MCPiAggregator(output);
            task.run(MonteCarloPi.class.getName(), aggregator, 10);
            log.info("output : " + output.showResult());
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
        }
    }

    private void run(String[] args)
    {
        buffonsNeedle();
        badDartPlayer();
    }

    private boolean showProgress(int i)
    {
        return (i > 0) && (i % 1000000 == 0);
    }

    private void showBadDartPlayerApproximation(int i, int m)
    {
        double approximatepi = 4.0 * ((double) m) / ((double) i);
        log.info("i = " + i + " approximate pi = " + approximatepi);
    }

    private void buffonsNeedle()
    {
        // Simulate a needle of length 'l' being dropped at random between two parallel lines separated by
        // distance 'd'.  The needle is smaller than the distance between the lines (d > l).
        // On each random trial, one end of the needle will fall distance 'A' between the lines, so
        // 'A' is a random number between 0 and 'd'.   The angle of the needle, theta, will also be random
        // from zero to pi radians.
        // The needle will cross the line if 'A' < 'l' * sin(theta)
        long start = System.currentTimeMillis();
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
        double iterationsPerSecond = n / ((System.currentTimeMillis() - start) / 1000.0);
        log.info("" + iterationsPerSecond + " iterations / sec.");
    }

    private void showBuffonApproximation(int i, int hits, double l, double d)
    {
        double approximatepi = calculateNeedlePi(i, hits, l, d);
        log.info("i = " + i + " approximate pi = " + approximatepi);
    }

    private static double calculateNeedlePi(int i, int hits, double l, double d)
    {
        // pi = n/m * (2*l)/d
        // where:
        // n is the total number of trials
        // m is the number of times the needle crosses the line
        // l is the length of the needle
        // d is the distance between the lines
        return ((double) i) / ((double) hits) * (2.0 * l) / d;
    }

    private void badDartPlayer()
    {
        // Simulate a very bad dart player throwing darts at a square target with one quarter of a circle as
        // the target area.  The dartboard is a 1x1 square and the 1/4 circle is of radius 1.
        // A dart hits the target if the distance from (0,0) is less than or equal to 1.
        // Pi is approximately 4 * hits / attempts
        long start = System.currentTimeMillis();
        System.out.println("Bad dart player:");
        Random r = new Random(31);
        int n = MonteCarloPi.ITERATIONS;
        int m = 0;
        // Pre-compute the square of the radius so we don't have to call Math.sqrt().
        double radiusSquared = RADIUS * RADIUS;
        for (int i = 0; i < n; i++)
        {
            double x = r.nextDouble() * RADIUS;
            double y = r.nextDouble() * RADIUS;
            double d = x * x + y * y;
            if (d <= radiusSquared)
                m++;
            if (showProgress(i))
                showBadDartPlayerApproximation(i, m);
        }
        showBadDartPlayerApproximation(n, m);
        double iterationsPerSecond = n / ((System.currentTimeMillis() - start) / 1000.0);
        log.info("" + iterationsPerSecond + " iterations / sec.");
    }

    public Serializable run(int inputId, Serializable input)
    {
        Input in = (Input) input;
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
        return new Output(n, hits, l, d);
    }

    public static class Input implements Serializable
    {
        private long seed;
        private int iterations;
        private double d = 2.0;
        private double l = 1.0;

        public Input(long seed, int iterations)
        {
            this.seed = seed;
            this.iterations = iterations;
        }

        public long getSeed()
        {
            return seed;
        }

        public int getIterations()
        {
            return iterations;
        }

        public double getDistance()
        {
            return d;
        }

        public double getLength()
        {
            return l;
        }
    }

    public static class Output implements Serializable
    {
        private int n;
        private int hits;
        private double l;
        private double d;


        public Output()
        {
        }

        public Output(int n, int hits, double l, double d)
        {
            this.n = n;
            this.hits = hits;
            this.l = l;
            this.d = d;
        }

        public int getN()
        {
            return n;
        }

        public int getHits()
        {
            return hits;
        }

        public double getL()
        {
            return l;
        }

        public double getD()
        {
            return d;
        }

        public String showResult()
        {
            double approximatepi = calculateNeedlePi(n, hits, l, d);
            return "n = " + n + " approximate pi = " + approximatepi;
        }

        public void aggregate(Output out)
        {
            n += out.n;
            hits += out.hits;
            l = out.l;
            d = out.d;
        }
    }

    public static class MCPiAggregator implements Aggregator
    {
        private final Output aggregate;

        public MCPiAggregator(Output aggregate)
        {
            this.aggregate = aggregate;
        }

        public void aggregate(TaskData output)
        {
            Output out = (Output) output.getData();
            log.info("# " + output.getInputId() + " : " + out.showResult());
            aggregate.aggregate(out);
        }
    }

}
