package org.jgrid.impl;

import java.util.Map;
import java.util.HashMap;

/**
 * A map of request states.
 * <br>User: Joshua Davis
 * Date: Mar 4, 2006
 * Time: 9:52:42 AM
 */
public class RequestStateMap
{
    private final Map requestStateById = new HashMap();

    public RequestState remove(String requestId)
    {
        synchronized (requestStateById)
        {
            return (RequestState) requestStateById.remove(requestId);
        }
    }

    public boolean contains(String requestId)
    {
        synchronized (requestStateById)
        {
            return requestStateById.containsKey(requestId);
        }
    }

    public void put(RequestState request)
    {
        synchronized (requestStateById)
        {
            requestStateById.put(request.getRequestId(),request);
        }
    }

}
