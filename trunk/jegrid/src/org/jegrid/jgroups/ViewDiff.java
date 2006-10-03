package org.jegrid.jgroups;

import org.jgroups.View;

import java.util.Set;
import java.util.Vector;
import java.util.HashSet;

/**
 * Computes the members that joined or left a view.
 * <br>User: Joshua Davis
 * Date: Oct 3, 2006
 * Time: 7:33:18 AM
 */
class ViewDiff
{
    private Set joined;
    private Set left;

    public ViewDiff(View oldView,View newView)
    {
        Vector oldMembers = oldView == null ? new Vector() : oldView.getMembers();
        Vector newMembers = newView.getMembers();
        // Taken from DistributedQueue building block code.  It's not great, but it works.
        // 1. Compute set of members that joined: all that are in new, but not in old
        joined = new HashSet();
        Object mbr;
        for (int i = 0; i < newMembers.size(); i++)
        {
            mbr = newMembers.elementAt(i);
            if (!oldMembers.contains(mbr))
                joined.add(mbr);
        }

        // 2. Compute set of members that left: all that were in old, but not in new
        left = new HashSet();
        for (int i = 0; i < oldMembers.size(); i++)
        {
            mbr = oldMembers.elementAt(i);
            if (!newMembers.contains(mbr))
                left.add(mbr);
        }
    }

    public Set getJoined()
    {
        return joined;
    }

    public Set getLeft()
    {
        return left;
    }
}
