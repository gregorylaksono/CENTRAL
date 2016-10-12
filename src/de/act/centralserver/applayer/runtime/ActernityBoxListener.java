/**
 * 
 */
package de.act.centralserver.applayer.runtime;

import javax.servlet.ServletContextEvent;

import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StopWatch;

/**
 * @author User
 * @version 1.0 04.06.2007
 */
public class ActernityBoxListener implements javax.servlet.ServletContextListener{

	private ClassPathXmlApplicationContext appContext;

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent arg0) {
		try {
			appContext.close();
		}
		catch (final Exception e) {
			// ignore
		}   
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent arg0) {
		StopWatch st = new StopWatch("Starting BLACKBOX");
		st.start("BLACKBOX Start");
		@SuppressWarnings("unused")
		String mainBean = "MainBean";

		final String configFile = "blackbox";
		appContext = null;
		System.out.println("Starting service layer with '" + configFile + "' ...");
		try {
			appContext = new ClassPathXmlApplicationContext(configFile + ".xml");

		} 
		catch (final BeansException e) {
			System.err.println("Unable to start service layer with: " + configFile);
			e.printStackTrace();
		}
		catch (RuntimeException e) {
			e.printStackTrace();
		}

		if (appContext != null) {
			appContext.registerShutdownHook();
			System.out.println("ActernityBox Server started !");   
		}
	}

}
