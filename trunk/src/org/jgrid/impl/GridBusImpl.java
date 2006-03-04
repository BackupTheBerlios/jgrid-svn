// $Id:                                                                    $
package org.jgrid.impl;

import org.apache.log4j.Logger;
import org.jgrid.*;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.MethodCall;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

/**
 * The communications backplane of the grid.
 *
 * @author josh Jan 5, 2005 7:29:56 AM
 */
public class GridBusImpl implements GridBus, MessageConstants {
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

    // Event listeners.
    private Notifier notifier;

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

    public GridBusImpl(GridConfiguration config) {
        this.config = config;
        notifier = new Notifier(this);
    }

    Object getComponentInstance(Object key) {
        return config.getContainer().getComponentInstance(key);
    }

    GridRpcDispatcher getDispatcher() {
        return dispatcher;
    }

    Notifier getNotifier() {
        return notifier;
    }

    public void addEventListener(GridEventListener listener) {
        notifier.addEventListener(listener);
    }

    public void removeEventListener(GridEventListener listener) {
        notifier.removeEventListener(listener);
    }

    ServerImpl getServer() {
        synchronized (this) {
            if (server == null)
                server = (ServerImpl) getComponentInstance(Server.class);
            return server;
        }
    }

    ClientSessionImpl getClient() {
        synchronized (this) {
            if (client == null)
                client = (ClientSessionImpl) getComponentInstance(ClientSession.class);
            return client;
        }
    }

    public String getLocalAddress() {
        return localAddress;
    }

    Address getMyAddress() {
        return channel.getLocalAddress();
    }

    GridRpcTarget getHandler() {
        return handler;
    }

    public void connect() {
        synchronized (this) {
            if (running) {
                return;
            }
            log.info("Connecting...");
            doConnect();
            running = true;
            notify();
            log.info(getLocalAddress() + " connected.");
            notifier.notifyConnected();
        }
    }

    private void doConnect() {
        try {
            if (channel == null)
                channel = new JChannel(config.getChannelProperties());
            channel.setOpt(Channel.VIEW, Boolean.TRUE);
            channel.setOpt(Channel.GET_STATE_EVENTS, Boolean.TRUE);
            channel.connect(config.getGridName());
            if (log.isDebugEnabled())
                log.debug("doConnect() : channel connected.");
            localAddress = channel.getLocalAddress().toString();
            peersImpl = new PeersImpl(this);                        // Handles membership messages.
            listener = new GridListener(this);                      // Handles state change messages.
            handler = new GridRpcTarget(this);                      // Handles RPCs from the MessageDispatcher.
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
            if (rc) {
                log.info(channel.getLocalAddress() + " Waiting for state...");
                listener.waitForGridState();
                log.info(channel.getLocalAddress() + " State received.");
                broadcastNodeStateChange();     // Broadcast a change for myself.
            } else {
                log.info(channel.getLocalAddress() + " *** State was not retrieved (therefore I am the coordinator)");
                listener.setCoordinator(myState);
            }
            dispatcher.setReady(true);
        }
        catch (ChannelException e) {
            disconnect();
            throw new GridException(e);
        }
        catch (GridException e) {
            disconnect();
            throw e;
        }
    }

    void broadcastNodeStateChange() {
        dispatcher.gridInvoke(null, "_nodeUpdate", myState,
                GroupRequest.GET_NONE,  // Fire and forget.
                STATE_UPDATE_TIMEOUT);
    }

    void stop() {
        if (pump != null)
            pump.stop();
        if (dispatcher != null)
            dispatcher.stop();
        if (peersImpl != null)
            peersImpl.stop();
    }

    public void disconnect() {
        synchronized (this) {
            if (!running)
                return;
            String localAddress = getLocalAddress();
            // Stop the message pump thread.
            stop();
            pump = null;
            dispatcher = null;

            // Close the channel.
            if (channel != null) {
                channel.close();
                channel = null;
            }
            running = false;
            notify();
            log.info(localAddress + " disconnected.");
            notifier.notifyDisconnect();
        }
    }

    public Peers getPeers() {
        if (peersImpl == null)
            throw new GridException("Not connected.");
        return peersImpl;
    }

    public void broadcastStop() {
        int i = 0;
        int peers = getNumberOfPeers();
        MethodCall method = new MethodCall("_stop", new Object[0], new Class[0]);
        while (peers > 0) {
            RspList list = dispatcher.callRemoteMethods(null, method, GroupRequest.GET_ALL, 15000);
            i++;
            log.info("Attempt #" + i + " STOP responses:\n" + list.toString());
            if (peers > 1) {
                log.info("Waiting for " + peers + " peers to disconnect...");
                Util.sleep(1000);
            }
            peers = getNumberOfPeers();
        }
    }

    private int getNumberOfPeers()
    {
        synchronized (this)
        {
            return (peersImpl == null) ? 0 : peersImpl.size();
        }
    }

    public boolean isRunning() {
        synchronized (this) {
            return running;
        }
    }

    public GridConfiguration getConfig() {
        return config;
    }


    boolean isLocal(Address sender) {
        Address localAddress = (channel == null) ? null : channel.getLocalAddress();
        return sender.compareTo(localAddress) == 0;
    }

    public GridStateImpl getGridState() {
        return listener.getGridState();
    }

    public NodeStateImpl getMyState() {
        return myState;
    }

    public void startServer() {
        Server server = config.getServer();
        Thread thread = new Thread(server);
        thread.start();
    }

    public void setChannel(Object jgroupsChannel) {
        channel = (Channel) jgroupsChannel;
    }

    public Object getChannel() {
        return channel;
    }

    public String getNextId() {
        long id;
        synchronized (this) {
            if (!running)
                throw new GridException("GridBus is not running!");
            id = idCounter++;
        }
        StringBuffer buf = new StringBuffer();
        buf.append(id).append('@').append(localAddress);
        return buf.toString();
    }

    public GridListener getGridListener() {
        return listener;
    }

    public String getGridName() {
        return config.getGridName();
    }
}
