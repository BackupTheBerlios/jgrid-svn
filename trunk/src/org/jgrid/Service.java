package org.jgrid;

import java.io.Serializable;

/**
 * Executes an algorithm with a single input and a single output.
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 12:49:59 PM
 */
public interface Service
{
    public Serializable execute(Serializable input);
}
