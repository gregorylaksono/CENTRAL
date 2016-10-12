package de.act.centralserver.applayer.gateway;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import org.apache.activemq.ActiveMQConnection;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

import de.act.blackbox.applayer.interfaces.IStaticDataService;
import de.act.centralserver.dblayer.core.CDBSessionManager;
import de.act.common.interfaces.IActernityGatewayServer;
import de.act.common.jmstypes.SerializableObject;

/**
 * Gateway for handling acternityAgents. This gateway implements forwarding for the 
 * Agents (Softwarekomponents) from ABox to ACentral and wise versa.
 * 
 * <note> This class is configured with spring xml. So you dont find any Factorypattern here.</note>  
 * @author Martin Sachs & Henry
 * @since 1.0 - 30.01.2007
 * @update 03.11.2009
 */
public class ActernityGateway implements MessageListener, IActernityGatewayServer {
	/**
	 * Logger for this class
	 */
	private static final Logger log = Logger.getLogger(ActernityGateway.class);
	private String table;
	private JmsTemplate serverJmsTemplate;
	private String clientSendTopic;
	private String clientRecieveTopic;
	private String serverSendTopic;
	private String serverRecieveTopic;
	private String address;
	private Connection serverConnection;
	private Connection clientConnection;
	private Session serverSession;
	private TopicSubscriber serverConsumer;
	private JMSServerTransportListener tl;

	private String serverReceiveSAreaTopic;
	private String serverReceiveFAreaTopic;
	private String serverReceiveCAreaTopic;
	private TopicSubscriber serverReceiveSAreaConsumer;
	private TopicSubscriber serverReceiveFAreaConsumer;
	private TopicSubscriber serverReceiveCAreaConsumer;

	private CArea centralArea;
	private SArea staticArea;
	private FArea formularArea;
	private String clientID;

	private IStaticDataService staticDataService;
	private CDBSessionManager dbSessionManager;

	public ActernityGateway() {
		super();
	}

	public void init() {
		// init connections
		address = staticDataService.getID_DB().toString();
		initServerConnection();
		initServerConsumer();
		resendPendingMessage();
		System.out.println("Central JMS Started...");
	}

	private void initServerConnection() {
		try{
			serverConnection = this.serverJmsTemplate.getConnectionFactory().createConnection();
			serverConnection.setClientID(serverSendTopic + this.address);
			//serverConnection.start();
			//TransportListener tl = new JMSServerTransportListener(serverConnection);
			tl = new JMSServerTransportListener(serverConnection, dbSessionManager, table);
			if (serverConnection instanceof ActiveMQConnection){
				ActiveMQConnection a = (ActiveMQConnection) serverConnection;
				a.addTransportListener(tl);				
			}
		}
		catch(JMSException e){
			System.out.println((e.getMessage()));
			System.exit(1);
		}
		catch(RuntimeException e){
			log.fatal(e.getMessage(), e);
			System.exit(1);
		}
	}
	@SuppressWarnings("unused")
	private void initServerConsumer() {
		try{
			this.serverConnection.setExceptionListener(new ConsumerExceptionListener(this));
			this.serverConnection.start();
			this.serverSession = this.serverConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			// create Destination
			Topic formularTopic = null;
			Topic otherTopic = null;

			Topic ReceiveSAreaTopic = this.serverSession.createTopic(this.serverReceiveSAreaTopic);
			Topic ReceiveFAreaTopic = this.serverSession.createTopic(this.serverReceiveFAreaTopic);
			Topic ReceiveCAreaTopic = this.serverSession.createTopic(this.serverReceiveCAreaTopic);

			formularTopic = this.serverSession.createTopic(this.serverRecieveTopic/* + this.address*/);
			otherTopic = this.serverSession.createTopic(this.serverRecieveTopic);
			this.serverConsumer = this.serverSession.createDurableSubscriber(formularTopic, "13");
			this.serverConsumer.setMessageListener(this);

			this.serverReceiveCAreaConsumer = this.serverSession.createDurableSubscriber(ReceiveCAreaTopic, "ServerCArea");
			this.serverReceiveCAreaConsumer.setMessageListener(centralArea);

			this.serverReceiveSAreaConsumer = this.serverSession.createDurableSubscriber(ReceiveSAreaTopic, "ServerSArea");
			this.serverReceiveSAreaConsumer.setMessageListener(staticArea);

			this.serverReceiveFAreaConsumer = this.serverSession.createDurableSubscriber(ReceiveFAreaTopic, "ServerFArea");
			this.serverReceiveFAreaConsumer.setMessageListener(formularArea);
		}
		catch(final JMSException e1){
			log.error(e1.getMessage());
		}
		catch(final Exception e){
			log.error(" ", e);
			e.printStackTrace();
		}
	}
	
