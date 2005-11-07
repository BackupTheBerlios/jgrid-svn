package org.jgrid.util;

import org.jgrid.GridException;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Inet4Address;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Network utility methods.
 * <br>User: Joshua Davis
 * <br>Date: Oct 22, 2005 Time: 12:38:56 PM
 */
public class NetworkUtil
{
    private static Logger log = Logger.getLogger(NetworkUtil.class);

    public static InetAddress findNonLoopbackAddress()
    {
        try
        {
            Enumeration en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements())
            {
                NetworkInterface i = (NetworkInterface) en.nextElement();
                if (log.isDebugEnabled())
                    log.debug("findNonLoopbackAddress() : Looking for IP4 non-loopback address in: " + i.getDisplayName());
                for (Enumeration en2 = i.getInetAddresses(); en2.hasMoreElements();)
                {
                    InetAddress addr = (InetAddress) en2.nextElement();
                    if ((!addr.isLoopbackAddress()) && (addr instanceof Inet4Address))
                    {
                        log.info("Address = " + addr.toString());
                        return addr;
                    }
                }
            }
            log.warn("No IP4 non-loopback address found!");
            return null;
        }
        catch (SocketException e)
        {
            log.error(e,e);
            throw new GridException(e);
        }
    }

}
