package de.act.centralserver.applayer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RmiMainBean implements Runnable {
	private final static Log LOG = LogFactory.getLog(RmiMainBean.class.getName());

	private RmiMainBean() {}

	public void run() {
		try {
			System.out.println("The main bean is started.");
			final Object lock = new Object();
			synchronized (lock) {
				lock.wait();
			}
		} 
		catch (final Exception e) {
			RmiMainBean.LOG.error("Unexpected error in main bean:", e);
		}
	}
}
