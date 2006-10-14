package org.jegrid.impl;

import org.jegrid.NodeStatus;

import java.util.Comparator;

/**
 * Orders servers by their ability to do work.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 10:50:36 AM
 */
public class ServerComparator implements Comparator
{

    public int compare(Object o1, Object o2)
    {
        NodeStatus n1 = (NodeStatus) o1;
        NodeStatus n2 = (NodeStatus) o2;
        // Primary criteria: Nodes with zero free threads should always go last.
        if (n1.getAvailableWorkers() == 0 || n2.getAvailableWorkers() == 0)
            return n2.getAvailableWorkers() - n1.getAvailableWorkers();
        // Secondary criteria: Nodes with a greater ratio of free threads are preferred.
        int diff = ratioDiff(
                n1.getAvailableWorkers(), n1.getTotalWorkers(),
                n2.getAvailableWorkers(), n2.getTotalWorkers());
        if (diff != 0)
            return diff;
        // Tertiary criteria: Nodes with more memory per free thread are preferred.
        // Note that divide by zero is prevented by the first criteria.
        diff = ratioDiff(
                n1.getFreeMemory(), n1.getAvailableWorkers(),
                n2.getFreeMemory(), n2.getAvailableWorkers()
        );
        return diff;
    }

    private int ratioDiff(float a1, float b1, float a2, float b2)
    {
        float ratio1 = a1 / b1;
        float ratio2 = a2 / b2;
        float diff = ratio2 - ratio1;
        if (Math.abs(diff) > 0.0001)
            return diff > 0 ? 1 : -1;
        else
            return 0;
    }
}
