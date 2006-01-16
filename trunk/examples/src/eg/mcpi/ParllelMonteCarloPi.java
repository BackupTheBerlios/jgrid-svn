package eg.mcpi;

import org.apache.log4j.Logger;
import org.jgrid.ClientSession;
import org.jgrid.GridConfiguration;
import org.jgrid.Job;

import java.util.ArrayList;
import java.util.List;


/**
 * Command line client that executes monte carlo calculation of pi in parallel.
 * <br>User: Joshua Davis
 * Date: Jan 15, 2006
 * Time: 11:34:51 AM
 */
public class ParllelMonteCarloPi {
    private static Logger log = Logger.getLogger(ParllelMonteCarloPi.class);

    public static void main(String[] args) {
        try {
            // Connect to the grid as a client.
            GridConfiguration config = new GridConfiguration();
            ClientSession clientSession = config.getClientSession();

            // Define the service.
            Job job = clientSession.createJob(MonteCarloPiService.class);

            // Serial execution first, time it.
            int iterations = 100000;
            int blocks = 1000;
            log.info("Starting serial execution...");
            job.start(new Input(0,iterations * blocks));
            Result result = (Result) job.join(-1);
            log.info("approximate pi = " + result.getApproximation());

            // Issue the parallel part of the job.
            List inputList = new ArrayList();
            long seed = 1;
            for (int i = 0; i < blocks ; i++)
                inputList.add(new Input(seed++,iterations));
            job.startParallel(inputList);
        } catch (Throwable t) {
            log.error(t);
        }
    }
}
