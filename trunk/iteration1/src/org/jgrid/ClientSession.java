// $Id:                                                                    $
package org.jgrid;

/**
 * Client's entry point into the grid. JobFactory of sorts...
 *
 * @author josh Jan 29, 2005 9:56:22 AM
 */
public interface ClientSession
{
    /**
     * Create a job handle with the given service class.
     * @param aClass the service class
     * @return a new job handle
     */
    Job createJob(Class aClass);
}
