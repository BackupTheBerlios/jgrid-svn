package org.jgrid.impl;

import org.jgroups.Address;
import org.jgroups.BlockEvent;
import org.jgroups.Channel;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.GetStateEvent;
import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.SetStateEvent;
import org.jgroups.SuspectEvent;
import org.jgroups.Transport;
import org.jgroups.View;
import org.jgroups.MembershipListener;
import org.jgroups.util.Util;
import org.jgroups.blocks.PullPushAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;

/**
 * A specialied version of PullPushAdapter that fixes the following bugs:
 * <ul>
 * <li>Don't issue an error-level log message if the channel was disconnected.  This just means
 * that the listener is shutting down.</li>
 * <li>Show the full exception if there is a problem notifying.</li>
 * <li>Fix concurrent access problem with the message listener list.</li>
 * </ul>
 * Starts a thread that receives messages on the transport and delegates them to the MessageListener.
 * <br>User: Joshua Davis
 * <br>Date: Oct 4, 2005 Time: 8:02:10 AM
 */
public class MessagePump extends PullPushAdapter
{
    private static Log log = LogFactory.getLog(MessagePump.class);

    public MessagePump(Transport transport, MessageListener l)
    {
        super(transport, l);
    }

    public void stop()
    {
        log.info("stopping...");
        super.stop();
        log.info("stopped.");
    }

    /**
     * Reentrant run(): message reception is serialized, then the listener is notified of the
     * message reception
     */
    public void run()
    {
        if (log.isDebugEnabled())
            log.debug("run() : ENTER");
        Object obj;
        while (receiver_thread != null && Thread.currentThread().equals(receiver_thread))
        {
            try
            {
                obj = transport.receive(0);
                if (obj == null)
                    continue;

                if (obj instanceof Message)
                {
                    handleMessage((Message) obj);
                }
                else if (obj instanceof GetStateEvent)
                {
                    handleGetState();
                }
                else if (obj instanceof SetStateEvent)
                {
                    handleSetState(obj);
                }
                else if (obj instanceof View)
                {
                    notifyViewChange((View) obj);
                }
                else if (obj instanceof SuspectEvent)
                {
                    notifySuspect((Address) ((SuspectEvent) obj).getMember());
                }
                else if (obj instanceof BlockEvent)
                {
                    notifyBlock();
                }
            }
            catch (ChannelNotConnectedException conn)
            {
                Address local_addr = ((Channel) transport).getLocalAddress();
                if (log.isWarnEnabled()) log.warn('[' + getAddressString(local_addr) +
                        "] channel not connected, exception is " + conn);
                Util.sleep(1000);   // why?
                receiver_thread = null;
                break;
            }
            catch (ChannelClosedException closed_ex)
            {
                Address local_addr = ((Channel) transport).getLocalAddress();
                if (receiver_thread == null)
                    log.info("Channel closed, thread stopped.");
                else if (log.isWarnEnabled()) log.warn('[' + getAddressString(local_addr) +
                        "] channel closed, exception is " + closed_ex);
                receiver_thread = null;
                break;
            }
            catch (Throwable e)
            {
                log.error("Unexpected exception: " + e.getMessage(),e);
            }
        }
        log.info("run() : Message pump stopped.");
    }

    private String getAddressString(Address local_addr)
    {
        return (local_addr == null ? "<null>" : local_addr.toString());
    }

    private void handleSetState(Object obj)
    {
        if (listener != null)
        {
            try
            {
                listener.setState(((SetStateEvent) obj).getArg());
            }
            catch (ClassCastException cast_ex)
            {
                if (log.isErrorEnabled()) log.error("received SetStateEvent, but argument " +
                        ((SetStateEvent) obj).getArg() + " is not serializable ! Discarding message.");
            }
        }
        else
        {
            log.warn("handleSetState() : no listener registered, state ignored");
        }
    }

    private void handleGetState()
    {
        byte[] retval = null;
        if (listener != null)
        {
            try
            {
                retval = listener.getState();
            }
            catch (Throwable t)
            {
                log.error("getState() from application failed, will return empty state", t);
            }
        }
        else
        {
            log.warn("handleGetState() : no listener registered, returning empty state");
        }

        if (transport instanceof Channel)
        {
            ((Channel) transport).returnState(retval);
        }
        else
        {
            if (log.isErrorEnabled())
                log.error("underlying transport is not a Channel, but a " +
                        transport.getClass().getName() + ": cannot return state using returnState()");
        }
    }

    public void addMembershipListener(MembershipListener l)
    {
        synchronized (this)
        {
            super.addMembershipListener(l);
        }
    }

    public void removeMembershipListener(MembershipListener l)
    {
        synchronized (this)
        {
            super.removeMembershipListener(l);
        }
    }

    protected void notifyViewChange(View v) {
        MembershipListener l;
        if(v == null) return;
        synchronized (this)
        {
            for(Iterator it=membership_listeners.iterator(); it.hasNext();) {
                l=(MembershipListener)it.next();
                try {
                    l.viewAccepted(v);
                }
                catch(Throwable ex) {
                    if(log.isErrorEnabled()) log.error("exception notifying " + l + ": " + ex,ex);
                }
            }
        }
    }

    protected void notifySuspect(Address suspected_mbr) {
        MembershipListener l;

        if(suspected_mbr == null) return;
        synchronized (this)
        {
            for(Iterator it=membership_listeners.iterator(); it.hasNext();) {
                l=(MembershipListener)it.next();
                try {
                    l.suspect(suspected_mbr);
                }
                catch(Throwable ex) {
                    if(log.isErrorEnabled()) log.error("exception notifying " + l + ": " + ex,ex);
                }
            }
        }
    }

    protected void notifyBlock() {
        MembershipListener l;

        synchronized (this)
        {
            for(Iterator it=membership_listeners.iterator(); it.hasNext();) {
                l=(MembershipListener)it.next();
                try {
                    l.block();
                }
                catch(Throwable ex) {
                    if(log.isErrorEnabled()) log.error("exception notifying " + l + ": " + ex,ex);
                }
            }
        }
    }
}
