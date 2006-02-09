package org.jgrid.impl;

import org.jgroups.MessageListener;
import org.jgroups.Message;
import org.jgroups.TimeoutException;
import org.jgroups.util.Promise;
import org.jgrid.util.SerializationUtil;
import org.jgrid.GridException;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Manages the grid state.
 *
 * <br>User: Joshua Davis
 * <br>Date: Oct 24, 2005 Time: 6:54:00 AM
 */
class GridListener extends GridComponent implements MessageListener
{
    private static Logger log = Logger.getLogger(GridListener.class);

    private Promise gridStatePromise;
    private GridStateImpl gridState;
    private static final long GRID_STATE_TIMEOUT = 10000;

    public GridListener(GridBusImpl gridBus)
    {
        super(gridBus);
        gridStatePromise = new Promise();
    }

    public void receive(Message msg)
    {
        log.info(getLocalAddress() + " *** receive() " + msg);
    }

    public byte[] getState()
    {
        try
        {
            GridStateImpl gridState = getGridState();
            if (log.isDebugEnabled())
                log.debug("getState() : returning " + gridState);
            return SerializationUtil.objectToByteArray(gridState);
        }
        catch (Exception e)
        {
            log.error(e, e);
            throw new GridException(e);
        }
    }

    public void setState(byte[] stateBytes)
    {
        try
        {
            if (stateBytes != null && stateBytes.length > 0)
            {
                GridStateImpl newState = null;
                try
                {
                    newState = (GridStateImpl) SerializationUtil.byteArrayToObject(stateBytes);
                }
                catch (IOException e)
                {
                    // Satisfy the promise, but pass the exception as the result!
                    gridStatePromise.setResult(e);
                }
                if (log.isDebugEnabled())
                    log.debug("setState() : new state is " + newState);
                gridStatePromise.setResult(newState);
            }
        }
        catch (Exception e)
        {
            log.error(e, e);
            throw new GridException(e);
        }
    }

    private void setGridState(GridStateImpl newGridState)
    {
        synchronized (this)
        {
            gridState = newGridState;
        }
    }

    public GridStateImpl getGridState()
    {
        synchronized (this)
        {
            return gridState;
        }
    }

    public void waitForGridState()
    {
        try
        {
            Object o = gridStatePromise.getResultWithTimeout(GRID_STATE_TIMEOUT);
            if (o instanceof Throwable)
            {
                Throwable throwable = (Throwable) o;
                if (throwable instanceof GridException)
                {
                    throw (GridException) throwable;
                }
                else
                {
                    throw new GridException("Grid state unavailable due to: " + throwable.getMessage(), throwable);
                }
            }
            if (o instanceof GridException)
            {
                throw (GridException) o;
            }
            setGridState((GridStateImpl) o);
        }
        catch (TimeoutException e)
        {
            throw new GridException("Timed out waiting for grid state!", e);
        }
    }

    public void setCoordinator(NodeStateImpl myState)
    {
        GridStateImpl newGridState = new GridStateImpl();
        newGridState.handleUpdate(myState);
        setGridState(newGridState);
    }

    public Object handleNodeUpdate(NodeStateImpl state)
    {
        boolean updateApplied = false;
        synchronized (this)
        {
            // No state yet, so there is nothing to apply the message to.
            if (gridState == null)
            {
                log.info(" *** Grid state not available, ignoring " + state);
                return "NACK: grid state is not available";
            }
            if (state != null)
            {
                gridState.handleUpdate(state);
                updateApplied = true;
            }
            else
                throw new GridException("NodeState was null!");
        }
        // We don't need a lock on the state to do this, so do it after the synchronized block.
        if (updateApplied)
        {
            gridBus.getNotifier().notifyPeersUpdated();
            return MessageConstants.ACK;
        }
        else
        {
            return "NACK: did not apply update.";
        }
    }
}
