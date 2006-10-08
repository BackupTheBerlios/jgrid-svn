package org.jegrid.gui;

import javax.swing.*;

/**
 * TODO: Add class level javadoc.
 * <br>User: Joshua Davis
 * Date: Oct 8, 2006
 * Time: 8:43:22 AM
 */
public class Monitor
{
    public static void main(String[] args)
    {
        final JFrame frame = new JFrame("Grid Monitor");
        MonitorForm form = new MonitorForm();
        frame.setContentPane(form.mainPanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.show();
    }
}
