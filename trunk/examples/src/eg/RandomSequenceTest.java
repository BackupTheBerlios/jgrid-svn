package eg;

import edu.cornell.lassp.houle.RngPack.Ranlux;
import edu.cornell.lassp.houle.RngPack.Ranmar;
import edu.cornell.lassp.houle.RngPack.Ranecu;

import java.util.Random;
import java.util.Arrays;

/**
 * TODO: Add class level javadoc.
 * <br>User: Joshua Davis
 * Date: Dec 31, 2005
 * Time: 6:08:10 PM
 */
public class RandomSequenceTest {


    /**
     * Number of bytes needed to compute a Monte Carlo PI calculation.
     */
    private static final int MC_XY_BYTES = 6;

    /**
     * Limit for selecting points relative to the Monte Carlo circle.
     */
    private static final double INSIDE_CIRCLE =
            Math.pow(Math.pow(256.0, MC_XY_BYTES / 2.0) - 1, 2.0);

    /**
     * Table of chi-square Xp values versus corresponding probabilities.
     */
    private static final double[][] CHI_SQUARE_P = new double[][]{
            {0.5, 0.25, 0.1, 0.05, 0.025, 0.01, 0.005, 0.001, 0.0005, 0.0001},
            {0.0, 0.6745, 1.2816, 1.6449, 1.9600, 2.3263, 2.5758, 3.0902, 3.2905, 3.7190}
    };

    private RandomFunction randomFunction;

    private byte[] buffer = new byte[1024];

    private long duration;

    private long[] counters = new long[2];
    private long totalBits;
    private long[] bytecounters = new long[256];
    private long totalBytes;

    private byte[] mcBuffer = new byte[6];
    private int mcBufferNdx;
    private double mcCount, mcInside;

    /**
     * Serial Correlation Coefficient work variables.
     */
    private boolean sccFirst;
    private double scc;
    private double sccLast;
    private double sccU0;
    private double sccT1;
    private double sccT2;
    private double sccT3;

    /**
     * Other variables.
     */
    private double chiSquare, mean, pi;

    int iterations = 4096;
    private static final int HALF_MC_XY_BYTES = (MC_XY_BYTES / 2);

    public RandomFunction getRandomFunction() {
        return randomFunction;
    }

    public void setRandomFunction(RandomFunction randomFunction) {
        this.randomFunction = randomFunction;
    }

