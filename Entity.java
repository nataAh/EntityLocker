package entityLocker;
import java.util.concurrent.TimeUnit;


public class Entity {
	int id;
	String value;
	EntityLocker<Integer> locker;
	long timeOut = 1; // SECONDS
	
	Entity(int id, EntityLocker<Integer> locker){
		this.id = id;
		this.value = "";
		this.locker = locker;
	}

	public Integer getId() {
		return id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public void check() {
		System.out.println("------> from check for thread " + Thread.currentThread().getName() + " for id " + id);
		this.setValue(this.getValue() + " + check");
		try {
			TimeUnit.SECONDS.sleep(timeOut);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	} 

	public void setValue() {
		System.out.println("------> Calling target for thread " + Thread.currentThread().getName() + " for id " + id);
		this.value += "id = " + id +" tread: "+ Thread.currentThread().getName();
		try {
			TimeUnit.SECONDS.sleep(timeOut);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
