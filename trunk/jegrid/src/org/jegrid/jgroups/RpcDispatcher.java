package org.jegrid.jgroups;

import org.jegrid.GridException;
import org.jegrid.NodeAddress;
import org.jegrid.impl.RpcTimeoutException;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.blocks.GridMethodCall;
import org.jgroups.blocks.GridRpcDispatcher;
import org.jgroups.blocks.MarshallingException;
import org.jgroups.blocks.MethodCall;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Sends outgoing RPCs, receives incoming RPCs with the RpcHandler.
 * <br>
 * Super class methods that create MethodCall objects are not supported because JEGrid needs to do
 * more detailed checking to make sure that messages are not dropped because we are using different
 * versions of JEGrid.
 * <br>User: Joshua Davis
 * Date: Oct 22, 2006
 * Time: 1:18:04 PM
 */
public class RpcDispatcher extends GridRpcDispatcher
{
    public RpcDispatcher(Channel channel, JGroupsListener listener, RpcHandler handler)
    {
        super(channel, listener, listener, handler);
    }

    public RspList callRemoteMethods(Vector dests, String method_name, Object[] args, Class[] types, int mode, long timeout)
    {
        throw new UnsupportedOperationException("callRemoteMethods()");
    }

    public RspList callRemoteMethods(Vector dests, String method_name, Object[] args, String[] signature, int mode, long timeout)
    {
        throw new UnsupportedOperationException("callRemoteMethods()");
    }

    public Object callRemoteMethod(Address dest, String method_name, Object[] args, Class[] types, int mode, long timeout) throws TimeoutException, SuspectedException
    {
        throw new UnsupportedOperationException("callRemoteMethod()");
    }

    public Object callRemoteMethod(Address dest, String method_name, Object[] args, String[] signature, int mode, long timeout) throws TimeoutException, SuspectedException
    {
        throw new UnsupportedOperationException("callRemoteMethod()");
    }
    
    public Object call(NodeAddress address, String methodName,
                       Object[] args,
                       Class[] types,
                       int mode,
                       long timeout) throws RpcTimeoutException
    {
        try
        {
            Address dest = toAddress(address);
            MethodCall method_call = createMethodCall(methodName, args, types);
            Object rv = callRemoteMethod(dest, method_call, mode, timeout);
            // Throw if the return value is an exeption.
            checkForException(rv);
            return rv;
        }
        catch (GridException ge)
        {
            throw ge;
        }
        catch (TimeoutException e)
        {
            // NOTE: If this call times out it may mean that the client has already
            // given the server the input.
            throw new RpcTimeoutException(e);
        }
        catch (Exception e)
        {
            throw new GridException(e);
        }
    }

    private MethodCall createMethodCall(String methodName, Object[] args, Class[] types)
    {
        // Create the JEGrid method call that has some more safety checks on the
        // server side.
        return new GridMethodCall(methodName, args, types);
    }

    void checkForException(Object o)
            throws Exception
    {
        if (o instanceof Exception)
            throw(Exception) o;
    }

    private Address toAddress(NodeAddress nodeAddress)
    {
        return ((JGroupsAddress) nodeAddress).getAddress();
    }

    public List broadcast(NodeAddress[] addresses, String methodName,
                          Object[] args, Class[] types, int mode, long timeout)
    {
        RspList responses = null;
        try
        {
            responses = doBroadcast(addresses, methodName, args, types, mode, timeout);
        }
        catch (MarshallingException e)
        {
            throw new GridException(e);
        }
        int numberOfResponses = responses.size();
        List rv = new ArrayList(numberOfResponses);
        for (int i = 0; i < numberOfResponses; i++)
        {
            Rsp rsp = (Rsp) responses.elementAt(i);
            rv.add(rsp.getValue());
        }
        return rv;
    }

    public List broadcastWithExceptionCheck(NodeAddress[] addresses, String methodName,
                                            Object[] args, Class[] types, int mode, long timeout) throws Exception
    {
        RspList responses = doBroadcast(addresses, methodName, args, types, mode, timeout);
        int numberOfResponses = responses.size();
        List rv = new ArrayList(numberOfResponses);
        for (int i = 0; i < numberOfResponses; i++)
        {
            Rsp rsp = (Rsp) responses.elementAt(i);
            Object o = rsp.getValue();
            checkForException(o);
            rv.add(o);
        }
        return rv;
    }

    private RspList doBroadcast(NodeAddress[] addresses, String methodName, Object[] args, Class[] types, int mode, long timeout) throws MarshallingException
    {
        Vector dests = null;
        if (addresses != null && addresses.length > 0)
        {
            dests = new Vector();
            for (int i = 0; i < addresses.length; i++)
                dests.add(toAddress(addresses[i]));
        }
        MethodCall method_call = createMethodCall(methodName, args, types);
        return callRemoteMethods(dests, method_call, mode, timeout);
    }

}
