package de.act.centralserver.applayer.gateway;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.transport.TransportListener;
import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import de.act.centralserver.dblayer.core.CDBSessionManager;
import de.act.common.jmstypes.SerializableObject;

/**
 * @author Martin Sachs & Henry
 * @since 1.0 - 03.11.2006
 */
public class JMSServerTransportListener implements TransportListener {

	private static final Logger log = Logger.getLogger(JMSServerTransportListener.class);
	private String table;
	private Connection connection;
	private CDBSessionManager dbSessionManager;

	public JMSServerTransportListener(Connection serverConnection) {
		this.connection = serverConnection;
	}
	
	public JMSServerTransportListener(Connection serverConnection, CDBSessionManager dbSessionManager, String table) {
		this.connection = serverConnection;
		this.dbSessionManager = dbSessionManager;
		this.table = table;
	}

	public void onException(IOException error) {
		log.error("JMSServer Exception",error);
	}

	public void transportInterupted() {
		if (this.connection!=null) {
			try{
				log.warn("JMS connection Interupted - stopping it");
				this.connection.stop();
			}
			catch(JMSException e){
				log.error(e.getMessage());
			}
		}
	}
	
	public void transportResumed() {
		if (this.connection!=null) {
			try{
				log.warn("JMS connection Resumed");
				this.connection.start();
				resendPendingMessage();
			}
			catch(JMSException e){
				log.error(e.getMessage());
			}
		}
	}

	public void onCommand(Object arg0) {

	}
	
	public boolean isStarted(){
		ActiveMQConnection activeMQ = (ActiveMQConnection) this.connection;
		return activeMQ.isStarted();
	}
	
	@SuppressWarnings("unchecked")
	public void resendPendingMessage(){
		final JdbcTemplate jt = this.dbSessionManager.getJdbcTemplate();
		String sql = "SELECT id, pending_message FROM " + this.table;
		List<Map<Integer, Map<String, SerializableObject>>> list = jt.query(sql, new jmsRowMapper());
		for(int i = 0; i < list.size(); i++){
			Map<Integer, Map<String, SerializableObject>> persistentMessage = list.get(i);
			try {
				for(Integer pKey : persistentMessage.keySet()){
					if(isStarted()){
						Map<String, SerializableObject> pendingMessage = persistentMessage.get(pKey);
						for(String topicKey : pendingMessage.keySet()){
							Session ses = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
							javax.jms.Topic topic = ses.createTopic(topicKey);
							MessageProducer producer = ses.createProducer(topic);
							producer.setDeliveryMode(DeliveryMode.PERSISTENT);
							producer.setTimeToLive(0);
							ObjectMessage message = ses.createObjectMessage(pendingMessage.get(topicKey));
							producer.send(message);
							producer.close();
							ses.close();
						}
						deletePendingMessage(pKey);
					}
				}
			} 
			catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}
	
	static class jmsRowMapper implements RowMapper {
		@SuppressWarnings("unchecked")
		public Object mapRow(final ResultSet rs, final int index) throws SQLException {
			final Integer pKey = rs.getInt("id");
//				byte[] blob = rs.getBytes("pending_message");
//				ByteArrayInputStream bis = new ByteArrayInputStream(blob);
//				ObjectInputStream objectInputStream = new ObjectInputStream(bis);
//				Map<String, SerializableObject> pendingMessage = (Map<String, SerializableObject>) objectInputStream.readObject();
			Map<Integer, Map<String, SerializableObject>> persistentMessage = new HashMap<Integer, Map<String,SerializableObject>>();
//				persistentMessage.put(pKey, pendingMessage);
			return persistentMessage;
		}
	}
	
	private void deletePendingMessage(Integer ackMessage){
		try{
			final JdbcTemplate jt = this.dbSessionManager.getJdbcTemplate();
			String hql = "DELETE FROM " + this.table;
			hql = hql + " WHERE id = "+ ackMessage;
			if(jt != null){
				jt.execute(hql);
			}
		}
		catch(DataAccessException e){
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}
}
