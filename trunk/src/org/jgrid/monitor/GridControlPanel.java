package org.jgrid.monitor;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * User: Joshua Davis<br>
 * Date: Oct 2, 2005<br>
 * Time: 8:24:54 AM<br>
 */
public class GridControlPanel
{
    private GridMonitor monitor;
    private JButton connect;
    private JPanel panel;
    private JButton disconnect;
    private JButton stop;

    public GridControlPanel(GridMonitor monitor)
    {
        this.monitor = monitor;
        connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                GridControlPanel.this.monitor.connect();
            }
        });

        disconnect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                GridControlPanel.this.monitor.disconnect();
            }
        });

        stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                GridControlPanel.this.monitor.stop();
            }
        });
    }

    public JPanel getPanel()
    {
        return panel;
    }
}
