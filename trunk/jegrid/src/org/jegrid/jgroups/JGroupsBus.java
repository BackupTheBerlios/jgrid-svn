package org.jegrid.jgroups;

import org.apache.log4j.Logger;
import org.jegrid.GridConfiguration;
import org.jegrid.GridException;
import org.jegrid.Bus;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;

/**
 * JGroups implementation of the messaging layer.
 * <br>User: jdavis
 * Date: Sep 21, 2006
 * Time: 5:10:41 PM
 */
public class JGroupsBus implements Bus
{
    private static Logger log = Logger.getLogger(JGroupsBus.class);
    private static final String DEFAULT_PROPS = "UDP1_4(mcast_addr=228.8.8.8;mcast_port=27000;ip_ttl=1;" +
            "mcast_send_buf_size=64000;mcast_recv_buf_size=64000):" +
            "PING(timeout=2000;num_initial_members=3):" +
            "MERGE2(min_interval=5000;max_interval=10000):" +
            "FD_SOCK:" +
            "VERIFY_SUSPECT(timeout=1500):" +
            "pbcast.NAKACK(max_xmit_size=8096;gc_lag=50;retransmit_timeout=300,600,1200,2400,4800):" +
            "UNICAST(timeout=600,1200,2400,4800):" +
            "pbcast.STABLE(desired_avg_gossip=20000):" +
            "FRAG(frag_size=8096;down_thread=false;up_thread=false):" +
            "pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;" +
            "shun=false;print_local_addr=true)";

    private boolean running = false;
    private Channel channel;
    private GridConfiguration config;
    private String localAddress;

    public JGroupsBus(GridConfiguration config)
    {
        this.config = config;
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
        }
    }

    public String getLocalAddress()
    {
        return localAddress;
    }

    private void doConnect() {
        try {
            if (channel == null)
            {
                if (config.getBusConfiguration() == null)
                    channel = new JChannel(DEFAULT_PROPS);
                else
                    channel = new JChannel(config.getBusConfiguration());
            }
            channel.setOpt(Channel.VIEW, Boolean.TRUE);
            channel.setOpt(Channel.GET_STATE_EVENTS, Boolean.TRUE);
            if (config.getGridName() == null || config.getGridName().length() == 0)
                throw new GridException("No grid name.  Please provide a grid name so the grid can federate.");
            channel.connect(config.getGridName());
            if (log.isDebugEnabled())
                log.debug("doConnect() : channel connected.");
            localAddress = channel.getLocalAddress().toString();
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

    public void disconnect() {
        synchronized (this) {
            if (!running)
                return;
            String localAddress = getLocalAddress();

            // Close the channel.
            if (channel != null) {
                channel.close();
                channel = null;
            }
            running = false;
            notify();
            log.info(localAddress + " disconnected.");
        }
    }

}
