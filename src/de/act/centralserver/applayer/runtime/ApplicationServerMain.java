package de.act.centralserver.applayer.runtime;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StopWatch;

public class ApplicationServerMain {
	public static void main(final String[] args) throws Exception {

		StopWatch st = new StopWatch("Starting CENTRAL");
		st.start("CENTRAL Start");
		String mainBean = "MainBean";
		if (args.length < 1 || args.length > 2) {
			System.err.println("Usage: java " + ApplicationServerMain.class.getName() + " <config file> [<MainBean>]");
			System.exit(1);
		} 
		else if (args.length > 1) {
			mainBean = args[1];
		}
		final String configFile = args[0];
		ClassPathXmlApplicationContext appContext = null;
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
			try {
				System.out.println("Running service: " + mainBean);
				final Runnable app = (Runnable) appContext.getBean(mainBean);
				st.stop();
				System.out.println(st.shortSummary());
				System.out.println("Application Run!");
				app.run();
			} catch (final Exception e) {
				e.printStackTrace();
			}
			try {
				appContext.close();
			} 
			catch (final Exception e) {

			}
		}
	}
}
