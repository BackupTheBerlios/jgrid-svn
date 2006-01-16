package eg;

import edu.cornell.lassp.houle.RngPack.RandomElement;

/**
 * Implements java.util.Random-like interface using a RngPack RandomElement delegate.
 * <br>User: Joshua Davis
 * Date: Jan 1, 2006
 * Time: 12:33:25 PM
 */
public class RngPackAdapter implements RandomFunction {
    private static final int BITS_PER_BYTE = 8;

    private RandomElement randomElement;
    private static final double MIN = Integer.MIN_VALUE;
    private static final double MAX = Integer.MAX_VALUE;
    private int bytesPerInt;

    public RngPackAdapter(RandomElement randomElement, int bytesPerInt) {
        this.randomElement = randomElement;
        this.bytesPerInt = bytesPerInt;
    }

    public void nextBytes(byte[] buffer) {
        int length = buffer.length;
        int i = 0, rnd = 0;

        while (true) {
            for (int j = 0; j < bytesPerInt; j++) {
                if (i == length)
                    return;
                if (j == 0)
                    rnd = nextInt();
                else
                    rnd = rnd >>> BITS_PER_BYTE;
                buffer[i++] = (byte) rnd;
            }
        }
    }

    protected int nextInt() {
        int i = (int) uniform(MIN, MAX);
        if (bytesPerInt == 3)
            i >>>= 8;
        return i;
    }

    protected double uniform(double min, double max) {
        return randomElement.uniform(min,max);
    }
}