    public void computeIndices() {
        initInternal();
        duration = -System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            randomFunction.nextBytes(buffer);
            update(buffer);
        }
        computeResults();
        duration += System.currentTimeMillis();
    }

    public long getDuration() {
        return duration;
    }

    public long getTotalBits() {
        return (long) totalBits;
    }

    public long getSetBits() {
        return (long) counters[1];
    }

    public double getMean() {
        return mean;
    }

    public double getMeanPercentDeviation() {
        return 100.0 * (Math.abs(0.5 - mean) / 0.5);
    }

    public double getChiSquare() {
        return chiSquare;
    }

    public double getChiSquareProbability() {
        double chip = Math.sqrt(2.0 * chiSquare) - 1.0;
        double a = Math.abs(chip);
        int i = 10;
        for (; --i >= 0;) {
            if (CHI_SQUARE_P[1][i] < a) {
                break;
            }
        }
        chip = (chip >= 0.0) ? CHI_SQUARE_P[0][i] : 1.0 - CHI_SQUARE_P[0][i];
        return chip * 100.0;
    }

    public double getSerialCorrelationCoefficient() {
        return scc;
    }

    public double getPi() {
        return pi;
    }

    public double getPiPercentDeviation() {
        return 100.0 * (Math.abs(Math.PI - pi) / Math.PI);
    }

    private void initInternal() {
        Arrays.fill(counters,0L);
        totalBits = 0L;
        Arrays.fill(bytecounters,0L);
        totalBytes = 0L;

        initMonteCarloBuffer();
        sccFirst = true;

        chiSquare = 0.0;

        mcCount = 0.0;
        mcInside = 0.0;
    }

    private void initMonteCarloBuffer() {
        Arrays.fill(mcBuffer, (byte) 0);
        mcBufferNdx = 0;
    }

    private void update(byte[] buffer) {
        byte b;
        for (int i = 0; i < buffer.length; i++) {
            b = buffer[i];
            updateBitCountAndSCC(b);
            updateMonteCarloPI(b);
        }
    }

    private void updateBitCountAndSCC(byte b) {
        bytecounters[b - Byte.MIN_VALUE]++;
        totalBytes++;
        int limit = 8;
        for (int i = 0; i < limit; i++) {
            totalBits++;
            counters[(b >>> 7) & 0x01]++;   // Update the '1' or the '0' counter.

            double sccUn = b & 0x80;
            if (sccFirst) {
                sccFirst = false;
                sccLast = 0.0;
                sccU0 = sccUn;
            } else {
                sccT1 += sccLast * sccUn;
            }
            sccT2 += sccUn;
            sccT3 += sccUn * sccUn;
            sccLast = sccUn;
            b <<= 1;
        }
    }

    private void updateMonteCarloPI(byte b) {
        mcBuffer[mcBufferNdx] = b;
        mcBufferNdx++;
        if (mcBufferNdx >= MC_XY_BYTES) {
            computeMonteCarloPI();
            initMonteCarloBuffer();
        }
    }

    private void computeMonteCarloPI() {
        mcCount++;
        double x = 0.0;
        double y = 0.0;
        for (int i = 0; i < HALF_MC_XY_BYTES; i++) {
            x = (x * 256.0) + (mcBuffer[i] & 0xFF);
            y = (y * 256.0) + (mcBuffer[HALF_MC_XY_BYTES + i] & 0xFF);
        }
        if ((x * x + y * y) <= INSIDE_CIRCLE) {
            mcInside++;
        }
    }

    private void computeResults() {
        // complete calculation of serial correlation coefficient
        sccT1 += sccLast * sccU0;
        sccT2 = sccT2 * sccT2;
        scc = totalBits * sccT3 - sccT2;
        if (scc == 0.0) {
            scc = -100000;
        } else {
            scc = (totalBits * sccT1 - sccT2) / scc;
        }

        // compute Chi-Square distribution
        double cexp = totalBits / 2.0;  // expected count per bit counter
        double a = counters[0] - cexp;
        double b = counters[1] - cexp;
        chiSquare = (a * a + b * b) / cexp;
        mean = counters[1] * 1.0 / totalBits;

        // compute Monte Carlo value for PI from % of hits within the circle
        pi = 4.0 * mcInside / mcCount;
    }

    public void printResults() {
        System.out.println("----- " + randomFunction + " -----");
        System.out.println("Total execution time (ms): " + String.valueOf(duration));
        System.out.println("                  Total bit count: "
                + String.valueOf((long) getTotalBits()));
        System.out.println("           Mean value of set bits: "
                + String.valueOf(getMean()));
        System.out.println("                 Mean % deviation: "
                + String.valueOf(getMeanPercentDeviation()));
        System.out.println("          Chi-square distribution: "
                + String.valueOf(getChiSquare()));
        System.out.println("  Chi-square excess % probability: "
                + String.valueOf(getChiSquareProbability()));
        System.out.println("                      Computed PI: "
                + String.valueOf(getPi()));
        System.out.println("          Computed PI % deviation: "
                + String.valueOf(getPiPercentDeviation()));
        System.out.println("   Serial Correlation Coefficient: "
                + String.valueOf(getSerialCorrelationCoefficient()));
        double minratio = Double.MAX_VALUE;
        double maxratio = Double.MIN_VALUE;
        int minbyte = -1;
        int maxbyte = -1;
        for (int i = 0; i < bytecounters.length; i++) {
            long bytecounter = bytecounters[i];
            if (bytecounter == 0)
                System.out.println("Byte " + i + " did not occur!");
            double ratio = ((double)bytecounter) / ((double)totalBytes);
            if (ratio < minratio)
            {
                minratio = ratio;
                minbyte = i;
            }
            if (ratio > maxratio)
            {
                maxratio = ratio;
                maxbyte = i;
            }
        }
        System.out.println("           Maximum byte frequency: " + maxbyte + " frequency " + maxratio);
        System.out.println("           Minimum byte frequency: " + minbyte + " frequency " + minratio);
    }

    public static void main(String[] args) {
        try {
            new RandomSequenceTest().run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

    private void run() {
        testRandomFunction(new JavaRandomFunction());
        testRandomFunction(new RanluxFunction(0, Ranmar.DEFSEED));
        testRandomFunction(new RanluxFunction(1, Ranmar.DEFSEED));
        testRandomFunction(new RanluxFunction(2, Ranmar.DEFSEED));
        testRandomFunction(new RanluxFunction(3, Ranmar.DEFSEED));
        testRandomFunction(new RanmarFunction());
        testRandomFunction(new RanecuFunction());
    }

    private void testRandomFunction(RandomFunction randomFunction) {
        setRandomFunction(randomFunction);
        computeIndices();
        printResults();
    }

    private static class RanluxFunction extends RngPackAdapter {
        private int luxuryLevel;
        private long seed;

        public RanluxFunction(int luxuryLevel, long seed) {
            super(new Ranlux(luxuryLevel, seed), 3);
            this.luxuryLevel = luxuryLevel;
            this.seed = seed;
        }

        public String toString() {
            return "Ranlux(" + luxuryLevel + "," + seed + ")";
        }
    }

    private static class JavaRandomFunction extends RngPackAdapter {
        private Random javaRandom;

        public JavaRandomFunction() {
            super(null, 4);
            javaRandom = new Random(Ranmar.DEFSEED);
        }

        protected double uniform(double min, double max) {
            return min + (max - min)*javaRandom.nextDouble();
        }

        public String toString() {
            return "java.util.Random(" + Ranmar.DEFSEED + ")";
        }
    }

    private static class RanmarFunction extends RngPackAdapter {

        public RanmarFunction() {
            super(new Ranmar(), 3);
        }

        public String toString() {
            return "Ranmar()";
        }
    }

    private static class RanecuFunction extends RngPackAdapter {

        public RanecuFunction() {
            super(new Ranecu(), 4);
        }

        public String toString() {
            return "Ranecu()";
        }
    }
}
