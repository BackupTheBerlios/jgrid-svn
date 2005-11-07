package org.jgrid.impl;

import org.jgroups.View;

import java.util.Vector;
import java.util.Set;
import java.util.HashSet;

/**
 * Determines the difference between to JGroups views.
 * <br>User: Joshua Davis
 * <br>Date: Oct 24, 2005 Time: 7:45:42 AM
 */
public class ViewDiff
{
    private View oldView;
    private View newView;
    private Vector joined;
    private Vector left;
    private boolean diffDone = false;

    public ViewDiff(View oldView, View newView)
    {
        this.oldView = oldView;
        this.newView = newView;
    }

    public Vector getJoined()
    {
        doDiff();
        return joined;
    }

    public Vector getLeft()
    {
        doDiff();
        return left;
    }
    
    private void doDiff()
    {
        if (diffDone)
            return;

        Vector oldMembers = oldView.getMembers();
        Set    oldMemberSet = new HashSet(oldMembers);
        Vector newMembers = newView.getMembers();
        Set    newMemberSet = new HashSet(newMembers);
        Object mbr;

        if ((oldMembers == null) || (newMembers == null) || (oldMembers.size() == 0) ||
                (newMembers.size() == 0))
        {
            return;
        }

        // 1. Compute set of members that joined: all that are in newMembers, but not in oldMembers
        joined = new Vector();
        for (int i = 0; i < newMembers.size(); i++)
        {
            mbr = newMembers.elementAt(i);
            if (!oldMemberSet.contains(mbr))
            {
                joined.addElement(mbr);
            }
        }

        // 2. Compute set of members that left: all that were in oldMembers, but not in newMembers
        left = new Vector();
        for (int i = 0; i < oldMembers.size(); i++)
        {
            mbr = oldMembers.elementAt(i);
            if (!newMemberSet.contains(mbr))
            {
                left.addElement(mbr);
            }
        }
        diffDone = true;
    }
}
