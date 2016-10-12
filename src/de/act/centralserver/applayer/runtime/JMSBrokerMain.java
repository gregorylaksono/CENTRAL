/**
 * @author Martin Sachs & Henry
 * @since 1.0
 */
package de.act.centralserver.applayer.runtime;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.jdbc.JDBCPersistenceAdapter;
import org.apache.activemq.store.jdbc.adapter.PostgresqlJDBCAdapter;
import org.apache.commons.dbcp.BasicDataSource;
import de.act.blackbox.utils.Utils;

public class JMSBrokerMain {
	/**
	 * @author Martin Sachs & Henry
	 * @since 1.0
	 * @param args
	 */
	public static void main(final String[] args) {

		final BrokerService broker = new BrokerService();
		final String uri = Utils.getURI();
		try {
			BasicDataSource postgreDataSource = new BasicDataSource();
			postgreDataSource.setDriverClassName("org.postgresql.Driver");
			postgreDataSource.setUrl("jdbc:postgresql://actDatabase:5432/activemq");
			postgreDataSource.setUsername("batch");
			postgreDataSource.setPassword("batch");
			postgreDataSource.setInitialSize(10);
			postgreDataSource.setMaxActive(200);
			postgreDataSource.setPoolPreparedStatements(true);
			JDBCPersistenceAdapter jdbcPersistenceAdapter = new JDBCPersistenceAdapter();
			jdbcPersistenceAdapter.setAdapter(new PostgresqlJDBCAdapter());
			jdbcPersistenceAdapter.setDataSource(postgreDataSource);
			broker.setPersistenceAdapter(jdbcPersistenceAdapter);
			broker.deleteAllMessages();
			broker.setDeleteAllMessagesOnStartup(false);
			broker.addConnector(uri);
			broker.setUseJmx(false);
			broker.setPopulateJMSXUserID(true);
			broker.setBrokerName("Localhost");
			broker.setUseShutdownHook(true);
			broker.setPersistent(true);
			broker.start();
		} 
		catch (final Exception e) {
			e.printStackTrace();
		}

		System.out.println("JMS Broker started on " + uri);

		final Object t = new Object();
		synchronized (t) {
			try {
				t.wait();
			} 
			catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}