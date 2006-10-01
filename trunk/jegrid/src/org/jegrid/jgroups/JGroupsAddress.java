package org.jegrid.jgroups;

import org.jegrid.NodeAddress;
import org.jgroups.Address;

/**
 * TODO: Add class level javadoc
 * <br> User: jdavis
 * Date: Sep 30, 2006
 * Time: 9:21:13 PM
 */
public class JGroupsAddress implements NodeAddress
{
    private Address address;

    public JGroupsAddress(Address address)
    {
        this.address = address;
    }

    public String toString()
    {
        return address.toString();
    }
}
