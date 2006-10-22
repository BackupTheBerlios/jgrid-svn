package org.jgroups.blocks;

import java.io.Externalizable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Patches up the JGroups building block for MethodCall so that it
 * throws an exception if the target doesn't acutally have the method.
 * <br>User: Joshua Davis
 * Date: Oct 22, 2006
 * Time: 1:52:27 PM
 */
public class GridMethodCall extends MethodCall implements Externalizable
{
    private static final long serialVersionUID = -1373091004255907094L;

    public GridMethodCall()
    {
        super();
    }

    public GridMethodCall(String methodName, Object[] args, Class[] types)
    {
        super(methodName, args, types);
    }


    public Object invoke(Object target) throws Throwable
    {
        Class cl;
        Method meth = null;
        Object retval = null;

        if (method_name == null || target == null)
        {
            throw new NoSuchMethodException("No method name or no target.");
        }
        cl = target.getClass();
        try
        {
            switch (mode)
            {
                case OLD:
                    meth = findMethod(cl);
                    break;
                case METHOD:
                    if (this.method != null)
                        meth = this.method;
                    break;
                case TYPES:
                    meth = getMethod(cl, method_name, types);
                    break;
                case SIGNATURE:
                    Class[] mytypes = null;
                    if (signature != null)
                        mytypes = getTypesFromString(cl, signature);
                    meth = getMethod(cl, method_name, mytypes);
                    break;
                case ID:
                    break;
                default:
                    if (log.isErrorEnabled()) log.error("mode " + mode + " is invalid");
                    break;
            }

            if (meth != null)
            {
                retval = meth.invoke(target, args);
            }
            else
            {
                throw new NoSuchMethodException("method " + method_name + " not found");
            }
            return retval;
        }
        catch (InvocationTargetException inv_ex)
        {
            throw inv_ex.getTargetException();
        }
        catch (NoSuchMethodException no)
        {
            StringBuffer sb = new StringBuffer();
            sb.append("found no method called ").append(method_name).append(" in class ");
            sb.append(cl.getName()).append(" with (");
            if (args != null)
            {
                for (int i = 0; i < args.length; i++)
                {
                    if (i > 0)
                        sb.append(", ");
                    sb.append((args[i] != null) ? args[i].getClass().getName() : "null");
                }
            }
            sb.append(") formal parameters");
            log.error(sb.toString());
            throw no;
        }
        catch (Throwable e)
        {
            if (log.isErrorEnabled()) log.error("exception in invoke()", e);
            throw e;
        }
    }
}
