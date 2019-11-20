package entityLocker;

import java.util.concurrent.TimeUnit;

public interface EntityLockerInterface<T> {

	boolean trylockEntityById(T entityId, Long timeOutInMS, TimeUnit unit) throws InterruptedException, Exception;
	
	void unlockEntityById(T entityId);

}
