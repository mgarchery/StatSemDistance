package statsemdistance;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 *  This class provides multithreading support for our application.
 *  Contains a queue for tasks and an executor.
 * @author mgarchery
 */
public class Multithreading {
    
    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 4;
    private static final long KEEP_ALIVE_TIME = 0;
    private static BlockingQueue<Runnable> blockingQueue;
    private static ImageSignatureThreadPoolExecutor executor;
    
    /**
     * Initializes the tasks queue with the given size and prepares the executor for running.
     * Call this with the appropriate number of tasks before adding them to the executor. 
     * @param tasks the maximum number of tasks that will be run during this execution 
     * @return the initialized executor
     */
    public static ImageSignatureThreadPoolExecutor initializeQueueAndGetExecutor(int tasks){
        blockingQueue = new ArrayBlockingQueue<>(tasks);
        executor = new ImageSignatureThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.NANOSECONDS, blockingQueue);
        executor.prestartAllCoreThreads();
        return executor;
    }
    
    /**
     * Waits for the tasks currently in queue to be terminated and then returns. 
     */
    public static void waitForExecutionEnd(){
        executor.shutdown();
        while (!executor.isTerminated()); //wait for all the tasks to be executed
    }
    
   
    
}
