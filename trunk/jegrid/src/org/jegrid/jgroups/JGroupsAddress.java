package org.jegrid.jgroups;

import org.jegrid.NodeAddress;
import org.jgroups.Address;

/**
 * JGroups version of a node address.
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

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final JGroupsAddress that = (JGroupsAddress) o;
        return address.equals(that.address);
    }

    public int hashCode()
    {
        return address.hashCode();
    }


    public String toString()
    {
        return "JGroupsAddress{" +
                "address=" + address +
                '}';
    }
}
