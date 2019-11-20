package entityLocker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EntityLockerTest {
	static EntityLocker<Integer> locker = new EntityLocker<>();
	static ExecutorService executorService = Executors.newWorkStealingPool();

	final static long GLOBAL_TIMEOUT = 100L;
	final static long TIMEOUT = 500L;
	private final static TimeUnit unit = TimeUnit.MILLISECONDS;

	
	public static void main(String[] args) {
	
		System.out.println("Start test!");

		try {			
			//concurrent request to lock the different entities
//			System.out.println("Test global and concurent locks");
//			testGlobalLock(null); //OK
			
			//concurrent request to lock the same entity
//			System.out.println("Test global and concurrent request to lock the same entity");
//			testGlobalLock(1); //OK

			System.out.println("Test global lock Escalation");
			testGlobalLockEscalation();

			
		} catch (Exception e1) {
            e1.printStackTrace();
		} finally {
			shutdownAndAwaitTermination(executorService);
		}
	}


	private static void testGlobalLockEscalation() {
		
		Callable<Void>callableTask = () -> {
			for (int i = 0; i<5; i++) {
				Entity entity = new Entity(i, locker);
				if (locker.trylockEntityById(entity.getId(), TIMEOUT, unit)) { 
					try {
						entity.setValue();
						entity.check();
					} finally {
						locker.unlockEntityById(entity.getId());
					}			
			    }			
			}
		return null;
		};

		executorService.submit(callableTask);
	}


	/**
	 * Tests cases:
	 * 
	 * global lock:
	 * Expected result: One thread require a global lock 
	 * and release global lock after execution of protected code
	 * Other threads are waiting for the global lock release and execute “protected code” 
	 * 
	 * entityId is null:
	 * concurrent execution of protected code on different entities
	 * 
	 * entityId is a constant value of Integer:
	 * Reproduces a concurrent request to lock the same entity by more then one thread.
	 * Expected result: one thread executes protected code on that entity
	 * Other threads wait until the entity becomes available.
	 * 
	 * @param entityId
	 * @throws InterruptedException
	 */
	private static void testGlobalLock(Integer entityId) throws InterruptedException {
		
		List<Callable<Void>> callables = new ArrayList();
		
		// task acquire global lock
		Callable<Void> callableTask = () -> {
			Entity entity = new Entity(0, locker);
			if (locker.tryLockGlobaly(GLOBAL_TIMEOUT, unit)) {
				try {
					entity.setValue();
					entity.check();
				} finally {
					locker.unlockGlobalLock();
				}
			}			
			return null;
		};
		callables.add(callableTask);

		
		// tasks are waiting for global lock release
		for (int i = 0; i<5; i++) {
			Entity entity = new Entity(entityId != null ? entityId : i, locker);
			callableTask = () -> {
			    if (locker.trylockEntityById(entity.getId(), TIMEOUT, unit)) {
					try {
						entity.setValue();
						entity.check();
					} finally {
						locker.unlockEntityById(entity.getId());
					}			
			    }
				return null;
			};
	
			callables.add(callableTask);
		}
		executorService.invokeAll(callables);
	}

	
	static void shutdownAndAwaitTermination(ExecutorService pool) {
		   pool.shutdown(); // Disable new tasks from being submitted
		   try {
		     // Wait a while for existing tasks to terminate
		     if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
		       pool.shutdownNow(); // Cancel currently executing tasks
		       // Wait a while for tasks to respond to being cancelled
		       if (!pool.awaitTermination(60, TimeUnit.SECONDS))
		           System.err.println("Pool did not terminate");
		     }
		   } catch (InterruptedException ie) {
		     // (Re-)Cancel if current thread also interrupted
		     pool.shutdownNow();
		     // Preserve interrupt status
		     Thread.currentThread().interrupt();
		   }
		 }

}
