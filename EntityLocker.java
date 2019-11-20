package entityLocker;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EntityLocker<T> implements EntityLockerInterface<T>{

	private final static int locksEscalationLimit = 12;
	private ConcurrentMap<T, ReentrantLock> locksMap = new ConcurrentHashMap<T, ReentrantLock>();
	private static volatile ReentrantLock globalLock = new ReentrantLock();
	Condition globalLockCondition = globalLock.newCondition();
	private static ConcurrentMap<Long, Integer> locksByThread = new ConcurrentHashMap<Long, Integer>();
	private boolean useEscalation = false;


	/**
     * Attempts to acquire lock with timeout.
     * 
     * Check the lock escalation condition 
     * and call accordingly the result global or not global lock
     *
     * @param entityId
     * @param timeOut
     * @param unit
     * @return boolean if lock was successful
     * @throws InterruptedException
     */
	@Override
	public boolean trylockEntityById(T entityId, Long timeOut, TimeUnit unit) throws Exception {
		if(entityId == null || timeOut == 0 || unit == null ) throw new IllegalArgumentException();
		
		// Check and escalate the lock to global if needed
		if (useEscalation && isEscalateLockToGlobal()) {
			System.out.println("Escalate lock to global for thread " + Thread.currentThread().getName() + " for id " + entityId);
			return tryLockGlobaly(timeOut, unit);
		}
		
	
		// not global lock case
		final Lock lock = getLockByEntityId(entityId);
	    if (lock == null) {
	    	throw new IllegalArgumentException("Lock for the Entity id:" + entityId + " is not found");
	    }
		
    	while(globalLock.isLocked()) {
			try {
			    System.out.println("Thread " + Thread.currentThread().getName() + " is waiting for global lock release");					
				globalLockCondition.await(timeOut, unit);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}	
   
	    System.out.println("Locking thread " + Thread.currentThread().getName() + " for id " + entityId);	    
        return lock.tryLock(timeOut, unit);
	}


	/**
	 * 
	 * Checks escalation condition and Increments locks counter for thread attempts to get new lock
	 * 	
	 * @returns true if escalation condition is reached, otherwise - false
	 */
	public boolean isEscalateLockToGlobal() {
		Long threadId = Thread.currentThread().getId();

		Integer locksNbForCurrentThread = locksByThread.getOrDefault(threadId, 1);
		locksByThread.put(threadId, ++locksNbForCurrentThread);
		
		return (locksNbForCurrentThread > locksEscalationLimit);
	}


	/**
     * Unlocks the entity
     *
     * @param id
     * @throws IllegalMonitorStateException if the lock was not found for given entity
     * @throws IllegalArgumentException if entity id is null
     */
	@Override
	public void unlockEntityById(T entityId) {
		
        if (entityId == null) {
            throw new IllegalArgumentException("Entity id cannot be null");
        }

        if (globalLock.isLocked() && globalLock.isHeldByCurrentThread()) {
        	this.unlockGlobalLock();
        }
        
        ReentrantLock lock = locksMap.get(entityId);
        if (lock == null) {
            throw new IllegalMonitorStateException("Lock for the Entity id: " + entityId.toString() + " is not found");
        }

        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
        System.out.println("Unlocking thread " + Thread.currentThread().getName() + " for id " + entityId + "\n");
	}
	
	
	/**
	 * Returns lock by entity id from map if it's exists, 
	 * if not, creates new log and put it to the map
	 * 
	 * @param entityId
	 * @return
	 */
	private Lock getLockByEntityId(T entityId) {
        return Optional.ofNullable(entityId)
            .map(id -> locksMap.computeIfAbsent(id, lock -> new ReentrantLock()))
            .orElseThrow(() -> new IllegalArgumentException("Entity id cannot be null"));
    }

	
	/**
     * Attempts to acquire global lock with timeout.
     *
     * @param timeOut
     * @param unit
     * @return boolean if lock was successful
     * @throws InterruptedException
     */
	public boolean tryLockGlobaly(Long timeOut, TimeUnit unit) throws InterruptedException {
		if(timeOut == 0 || unit == null ) throw new IllegalArgumentException();
		
		System.out.println("Thread " + Thread.currentThread().getName() + " acquire a global lock");
		return globalLock.tryLock(timeOut, unit);
	}


	/**
	 * Release the global lock if the current thread is the holder of this lock 
	 * If the current thread is not the holder of this lock then IllegalMonitorStateException is thrown.
	 */
	public void unlockGlobalLock() {
		if(globalLock.isLocked() && globalLock.isHeldByCurrentThread()) {
			globalLock.unlock();
			System.out.println("Thread " + Thread.currentThread().getName()+ " release global lock. Current HoldCount:" + globalLock.getHoldCount() + "\n" + "\n");
			//globalLockCondition.signalAll();
		} else {
			throw new IllegalMonitorStateException("Global Lock is not found for thread " + Thread.currentThread().getName());
		}
	}
}
