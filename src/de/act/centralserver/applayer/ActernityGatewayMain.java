package de.act.centralserver.applayer;

import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Martin Sachs & Henry
 * @since 1.0 - 02.11.2006
 * @update 02.11.2009
 */
public class ActernityGatewayMain {

	public static void main(String[] args) {
		System.out.println("Central Server Context starting...");

		String mainBean = "CentralBean";
		if (args.length < 1 || args.length > 2) {
			System.err.println("Usage: java " + ActernityGatewayMain.class.getName() + " <config file> [<MainBean>]");
			System.exit(1);
		}
		else if (args.length > 1) {
			mainBean = args[1];
		}
		final String configFile = args[0];
		ClassPathXmlApplicationContext appContext = null;
		System.out.println("Starting central service layer with '" + configFile + "' ...");
		try {
			appContext = new ClassPathXmlApplicationContext(configFile + ".xml");
		} catch (final BeansException e) {
			System.err.println("Unable to start central service layer with: " + configFile);
			e.printStackTrace();
		}
		if (appContext != null) {
			appContext.registerShutdownHook();
			try {
				System.out.println("Running service: " + mainBean);
				final Runnable app = (Runnable) appContext.getBean(mainBean);
				app.run();
			} 
			catch (final Exception e) {
				e.printStackTrace();
			}
			try {
				appContext.close();
			} 
			catch (final Exception e) {
				// ignore
			}
		}
	}
}