	private void resendPendingMessage(){
		tl.resendPendingMessage();
	}

	public void onMessage(Message arg0) {

	}

	public void sendToClient(String stringTopic, SerializableObject objectMessage){
		try{ 
			if(tl.isStarted()){
				//System.out.println("[DEBUG] central try to distribute data");
				Session ses = serverConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				javax.jms.Topic topic = ses.createTopic(stringTopic);
				MessageProducer producer = ses.createProducer(topic);
				producer.setDeliveryMode(DeliveryMode.PERSISTENT);
				producer.setTimeToLive(0);
				ObjectMessage message = ses.createObjectMessage(objectMessage);
				producer.send(message);
				producer.close();
				ses.close();
			}
			else{
				Map<Integer, Map<String, SerializableObject>> persistentMessage = new HashMap<Integer, Map<String,SerializableObject>>();
				Map<String, SerializableObject> incomingMessage = new HashMap<String, SerializableObject>();
				incomingMessage.put(stringTopic, objectMessage);
				Integer pkey = this.getNextAddressID();
				persistentMessage.put(pkey, incomingMessage);
				String sql = "INSERT INTO "+ this.table +" values(?, ?)";
				PreparedStatement preparedStatement = this.dbSessionManager.getJdbcTemplate().getDataSource().getConnection().prepareStatement(sql);
				preparedStatement.setInt(1, pkey);
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
				objectOutputStream.writeObject(persistentMessage.get(pkey));
				objectOutputStream.flush();
				objectOutputStream.close();
				byte[] data = byteArrayOutputStream.toByteArray();
				ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
				preparedStatement.setBinaryStream(2, byteArrayInputStream, data.length);
				preparedStatement.executeUpdate();
				preparedStatement.close();
				byteArrayOutputStream.close();
				byteArrayInputStream.close();
			}
		}
		catch(JMSException e){
			log.error(e.getMessage());
		}
		catch(IOException e){
			log.error(e.getMessage());
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public final String getServerRecieveTopic() {
		return serverRecieveTopic;
	}

	public final void setServerRecieveTopic(String serverRecieveTopic) {
		this.serverRecieveTopic = serverRecieveTopic;
	}

	public final String getServerSendTopic() {
		return serverSendTopic;
	}

	public final void setServerSendTopic(String serverSendTopic) {
		this.serverSendTopic = serverSendTopic;
	}

	public void setClientID(String startupID) {
		this.clientID = startupID;
	}

	public final void setClientRecieveTopic(String centralClientTopic) {
		this.clientRecieveTopic = centralClientTopic;
	}

	public final void setClientSendTopic(String centralServerTopic) {
		this.clientSendTopic = centralServerTopic;
	}

	public final void setServerJmsTemplate(JmsTemplate serverJmsTemplate) {
		this.serverJmsTemplate = serverJmsTemplate;
	}

	public final void setAddress(String address) {
		this.address = address;
	}

	public String getServerReceiveSAreaTopic() {
		return serverReceiveSAreaTopic;
	}

	public void setServerReceiveSAreaTopic(String serverReceiveSAreaTopic) {
		this.serverReceiveSAreaTopic = serverReceiveSAreaTopic;
	}

	public String getServerReceiveFAreaTopic() {
		return serverReceiveFAreaTopic;
	}

	public void setServerReceiveFAreaTopic(String serverReceiveFAreaTopic) {
		this.serverReceiveFAreaTopic = serverReceiveFAreaTopic;
	}

	public String getServerReceiveCAreaTopic() {
		return serverReceiveCAreaTopic;
	}

	public void setServerReceiveCAreaTopic(String serverReceiveCAreaTopic) {
		this.serverReceiveCAreaTopic = serverReceiveCAreaTopic;
	}

	public Connection getServerConnection() {
		return serverConnection;
	}

	public void setServerConnection(Connection serverConnection) {
		this.serverConnection = serverConnection;
	}

	public Connection getClientConnection() {
		return clientConnection;
	}

	public void setClientConnection(Connection clientConnection) {
		this.clientConnection = clientConnection;
	}

	public Session getServerSession() {
		return serverSession;
	}

	public void setServerSession(Session serverSession) {
		this.serverSession = serverSession;
	}

	public TopicSubscriber getServerConsumer() {
		return serverConsumer;
	}

	public void setServerConsumer(TopicSubscriber serverConsumer) {
		this.serverConsumer = serverConsumer;
	}

	public TopicSubscriber getServerReceiveSAreaConsumer() {
		return serverReceiveSAreaConsumer;
	}

	public void setServerReceiveSAreaConsumer(
			TopicSubscriber serverReceiveSAreaConsumer) {
		this.serverReceiveSAreaConsumer = serverReceiveSAreaConsumer;
	}

	public TopicSubscriber getServerReceiveFAreaConsumer() {
		return serverReceiveFAreaConsumer;
	}

	public void setServerReceiveFAreaConsumer(TopicSubscriber serverReceiveFAreaConsumer) {
		this.serverReceiveFAreaConsumer = serverReceiveFAreaConsumer;
	}

	public TopicSubscriber getServerReceiveCAreaConsumer() {
		return serverReceiveCAreaConsumer;
	}

	public void setServerReceiveCAreaConsumer(TopicSubscriber serverReceiveCAreaConsumer) {
		this.serverReceiveCAreaConsumer = serverReceiveCAreaConsumer;
	}

	public JmsTemplate getServerJmsTemplate() {
		return serverJmsTemplate;
	}

	public String getClientSendTopic() {
		return clientSendTopic;
	}

	public String getClientRecieveTopic() {
		return clientRecieveTopic;
	}

	public String getAddress() {
		return address;
	}

	public String getClientID() {
		return clientID;
	}

	public CArea getCentralArea() {
		return centralArea;
	}

	public void setCentralArea(CArea centralArea) {
		this.centralArea = centralArea;
	}

	public SArea getStaticArea() {
		return staticArea;
	}

	public void setStaticArea(SArea staticArea) {
		this.staticArea = staticArea;
	}

	public FArea getFormularArea() {
		return formularArea;
	}

	public void setFormularArea(FArea formularArea) {
		this.formularArea = formularArea;
	}

	public IStaticDataService getStaticDataService() {
		return staticDataService;
	}

	public void setStaticDataService(IStaticDataService staticDataService) {
		this.staticDataService = staticDataService;
	}

	public CDBSessionManager getDbSessionManager() {
		return dbSessionManager;
	}

	public void setDbSessionManager(CDBSessionManager dbSessionManager) {
		this.dbSessionManager = dbSessionManager;
	}
	
	private Integer getNextAddressID() {
		int id = -1;
		String hql = "SELECT NEXTVAL ('d_db_seq');";
		final JdbcTemplate jt = this.dbSessionManager.getJdbcTemplate();
		if( jt != null ) {
			id = jt.queryForInt(hql);
		}
		return id;
	}
}

/*BrokerInfo brokerInfo = a.getBrokerInfo();
a.
ConnectionInfo connectInfo = a.getConnectionInfo();
connectInfo.*/
/*System.out.println(a.isUseAsyncSend() + ", " + a.getBrokerInfo().getBrokerURL());
System.out.println(ActiveMQConnection.DEFAULT_BROKER_URL);
System.out.println(ActiveMQConnection.DEFAULT_PASSWORD);
System.out.println(ActiveMQConnection.DEFAULT_USER);*/