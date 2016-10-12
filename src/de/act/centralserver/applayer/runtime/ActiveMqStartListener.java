/**
 * 
 */
package de.act.centralserver.applayer.runtime;

import java.io.File;
import javax.servlet.ServletContextEvent;
import org.apache.activemq.broker.BrokerService;
import de.act.blackbox.utils.Utils;

/**
 * @author User
 * @version 1.0 04.06.2007
 */
public class ActiveMqStartListener implements javax.servlet.ServletContextListener{
	private BrokerService broker ;
	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent arg0) {
		try{
			broker.stop();
			broker = null;
		}
		catch(Exception e){
			System.err.println(e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent arg0) {
		broker = new BrokerService();
		broker.setUseJmx(true);
		final String uri = Utils.getURI();
		System.out.println("DataPath:" +broker.getDataDirectory().getAbsolutePath());
		File da = new File("../webapps/ActernityBox/localhost");
		da.setWritable(true);
		broker.setDataDirectory(da);
		System.out.println("DataPath:" +broker.getDataDirectory().getAbsolutePath());
		try {
			broker.addConnector(uri);
			broker.setDeleteAllMessagesOnStartup(true);
			broker.setUseJmx(true);
			broker.setPopulateJMSXUserID(true);
			broker.setBrokerName("Localhost");

			broker.setUseShutdownHook(true);

		} 
		catch (final Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		try {
			broker.setPopulateJMSXUserID(true);
			broker.start();
			System.out.println("JMS Broker startet on " + uri);
		} 
		catch (final Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
