package org.jegrid;

import org.apache.log4j.Logger;

import java.io.Serializable;

/**
 * Exception throwing task.
 * <br>User: jdavis
 * Date: Nov 14, 2006
 * Time: 2:04:16 PM
 */
public class ExceptionThrower
{
    private static Logger log = Logger.getLogger(ExceptionThrower.class);

    public static class Processor implements InputProcessor
    {
        public Serializable processInput(int inputId, Serializable input)
        {
            if (input instanceof RuntimeException)
            {
                RuntimeException runtimeException = (RuntimeException) input;
                log.info("Throwing...");
                throw runtimeException;
            }
            else
            {
                log.info("Continuing...");
                return null;
            }
        }
    }
}
