// $Id:                                                                    $
package org.jgrid.impl;

import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.MethodCall;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;
import org.jgrid.GridEventListener;
import org.jgrid.GridException;
import org.jgrid.PeerInfo;
import org.jgrid.Peers;
import org.jgrid.Server;
import org.jgrid.GridBus;
import org.jgrid.GridConfiguration;
import org.jgrid.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The communications backplane of the grid.
 * @author josh Jan 5, 2005 7:29:56 AM
 */
public class GridBusImpl implements GridBus, MessageConstants
{
    private static final Logger log = Logger.getLogger(GridBusImpl.class);

    private GridConfiguration config;

    private Channel channel;
    // Receiver...
    private MessagePump pump;
    private GridListener listener;
    private GridRpcDispatcher dispatcher;
    private GridRpcTarget handler;
    // Federation...
    private PeersImpl peersImpl;

    private String localAddress;
    private boolean running = false;
    private List eventListeners;

    // The set of services on this node.
    private ServerImpl server;

    // The client side on this node.
    private ClientSessionImpl client;

    /**
     * State of this node. *
     */
    private NodeStateImpl myState;
    public static final int STATE_UPDATE_TIMEOUT = 100;

    private long idCounter;

    public GridBusImpl(GridConfiguration config)
    {
        this.config = config;
        this.eventListeners = new ArrayList();
    }

    Object getComponentInstance(Object key)
    {
        return config.getContainer().getComponentInstance(key);
    }

    GridRpcDispatcher getDispatcher()
    {
        return dispatcher;
    }

    ServerImpl getServer()
    {
        synchronized (this)
        {
            if (server == null)
                server = (ServerImpl) getComponentInstance(Server.class);
            return server;
        }
    }

    ClientSessionImpl getClient()
    {
        synchronized (this)
        {
            if (client == null)
                client = (ClientSessionImpl) getComponentInstance(ClientSession.class);
            return client;
        }
    }

    public String getLocalAddress()
    {
        return localAddress;
    }

    Address getMyAddress()
    {
        return channel.getLocalAddress();
    }

    GridRpcTarget getHandler()
    {
        return handler;
    }

    public void connect()
    {
        synchronized (this)
        {
            if (running)
            {
                if (log.isDebugEnabled())
                    log.debug("connect() : already connected.");
                return;
            }
            log.info("Connecting...");
            doConnect();
            running = true;
            notify();
            log.info(getLocalAddress() + " connected.");
            for (Iterator iterator = eventListeners.iterator(); iterator.hasNext();)
            {
                GridEventListener gridEventListener = (GridEventListener) iterator.next();
                gridEventListener.connected(this);
            }
        }
    }

    private void doConnect()
    {
        try
        {
            channel = new JChannel(config.getChannelProperties());
            channel.setOpt(Channel.VIEW, Boolean.TRUE);
            channel.setOpt(Channel.GET_STATE_EVENTS, Boolean.TRUE);
            channel.connect(config.getGridName());
            if (log.isDebugEnabled())
                log.debug("doConnect() : channel connected.");
            localAddress = channel.getLocalAddress().toString();
            peersImpl = new PeersImpl(this);                        // Handles membership messages.
            listener = new GridListener(this);                      // Handles state change messages.
            handler = new GridRpcTarget(this);                 // Handles RPCs from the MessageDispatcher.
            // Start a message pump thread.  Pass state messages through to the listener
            pump = new MessagePump(channel, listener);
            dispatcher = new GridRpcDispatcher(pump,
                    listener,               // Messages that 'fall through' go here.
                    peersImpl,              // Membership messages go here.
                    handler);               // Rpc's get handled by this object.
            // NOTE: Add other building blocks in the same way.  Make sure they have an 'id'.

            // Set the initial view (members of the grid).
            peersImpl.setInitialView(channel.getView());
            if (log.isDebugEnabled())
                log.debug("doConnect() : Initial view received.");
            myState = new NodeStateImpl(channel.getLocalAddress(), config.getNodeName());
            myState.setStatus(PeerInfo.STATUS_SELF);
            // Ask the coordinator for the state.  This will replace gridState.
            boolean rc = channel.getState(null, config.getGridStateTimeout());
            if (rc)
            {
                log.info(channel.getLocalAddress() + " Waiting for state...");
                listener.waitForGridState();
                log.info(channel.getLocalAddress() + " State received.");
                broadcastNodeStateChange();     // Broadcast a change for myself.
            }
            else
            {
                log.info(channel.getLocalAddress() + " *** State was not retrieved (therefore I am the coordinator)");
                listener.setCoordinator(myState);
            }
        }
        catch (ChannelException e)
        {
            disconnect();
            throw new GridException(e);
        }
        catch (GridException e)
        {
            disconnect();
            throw e;
        }
    }

