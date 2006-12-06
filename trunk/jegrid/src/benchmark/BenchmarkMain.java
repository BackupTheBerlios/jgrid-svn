package benchmark;

/**
 * A command line version of the benchmark applet.
 * <br> User: jdavis
 * Date: Dec 6, 2006
 * Time: 7:16:51 AM
 */
public class BenchmarkMain implements BenchmarkRunner
{
    private Benchmark[]			tests = {
                                    new Benchmark(this),
                                    new MixedBenchmark(this),
                                    new LoopBenchmark(this),
                                    new VariableBenchmark(this),
                                    new MethodBenchmark(this),
                                    new OperatorBenchmark(this),
                                    new CastingBenchmark(this),
                                    new InstantiationBenchmark(this),
                                    new ExceptionBenchmark(this),
                                    new ThreadBenchmark(this)
                                };

    public static void main(String[] args)
    {
        BenchmarkMain b = new BenchmarkMain();
        b.mixedBenchmark();
    }

    public void report(String msg, long nanoseconds)
    {
        System.out.println(msg + " " + nanoseconds);
    }

    public void println(String s)
    {
        System.out.println(s);
    }

    private void run(String[] args)
    {
        Benchmark.recalibrate();

        long	begin = System.currentTimeMillis();
        Benchmark.gc();
        println("Benchmark tests:  [mem=" + Runtime.getRuntime().freeMemory() +"/" +
                                            Runtime.getRuntime().totalMemory() +"]");

        int testSeconds = 0;
        int estimated = 0;
        for (int i = 0; i < tests.length; i++)
        {
            Benchmark test = tests[i];
            estimated += test.getRunningTime();
            testSeconds += test.getTestTime();
        }
        println("Estimated time: " + Benchmark.timeString(estimated) + " (tests " +
                Benchmark.timeString(testSeconds) + ")");

        long total = 0;
        for (int i = 0; i < tests.length; i++)
        {
            Benchmark test = tests[i];
            total += test.runTest();
        }

        println("*** done: " +
                Benchmark.timeString((int) (System.currentTimeMillis() + 500 - begin) / 1000) +
                " (tests " +
                Benchmark.timeString((int) ((total + 500) / 1000)) +
                ") ***");
    }

    public double mixedBenchmark()
    {
        Benchmark.recalibrate();

        long	begin = System.currentTimeMillis();
        Benchmark.gc();
        println("Mixed benchmark:  [mem=" + Runtime.getRuntime().freeMemory() +"/" +
                                            Runtime.getRuntime().totalMemory() +"]");

        int testSeconds = 0;
        int estimated = 0;
        for (int i = 0; i < 2; i++)
        {
            Benchmark test = tests[i];
            estimated += test.getRunningTime();
            testSeconds += test.getTestTime();
        }
        println("Estimated time: " + Benchmark.timeString(estimated) + " (tests " +
                Benchmark.timeString(testSeconds) + ")");

        long total = 0;
        for (int i = 0; i < 2; i++)
        {
            Benchmark test = tests[i];
            total += test.runTest();
        }

        println("*** done: " +
                Benchmark.timeString((int) (System.currentTimeMillis() + 500 - begin) / 1000) +
                " (tests " +
                Benchmark.timeString((int) ((total + 500) / 1000)) +
                ") ***");
        return total;
    }
}
