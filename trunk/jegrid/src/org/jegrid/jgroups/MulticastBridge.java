package org.jegrid.jgroups;

import org.apache.log4j.Logger;
import org.jgroups.protocols.AUTOCONF;

import java.net.*;
import java.io.IOException;

/**
 * Routes multicast packets between two networks using TCP.
 * <br>User: jdavis
 * Date: Oct 16, 2006
 * Time: 11:10:55 AM
 */
public class MulticastBridge
{
    private static Logger log = Logger.getLogger(MulticastBridge.class);

    private InetSocketAddress bindAddr;
    private InetSocketAddress bridgeAddr;
    private InetSocketAddress localAddr;

    private int packetSize;

    public void run() throws IOException
    {
        packetSize = AUTOCONF.senseMaxFragSizeStatic();

        Outgoing out = new Outgoing();
        out.initialize();
        Incoming in = new Incoming();
        in.initialize();

        Thread outThread = new Thread(out, "bridge-out");
        Thread inThread = new Thread(in, "bridge-in");

        // Start outgoing thread.
        outThread.start();
        // Start incoming thread.
        inThread.start();

        try
        {
            inThread.join();
            outThread.join();
        }
        catch (InterruptedException e)
        {
            log.error(e, e);
        }
    }

    class Incoming implements Runnable
    {
        private MulticastSocket receiver;
        private DatagramSocket sender;

        public void run()
        {
            DatagramPacket packet = new DatagramPacket(new byte[packetSize], packetSize);
            while (true)
            {
                try
                {
                    receiver.receive(packet);
                    log.info("IN  < sending " + packet.getLength() + " bytes to " + bridgeAddr + " ...");
                    packet.setSocketAddress(bridgeAddr);
                    sender.send(packet);
                }
                catch (IOException e)
                {
                    log.error(e);
                }
            }
        }

        private void initialize() throws IOException
        {
            log.info("Incoming.initialize() : reciever = " + bindAddr);
            receiver = new MulticastSocket(bindAddr.getPort());
            receiver.joinGroup(bindAddr.getAddress());
            sender = new DatagramSocket();
            log.info("Incoming.initialize() : sender = " + sender.getLocalSocketAddress());
        }
    }

    class Outgoing implements Runnable
    {
        private DatagramSocket receiver;
        private MulticastSocket sender;

        public void run()
        {
            // Recieve UDP packets from the other side and multicast them.
            DatagramPacket packet = new DatagramPacket(new byte[packetSize], packetSize);
            while (true)
            {
                try
                {
                    receiver.receive(packet);
                    log.info("OUT < sending " + packet.getLength() + " bytes to " + bindAddr + " ...");
                    packet.setSocketAddress(bindAddr);
                    sender.send(packet);
                }
                catch (IOException e)
                {
                    log.error(e);
                }
            }
        }

        private void initialize() throws IOException
        {
            log.info("Outgoing.initialize() : reciever = " + localAddr);
            receiver = new DatagramSocket(localAddr);
            log.info("Outgoing.initialize() : sender = " + bindAddr);
            sender = new MulticastSocket(bindAddr.getPort());
            sender.joinGroup(bindAddr.getAddress());
        }
    }

    public static void main(String[] args)
    {
        try
        {
            MulticastBridge b = new MulticastBridge();
            b.initialize(args);
            b.run();
        }
        catch (Exception e)
        {
            log.error(e, e);
        }
    }

    private void initialize(String[] args) throws UnknownHostException
    {
        String[] bridge = args[0].split("\\:");
        String bridgeHost = bridge[0];
        int bridgeport = Integer.parseInt(bridge[1]);

        String[] bind = args[1].split("\\:");
        String bindHost = bind[0];
        int bindport = Integer.parseInt(bind[1]);

        InetAddress bindNic = InetAddress.getByName(bindHost);
        InetAddress bridgeNic = InetAddress.getByName(bridgeHost);
        initialize(bindNic, bindport, bridgeNic, bridgeport);

    }

    private void initialize(InetAddress bindNic, int bindport, InetAddress bridgeNic, int bridgeport) throws UnknownHostException
    {
        bindAddr = new InetSocketAddress(
                bindNic,
                bindport
        );

        bridgeAddr = new InetSocketAddress(
                bridgeNic,
                bridgeport
        );

        localAddr = new InetSocketAddress(
                InetAddress.getLocalHost(),
                bridgeport            
        );
        log.info("bridge = " + bridgeAddr + " bind = " + bindAddr + " local = " + localAddr);
    }
}
