package de.act.centralserver.applayer.gateway;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.apache.log4j.Logger;

/**
 * @author Martin Sachs
 * @since 1.0 - 03.11.2006
 */
public class ConsumerExceptionListener implements ExceptionListener {
    /**
     * Logger for this class
     */
    private static final Logger log = Logger.getLogger(ConsumerExceptionListener.class);

    /**
     * @author Martin Sachs
     * @since 1.0 - 03.11.2006
     * @param gateway
     */
    public ConsumerExceptionListener(ActernityGateway gateway) {
	   
    }

    /* (non-Javadoc)
     * @see javax.jms.ExceptionListener#onException(javax.jms.JMSException)
     */
    public void onException(JMSException arg0) {
	   log.error(arg0.getMessage(),arg0);
    }
}
