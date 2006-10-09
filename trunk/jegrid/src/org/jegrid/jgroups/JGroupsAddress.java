package org.jegrid.jgroups;

import org.jegrid.NodeAddress;
import org.jgroups.Address;

import java.io.Serializable;

/**
 * JGroups version of a node address.
 * <br> User: jdavis
 * Date: Sep 30, 2006
 * Time: 9:21:13 PM
 */
public class JGroupsAddress implements NodeAddress, Serializable
{
    private static final long serialVersionUID = 4218752161405881705L;

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
        return (address == null) ? "null" : address.toString();
    }

    Address getAddress()
    {
        return address;
    }
}
