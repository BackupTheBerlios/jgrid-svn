package org.jgroups.blocks;

import org.jgroups.*;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * This has been patched for JEGrid to have more robust error handling.
 * <br>
 * This class allows a programmer to invoke remote methods in all (or single)
 * group members and optionally wait for the return value(s).
 * An application will typically create a channel and layer the
 * GridRpcDispatcher building block on top of it, which allows it to
 * dispatch remote methods (client role) and at the same time be
 * called by other members (server role).
 * This class is derived from MessageDispatcher.
 * Is the equivalent of RpcProtocol on the application rather than protocol level.
 *
 * @author Bela Ban
 * @author Joshua Davis (patching for better error handling, removed code that isn't used by JEGrid)
 */
public class GridRpcDispatcher extends MessageDispatcher implements ChannelListener
{
    private Object serverObject = null;
    private Marshaller marshaller = null;
    private List additionalChannelListeners = null;
    private MethodLookup methodLookup = null;


    public GridRpcDispatcher(Channel channel, MessageListener messageListener,
                             MembershipListener membershipListener, Object server_obj)
    {
        super(channel, messageListener, membershipListener);
        channel.addChannelListener(this);
        this.serverObject = server_obj;
        additionalChannelListeners = new ArrayList();
    }

    public interface Marshaller
    {
        byte[] objectToByteBuffer(Object obj) throws Exception;

        Object objectFromByteBuffer(byte[] buf) throws Exception;
    }

    public String getName()
    {
        return "GridRpcDispatcher";
    }

    public void setMarshaller(Marshaller m)
    {
        this.marshaller = m;
    }

    public Marshaller getMarshaller()
    {
        return marshaller;
    }

    public Object getServerObject()
    {
        return serverObject;
    }

    public MethodLookup getMethodLookup()
    {
        return methodLookup;
    }

    public void setMethodLookup(MethodLookup method_lookup)
    {
        this.methodLookup = method_lookup;
    }


    public RspList castMessage(Vector dests, Message msg, int mode, long timeout)
    {
        if (log.isErrorEnabled()) log.error("this method should not be used with " +
                "GridRpcDispatcher, but MessageDispatcher. Returning null");
        return null;
    }

    public Object sendMessage(Message msg, int mode, long timeout) throws TimeoutException, SuspectedException
    {
        if (log.isErrorEnabled()) log.error("this method should not be used with " +
                "GridRpcDispatcher, but MessageDispatcher. Returning null");
        return null;
    }


    public RspList callRemoteMethods(Vector dests, String method_name, Object[] args,
                                     Class[] types, int mode, long timeout) throws MarshallingException
    {
        MethodCall method_call = new MethodCall(method_name, args, types);
        return callRemoteMethods(dests, method_call, mode, timeout);
    }

    public RspList callRemoteMethods(Vector dests, String method_name, Object[] args,
                                     String[] signature, int mode, long timeout) throws MarshallingException
    {
        MethodCall method_call = new MethodCall(method_name, args, signature);
        return callRemoteMethods(dests, method_call, mode, timeout);
    }


    public RspList callRemoteMethods(Vector dests, MethodCall method_call, int mode, long timeout) throws MarshallingException
    {
        if (dests != null && dests.size() == 0)
        {
            // don't send if dest list is empty
/*
            if (log.isTraceEnabled())
                log.trace(new StringBuffer("destination list of ").append(method_call.getName()).
                        append("() is empty: no need to send message"));
*/
            return new RspList();
        }

/*
        if (log.isTraceEnabled())
            log.trace(new StringBuffer("dests=").append(dests).append(", method_call=").append(method_call).
                    append(", mode=").append(mode).append(", timeout=").append(timeout));
*/

        byte[] buf;
        try
        {
            buf = marshaller != null ? marshaller.objectToByteBuffer(method_call) : Util.objectToByteBuffer(method_call);
        }
        catch (Exception e)
        {
            // The old code used to just return null.   It should really throw.
            log.error(e,e);
            throw new MarshallingException(e);
        }

        Message msg = new Message(null, null, buf);
        RspList retval = super.castMessage(dests, msg, mode, timeout);
//        if (log.isTraceEnabled()) log.trace("responses: " + retval);
        return retval;
    }


    public Object callRemoteMethod(Address dest, String method_name, Object[] args,
                                   Class[] types, int mode, long timeout)
            throws TimeoutException, SuspectedException, MarshallingException
    {
        MethodCall method_call = new MethodCall(method_name, args, types);
        return callRemoteMethod(dest, method_call, mode, timeout);
    }

    public Object callRemoteMethod(Address dest, String method_name, Object[] args,
                                   String[] signature, int mode, long timeout)
            throws TimeoutException, SuspectedException, MarshallingException
    {
        MethodCall method_call = new MethodCall(method_name, args, signature);
        return callRemoteMethod(dest, method_call, mode, timeout);
    }

    public Object callRemoteMethod(Address dest, MethodCall method_call, int mode, long timeout)
            throws TimeoutException, SuspectedException, MarshallingException
    {
        byte[] buf = null;
        Message msg = null;
        Object retval = null;

/*
        if (log.isTraceEnabled())
            log.trace("dest=" + dest + ", method_call=" + method_call + ", mode=" + mode + ", timeout=" + timeout);
*/

        try
        {
            buf = marshaller != null ? marshaller.objectToByteBuffer(method_call) : Util.objectToByteBuffer(method_call);
        }
        catch (Exception e)
        {
            log.error(e,e);
            throw new MarshallingException(e);
        }

        msg = new Message(dest, null, buf);
        retval = super.sendMessage(msg, mode, timeout);
//        if (log.isTraceEnabled()) log.trace("retval: " + retval);
        return retval;
    }

    /**
     * Message contains MethodCall. Execute it against *this* object and return result.
     * Use MethodCall.invoke() to do this. Return result.
     */
    public Object handle(Message req)
    {
        Object body = null;
        MethodCall method_call;

        if (serverObject == null)
        {
            if (log.isErrorEnabled()) log.error("no method handler is registered. Discarding request.");
            return null;
        }

        if (req == null || req.getLength() == 0)
        {
            if (log.isErrorEnabled()) log.error("message or message buffer is null");
            return null;
        }

        try
        {
            body = marshaller != null ? marshaller.objectFromByteBuffer(req.getBuffer()) : req.getObject();
        }
        catch (Throwable e)
        {
            if (log.isErrorEnabled()) log.error("exception=" + e);
            return e;
        }

        if (body == null || !(body instanceof MethodCall))
        {
            if (log.isErrorEnabled()) log.error("message does not contain a MethodCall object");
            return null;
        }

        method_call = (MethodCall) body;

        try
        {
/*
            if (log.isTraceEnabled())
                log.trace("[sender=" + req.getSrc() + "], method_call: " + method_call);
*/

            if (method_call.getMode() == MethodCall.ID)
            {
                if (methodLookup == null)
                    throw new Exception("MethodCall uses ID=" + method_call.getId() + ", but methodLookup has not been set");
                Method m = methodLookup.findMethod(method_call.getId());
                if (m == null)
                    throw new Exception("no method foudn for " + method_call.getId());
                method_call.setMethod(m);
            }

            return method_call.invoke(serverObject);
        }
        catch (Throwable x)
        {
            log.error("failed invoking method", x);
            return x;
        }
    }

    /**
     * Add a new channel listener to be notified on the channel's state change.
     *
     * @return true if the listener was added or false if the listener was already in the list.
     */
    public boolean addChannelListener(ChannelListener l)
    {

        synchronized (additionalChannelListeners)
        {
            if (additionalChannelListeners.contains(l))
            {
                return false;
            }
            additionalChannelListeners.add(l);
            return true;
        }
    }

    /**
     * @return true if the channel was removed indeed.
     */
    public boolean removeChannelListener(ChannelListener l)
    {

        synchronized (additionalChannelListeners)
        {
            return additionalChannelListeners.remove(l);
        }
    }

    /* --------------------- Interface ChannelListener ---------------------- */

    public void channelConnected(Channel channel)
    {

        synchronized (additionalChannelListeners)
        {
            for (Iterator i = additionalChannelListeners.iterator(); i.hasNext();)
            {
                ChannelListener l = (ChannelListener) i.next();
                try
                {
                    l.channelConnected(channel);
                }
                catch (Throwable t)
                {
                    log.warn("channel listener failed", t);
                }
            }
        }
    }

    public void channelDisconnected(Channel channel)
    {

        stop();

        synchronized (additionalChannelListeners)
        {
            for (Iterator i = additionalChannelListeners.iterator(); i.hasNext();)
            {
                ChannelListener l = (ChannelListener) i.next();
                try
                {
                    l.channelDisconnected(channel);
                }
                catch (Throwable t)
                {
                    log.warn("channel listener failed", t);
                }
            }
        }
    }

    public void channelClosed(Channel channel)
    {

        stop();

        synchronized (additionalChannelListeners)
        {
            for (Iterator i = additionalChannelListeners.iterator(); i.hasNext();)
            {
                ChannelListener l = (ChannelListener) i.next();
                try
                {
                    l.channelClosed(channel);
                }
                catch (Throwable t)
                {
                    log.warn("channel listener failed", t);
                }
            }
        }
    }

    public void channelShunned()
    {

        synchronized (additionalChannelListeners)
        {
            for (Iterator i = additionalChannelListeners.iterator(); i.hasNext();)
            {
                ChannelListener l = (ChannelListener) i.next();
                try
                {
                    l.channelShunned();
                }
                catch (Throwable t)
                {
                    log.warn("channel listener failed", t);
                }
            }
        }
    }

    public void channelReconnected(Address new_addr)
    {
        if (log.isTraceEnabled())
            log.trace("channel has been rejoined, old local_addr=" + local_addr + ", new local_addr=" + new_addr);
        this.local_addr = new_addr;
        start();

        synchronized (additionalChannelListeners)
        {
            for (Iterator i = additionalChannelListeners.iterator(); i.hasNext();)
            {
                ChannelListener l = (ChannelListener) i.next();
                try
                {
                    l.channelReconnected(new_addr);
                }
                catch (Throwable t)
                {
                    log.warn("channel listener failed", t);
                }
            }
        }
    }
    /* ----------------------------------------------------------------------- */

}
