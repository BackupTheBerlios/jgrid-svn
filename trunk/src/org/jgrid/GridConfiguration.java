// $Id:                                                                    $
package org.jgrid;

import org.jgrid.util.MicroContainer;
import org.jgrid.util.NetworkUtil;
import org.jgrid.util.ResourceUtil;
import org.jgrid.util.StringUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

/**
 * TODO: Add class javadoc
 *
 * @author josh Jan 4, 2005 9:46:55 PM
 */
public class GridConfiguration implements MicroContainer.Initializer
{
    public static final String RESOURCE_NAME = "grid.properties";
    public static final String DEFAULT_MCAST_ADDR = "224.0.0.35";
    public static final int DEFAULT_MCAST_PORT = 45566;
    public static final int DEFAULT_STATISTICS_UPDATE_INTERVAL = 15 * 1000;
    public static final int DEFAULT_GRID_STATE_TIMEOUT = 10 * 1000;
    private static final int DEFAULT_SERVER_THREAD_POOL_SIZE = 4;
    private static final boolean DEFAULT_PRINT_LOCAL_ADDR = false;

    private MicroContainer micro;
    private boolean initialized = false;
    private String gridName;
    private String mcastAddress = DEFAULT_MCAST_ADDR;
    private int mcastPort = DEFAULT_MCAST_PORT;
    private boolean valid = false;
    private long statisticsUpdateInterval = DEFAULT_STATISTICS_UPDATE_INTERVAL;
    private long gridStateTimeout = DEFAULT_GRID_STATE_TIMEOUT;
    private int serverThreadPoolSize = DEFAULT_SERVER_THREAD_POOL_SIZE;
    private boolean printLocalAddr = DEFAULT_PRINT_LOCAL_ADDR;

    public GridConfiguration()
    {
        micro = new MicroContainer(this);       // Create a new container, the config is the initializer.
        micro.registerComponentInstance(this);  // Put the configuration in the container.
        // Load 'grid.properties' resource.
        try
        {
            Properties props = ResourceUtil.loadProperties(RESOURCE_NAME);
            if (props != null)
            {
                gridName = props.getProperty("grid.name");
                mcastAddress = props.getProperty("grid.mcast.addr");
                mcastPort = Integer.parseInt(props.getProperty("grid.mcast.port"));
            }
        }
        catch (IOException e)
        {
            throw new GridException("Unable to load 'grid.properties' due to: " + e.getMessage(),e);
        }
    }

    public GridBus getGridBus()
    {
        return (GridBus)getContainer().getComponentInstance(GridBus.class);
    }

    public ClientSession getClientSession()
    {
        return (ClientSession)getContainer().getComponentInstance(ClientSession.class);
    }

    public Server getServer()
    {
        return (Server)getContainer().getComponentInstance(Server.class);
    }

    public MicroContainer getContainer()
    {
        if (micro == null)
        {
            validate();
            micro = new MicroContainer(this);
        }
        return micro;
    }


    public void setGridName(String gridName)
    {
        this.gridName = gridName;
    }

    public String getGridName()
    {
        return gridName;
    }

    public String getChannelProperties()
    {
        InetAddress bindAddr = NetworkUtil.findNonLoopbackAddress();
        String protocol = "UDP(mcast_addr=" + mcastAddress +
                    ";mcast_port=" + mcastPort +
                    (bindAddr != null ? ";bind_addr=" + bindAddr.getCanonicalHostName() : "" ) +
                    ";ip_ttl=32" +
                    ";mcast_send_buf_size=64000;mcast_recv_buf_size=64000" +
                    ";ucast_recv_buf_size=64000;ucast_send_buf_size=32000" +
                    ")";

        // Use TCP sockets for failure detection.  Ping based FD can yeild false failures when
        // a node is too busy to ping.
        String failureDetection = "FD_SOCK:";

        // GMS can print the local address to System.out, which is probably not desireable.
        String printGmsAddr = printLocalAddr ? "true" : "false";
        String gms = "pbcast.GMS(join_timeout=3000;" +
                "down_thread=false;" +
                "join_retry_timeout=2000;" +
                "shun=true;" +
                "print_local_addr="+ printGmsAddr + "):";

        return protocol + ":" +
                "PING(timeout=2000;down_thread=false;num_initial_members=3):" +
                "MERGE2:" +
                failureDetection +
                "VERIFY_SUSPECT(timeout=1500;down_thread=false):" +
                "pbcast.NAKACK(max_xmit_size=60000;down_thread=false;use_mcast_xmit=true;gc_lag=50;retransmit_timeout=300,600,1200,2400,4800):" +
                "UNICAST(timeout=300,600,1200,2400,4800;down_thread=false):" +
                "pbcast.STABLE(stability_delay=1000;desired_avg_gossip=5000;down_thread=false;max_bytes=250000):" +
                gms +
                "FRAG(frag_size=60000;down_thread=false;up_thread=true):" +
                "pbcast.STATE_TRANSFER(down_thread=false;up_thread=false)";
    }

    private void validate()
    {
        if (valid)
            return;
        if (StringUtil.isEmpty(gridName))
            throw new GridException("A grid name must be supplied!");
        valid = true;
    }

    public void initialize(MicroContainer microContainer)
    {
        if (initialized)
            return;
        try
        {
            // Only one instance of GridBus per configuration.
            microContainer.registerSingleton(GridBus.class, "org.jgrid.impl.GridBusImpl");
            // Only one instance of ClientSession per configuration.
            microContainer.registerSingleton(ClientSession.class, "org.jgrid.impl.ClientSessionImpl");
            // Only one instance of Server per configuration.
            microContainer.registerSingleton(Server.class, "org.jgrid.impl.ServerImpl");
            initialized = true;
        }
        catch (Throwable e)
        {
            throw new GridException("Initialization failed: " + e.getMessage(),e);
        }
    }

    public String getNodeName()
    {
        return null;
    }

    public long getStatisticsUpdateInterval()
    {
        return statisticsUpdateInterval;
    }

    public long getGridStateTimeout()
    {
        return gridStateTimeout;
    }

    public int getServerThreadPoolSize()
    {
        return serverThreadPoolSize;
    }
}
