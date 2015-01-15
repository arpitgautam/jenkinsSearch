package hobby.arpitgautam.buildmachinesearch;

public abstract class QueueProcessor extends Thread {

	public abstract void killMe();
	public abstract void operation();
	
	@Override
	public void run() {
		operation();
	}
	
	protected void sleep(int n) {
		try {
			Thread.sleep(n);
		} catch (InterruptedException e) {

		}
	}

}