    void broadcastNodeStateChange()
    {
        dispatcher.gridInvoke(null,"_nodeUpdate",myState,
                GroupRequest.GET_NONE,  // Fire and forget.
                STATE_UPDATE_TIMEOUT);
    }

    public void disconnect()
    {
        synchronized (this)
        {
            if (!running)
                return;
            String localAddress = getLocalAddress();
            // Stop the message pump thread.
            if (pump != null)
            {
                pump.stop();
                pump = null;
            }
            // Stop the message dispatcher.
            if (dispatcher != null)
            {
                dispatcher.stop();
                dispatcher = null;
            }
            // Stop the peer tracker.
            if (peersImpl != null)
            {
                peersImpl.disconnect();
                peersImpl = null;
            }
            // Close the channel.
            if (channel != null)
            {
                channel.close();
                channel = null;
            }
            running = false;
            notify();
            log.info(localAddress + " disconnected.");
            notifyDisconnect();
        }
    }

    public Peers getPeers()
    {
        return peersImpl;
    }

    public void broadcastStop()
    {
        int i = 0;
        int peers = peersImpl.size();
        MethodCall method = new MethodCall("_stop",new Object[0],new Class[0]);
        while (peers > 0)
        {
            RspList list = dispatcher.callRemoteMethods(null,method,GroupRequest.GET_ALL,15000);
            i++;
            log.info("Attempt #" + i + " STOP responses:\n" + list.toString());
            if (peers > 1)
            {
                log.info("Waiting for " + peers + " peers to disconnect...");
                Util.sleep(1000);
            }
            synchronized (this)
            {
                peers = (peersImpl == null) ? 0 : peersImpl.size();
            }
        }
    }

    public boolean isRunning()
    {
        synchronized (this)
        {
            return running;
        }
    }

    public void addEventListener(GridEventListener listener)
    {
        synchronized (this)
        {
            eventListeners.add(listener);
        }
    }

    public void removeEventListener(GridEventListener listener)
    {
        synchronized (this)
        {
            eventListeners.remove(listener);
        }
    }

    public GridConfiguration getConfig()
    {
        return config;
    }


    boolean isLocal(Address sender)
    {
        Address localAddress = (channel == null) ? null : channel.getLocalAddress();
        return sender.compareTo(localAddress) == 0;
    }

    private void notifyDisconnect()
    {
        for (Iterator iterator = eventListeners.iterator(); iterator.hasNext();)
        {
            GridEventListener gridEventListener = (GridEventListener) iterator.next();
            gridEventListener.disconnected(this);
        }
    }

    void notifyPeersChanged()
    {
        for (Iterator iterator = eventListeners.iterator(); iterator.hasNext();)
        {
            GridEventListener gridEventListener = (GridEventListener) iterator.next();
            gridEventListener.peersChanged(this);
        }
    }

    void notifyPeersUpdated()
    {
        for (Iterator iterator = eventListeners.iterator(); iterator.hasNext();)
        {
            GridEventListener gridEventListener = (GridEventListener) iterator.next();
            gridEventListener.peersUpdated(this);
        }
    }

    public GridStateImpl getGridState()
    {
        return listener.getGridState();
    }

    public NodeStateImpl getMyState()
    {
        return myState;
    }

    public void startServer()
    {
        Server server = config.getServer();
        Thread thread = new Thread(server);
        thread.start();
    }

    public String getNextId()
    {
        long id;
        synchronized (this)
        {
            id = idCounter++;
        }
        StringBuffer buf = new StringBuffer();
        buf.append(id).append('@').append(localAddress);
        return buf.toString();
    }

    public GridListener getGridListener()
    {
        return listener;
    }
}
