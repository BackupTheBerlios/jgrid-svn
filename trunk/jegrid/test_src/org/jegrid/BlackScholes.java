package org.jegrid;

/**
 * http://en.wikipedia.org/wiki/Black-Scholes
 * <br>User: Joshua Davis
 * Date: Nov 5, 2006
 * Time: 3:02:06 PM
 */
public class BlackScholes
{
    /**
     * Prices European style options usign the Black-Scholes formula.
     * @param call true for call options, false for puts
     * @param S the price of the underlying
     * @param K the strike of the option
     * @param T the time to expiration, in years
     * @param r the risk free interest rate
     * @param v the volatility
     * @return the Black-Scholes option price
     */
    public static double blackScholes(boolean call, double S, double K, double T, double r, double v)
    {
        double d1, d2;

        double sqrtT = Math.sqrt(T);
        double vsqrtT = v * sqrtT;
        d1 = (Math.log(S / K) + (r + v * v / 2) * T) / vsqrtT;
        d2 = d1 - vsqrtT;

        if (call)
            return S * N(d1) - K * Math.exp(-r * T) * N(d2);
        else
            return K * Math.exp(-r * T) * N(-d2) - S * N(-d1);
    }

    /**
     * @param X the value
     * @return the cumulative normal distribution for the value
     */
    public static double N(double X)
    {
        double L, K, w;
        double a1 = 0.31938153, a2 = -0.356563782, a3 = 1.781477937, a4 = -1.821255978, a5 = 1.330274429;

        L = Math.abs(X);
        K = 1.0 / (1.0 + 0.2316419 * L);
        w = 1.0 - 1.0 / Math.sqrt(2.0 * Math.PI) * Math.exp(-L * L / 2) * (a1 * K + a2 * K * K + a3
                * Math.pow(K, 3) + a4 * Math.pow(K, 4) + a5 * Math.pow(K, 5));

        if (X < 0.0)
            w = 1.0 - w;
        return w;
    }
}
