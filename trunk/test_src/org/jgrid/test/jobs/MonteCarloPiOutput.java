package org.jgrid.test.jobs;

import java.io.Serializable;

/**
 * Serializable output for monte carlo pi.
 * <br>User: Joshua Davis
 * Date: Mar 4, 2006
 * Time: 12:54:25 PM
 */
public class MonteCarloPiOutput implements Serializable
{
    private int n;
    private int hits;
    private double l;
    private double d;

    public MonteCarloPiOutput(int n, int hits, double l, double d)
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
}
