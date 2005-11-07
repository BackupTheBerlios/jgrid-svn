package org.jgrid;

/**
 * <br>User: Joshua Davis
 * <br>Date: Oct 2, 2005 Time: 8:56:27 AM
 */
public interface PeerInfo
{
    int STATUS_OK = 0;
    int STATUS_COORDINATOR = 1;
    int STATUS_SUSPECT = -1;
    int STATUS_UNKNOWN = -2;
    int STATUS_SELF = 2;

    String getAddress();

    String getName();

    int getStatus();

    int getProcessors();

    long getFreeMemory();

    long getTotalMemory();

    int getFreeThreads();
}
