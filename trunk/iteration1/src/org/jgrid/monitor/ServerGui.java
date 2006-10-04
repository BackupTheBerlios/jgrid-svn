package org.jgrid.monitor;

import org.jgrid.PeerInfo;
import org.jgrid.GridBus;
import org.jgrid.GridConfiguration;
import org.jgrid.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.util.List;
import java.util.Iterator;

/**
 * <br>User: Joshua Davis
 * <br>Date: Oct 2, 2005 Time: 7:38:12 AM
 */
public class ServerGui implements GridEventListener
{
    private JFrame frame;
    private JPanel contentPane;
    private Document messages;
    private GridBus bus;
    private DefaultTableModel tableModel;

    public ServerGui()
    {
        frame = new JFrame("Grid Server");
        contentPane = new JPanel(new GridLayout(3, 1));
        // First row: a panel with all the grid properties in it.
        GridControlPanel panel = new GridControlPanel(this);
        contentPane.add(panel.getPanel());
        // Second row: a scroll pane with a JTable in it.
        GridConfiguration config = new GridConfiguration();
        bus = config.getGridBus();
        bus.addEventListener(this);
        tableModel = new DefaultTableModel(new String[] { "address", "name", "status", "processors", "free memory", "total memory" },0);
        JTable table = new JTable(tableModel);
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        JScrollPane scrollPane = new JScrollPane(table);

        //Add the scroll pane to this panel.
        contentPane.add(scrollPane);

        // Third row: A text window for messages.
        messages = new PlainDocument();
        JTextArea text = new JTextArea(messages);
        JScrollPane textScroll = new JScrollPane(text);
        contentPane.add(textScroll);
    }

    private void addMessage(String str)
    {
        try
        {
            messages.insertString(messages.getLength(),str + "\n",null);
        }
        catch (BadLocationException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void showFrame(JFrame frame, JPanel contentPane)
    {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        contentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(contentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public void run()
    {
        showFrame(frame,contentPane);
    }

    public void connected(GridBus bus)
    {
        addMessage("started");
    }

    public void disconnected(GridBus bus)
    {
        addMessage("stopped");
        tableModel.setNumRows(0);
    }

    public void peersChanged(GridBus bus)
    {
        addMessage("peers changed");
        showPeerInfo(bus);
    }

    private void showPeerInfo(GridBus bus)
    {
        List peers = bus.getPeers().getPeerInfoList();
        int i = 0;
        tableModel.setNumRows(peers.size());
        for (Iterator iterator = peers.iterator(); iterator.hasNext(); i++)
        {
            PeerInfo peerInfo = (PeerInfo) iterator.next();
            int j = 0;
            String address = peerInfo.getAddress();
            tableModel.setValueAt(address,i,j++);
            tableModel.setValueAt(peerInfo.getName(),i,j++);
            tableModel.setValueAt(getStatusString(peerInfo.getStatus()),i,j++);
            tableModel.setValueAt(new Integer(peerInfo.getProcessors()),i,j++);
            tableModel.setValueAt(new Long(peerInfo.getFreeMemory()),i,j++);
            tableModel.setValueAt(new Long(peerInfo.getTotalMemory()),i,j++);
        }
    }

    private String getStatusString(int status)
    {
        switch (status)
        {
            case PeerInfo.STATUS_OK: return "ok";
            case PeerInfo.STATUS_COORDINATOR: return "coordinator";
            case PeerInfo.STATUS_SUSPECT: return "suspect";
            case PeerInfo.STATUS_SELF: return "self";
            default: return "???";
        }
    }

    public void peersUpdated(GridBus gridBus)
    {
        addMessage("peers updated");
        showPeerInfo(bus);
    }

    public static void main(String[] args)
    {
        ServerGui monitor = new ServerGui();
        monitor.run();
    }

    public void connect()
    {
        if (bus.isRunning())
        {
            addMessage("already connected.");
            return;
        }
        bus.connect();
        bus.startServer();
        addMessage("connected.");
    }

    public void disconnect()
    {
        if (bus.isRunning())
        {
            bus.disconnect();
            addMessage("disconnected.");
            return;
        }
    }

    public void stop()
    {
        if (bus.isRunning())
        {
            bus.broadcastStop();
            addMessage("stopped grid.");
            disconnect();
        }
    }
}
