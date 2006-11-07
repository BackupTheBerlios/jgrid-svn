package org.jegrid;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Add class level javadoc.
 * <br>User: Joshua Davis
 * Date: Nov 6, 2006
 * Time: 9:59:27 PM
 */
public class GridSingletonTest
{
    private static Logger log = Logger.getLogger(GridSingletonTest.class);

    public static class Thingie implements LifecycleAware
    {
        public void initialize()
        {
            log.info("initialize()");
        }

        public void terminate()
        {
            log.info("terminate()");
        }

        public Thingie()
        {
            log.info("<ctor>()");
        }
    }

    public static class Thingie2 implements LifecycleAware
    {
        private int prop1;
        private String prop2;

        public void initialize()
        {
            log.info("initialize()");
        }

        public void terminate()
        {
            log.info("terminate()");
        }

        public Thingie2()
        {
            log.info("<ctor>()");
        }


        public void setProp1(int prop1)
        {
            log.info("setProp1("+prop1+")");
            this.prop1 = prop1;
        }

        public void setProp2(String prop2)
        {
            log.info("setProp2("+prop2+")");
            this.prop2 = prop2;
        }
    }

    public static void main(String[] args)
    {
        try
        {
            GridConfiguration config = new GridConfiguration();
            config.setGridName(args[0]);
            config.setType(Grid.TYPE_SERVER);
            config.addGridSingletonDescriptor(
                    new GridSingletonDescriptor(
                            Thingie.class,
                            Thingie.class)
            );
            Map props = new HashMap();
            props.put("prop1","42");
            props.put("prop2","fourty two");
            
            config.addGridSingletonDescriptor(
                    new GridSingletonDescriptor(
                            Thingie2.class,
                            Thingie2.class,
                            props)
            );
            Grid grid = config.configure();
            grid.runServer();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
