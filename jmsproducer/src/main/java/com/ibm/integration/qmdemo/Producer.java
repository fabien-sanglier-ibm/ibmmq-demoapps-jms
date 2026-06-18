package com.ibm.integration.qmdemo;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

public class Producer {

	private static final Logger LOGGER = LoggerFactory.getLogger(Producer.class);

	       public static void main(String[] args) {
        	Connection connection = null;
        	Session session = null;
        	MessageProducer producer = null;
       
        	try {
        		// Load and validate configuration
        		String mqHost = getEnvOrDefault("MQ_HOST", "qmdemo-ibm-mq");
        		int mqPort = parseIntWithValidation("MQ_PORT", getEnvOrDefault("MQ_PORT", "1414"), 1, 65535);
        		String mqQueueManager = getEnvOrDefault("MQ_QUEUE_MANAGER", "QMDEMO");
        		String mqChannel = getEnvOrDefault("MQ_CHANNEL", "DEV.APP.SVRCONN.0TLS");
        		String mqAppName = getEnvOrDefault("MQ_APP_NAME", "MY-PRODUCER");
        		String mqQueueName = getEnvOrDefault("MQ_QUEUE_NAME", "DEV.QUEUE.1");
        		String mqUsername = getEnvOrDefault("MQ_APP_USERNAME", "app");
        		String mqCipherSuite = System.getenv("MQ_SSL_CIPHER_SUITE");
        		String mqSslKeyStore = System.getenv("MQ_SSL_KEYSTORE_PATH");
        		String mqSslKeyStorePassword = System.getenv("MQ_SSL_KEYSTORE_PASSWORD");
        		String mqSslTrustStore = System.getenv("MQ_SSL_TRUSTSTORE_PATH");
        		String mqSslTrustStorePassword = System.getenv("MQ_SSL_TRUSTSTORE_PASSWORD");
        		boolean mqSslPeerNameEnabled = Boolean.parseBoolean(getEnvOrDefault("MQ_SSL_PEER_NAME_ENABLED", "true"));
        		String mqMessage = getEnvOrDefault("MQ_MESSAGE", "Test some data here");
        		boolean continuousMode = Boolean.parseBoolean(getEnvOrDefault("MQ_CONTINUOUS_MODE", "true"));
        		int messageCount = parseIntWithValidation("MQ_MESSAGE_COUNT", getEnvOrDefault("MQ_MESSAGE_COUNT", "4000"), 1, Integer.MAX_VALUE);
        		long sendSleepMillis = parseLongWithValidation("MQ_SEND_SLEEP_MILLIS", getEnvOrDefault("MQ_SEND_SLEEP_MILLIS", "1000"), 0, Long.MAX_VALUE);
        		int logFrequency = parseIntWithValidation("MQ_LOG_FREQUENCY", getEnvOrDefault("MQ_LOG_FREQUENCY", "10"), 1, Integer.MAX_VALUE);
        		String mqAppPassword = System.getenv("MQ_APP_PASSWORD");
       
        		LOGGER.debug("Resolved MQ environment configuration for producer startup");
       
        		// Check if mTLS is configured (client certificate authentication)
        		boolean isMtlsConfigured = (mqCipherSuite != null && !mqCipherSuite.isEmpty())
        				&& (mqSslKeyStore != null && !mqSslKeyStore.isEmpty());
        		
        		// Check if password is provided
        		boolean hasPassword = (mqAppPassword != null && !mqAppPassword.isEmpty());
        		
        		// Password is required only if mTLS is not configured
        		if (!isMtlsConfigured && !hasPassword) {
        			throw new IllegalArgumentException("No environment variable supplied for password. Either provide MQ_APP_PASSWORD or configure mTLS with MQ_SSL_CIPHER_SUITE and MQ_SSL_KEYSTORE_PATH.");
        		}
        		
        		// Log authentication method
        		if (isMtlsConfigured && hasPassword) {
        			LOGGER.info("Using mTLS client certificate + username/password authentication");
        		} else if (isMtlsConfigured) {
        			LOGGER.info("Using mTLS client certificate authentication only");
        		} else if (hasPassword) {
        			LOGGER.info("Using username/password authentication");
        		}

                        // Configure MQ connection factory
                        MQConnectionFactory connectionFactory = new MQConnectionFactory();
                     
                        LOGGER.debug("Configuring MQ connection factory");
                        connectionFactory.setHostName(mqHost);
                        connectionFactory.setPort(mqPort);
                        
                        // Configure TLS/mTLS if cipher suite is provided
                        if (mqCipherSuite != null && !mqCipherSuite.isEmpty()) {
                        	connectionFactory.setSSLCipherSuite(mqCipherSuite);
                        	LOGGER.debug("TLS cipher suite configured: {}", mqCipherSuite);
                        	
                        	// Configure client certificate keystore for mTLS
                        	if (mqSslKeyStore != null && !mqSslKeyStore.isEmpty()) {
                        		System.setProperty("javax.net.ssl.keyStore", mqSslKeyStore);
                        		LOGGER.debug("SSL keystore path configured: {}", mqSslKeyStore);
                        		
                        		if (mqSslKeyStorePassword != null && !mqSslKeyStorePassword.isEmpty()) {
                        			System.setProperty("javax.net.ssl.keyStorePassword", mqSslKeyStorePassword);
                        			LOGGER.debug("SSL keystore password configured");
                        		}
                        	}
                        	
                        	// Configure truststore for server certificate validation
                        	if (mqSslTrustStore != null && !mqSslTrustStore.isEmpty()) {
                        		System.setProperty("javax.net.ssl.trustStore", mqSslTrustStore);
                        		LOGGER.debug("SSL truststore path configured: {}", mqSslTrustStore);
                        		
                        		if (mqSslTrustStorePassword != null && !mqSslTrustStorePassword.isEmpty()) {
                        			System.setProperty("javax.net.ssl.trustStorePassword", mqSslTrustStorePassword);
                        			LOGGER.debug("SSL truststore password configured");
                        		}
                        	}
                        	
                        	// Configure peer name verification
                        	if (!mqSslPeerNameEnabled) {
                        		connectionFactory.setSSLPeerName("*");
                        		LOGGER.warn("SSL peer name verification disabled - not recommended for production");
                        	}
                        	
                        	LOGGER.info("mTLS configuration applied");
                        }
                        
                        connectionFactory.setQueueManager(mqQueueManager);
                        connectionFactory.setChannel(mqChannel);
                        connectionFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
                        connectionFactory.setAppName(mqAppName);
                        connectionFactory.setClientReconnectOptions(WMQConstants.WMQ_CLIENT_RECONNECT);
                     
                        LOGGER.info(
                        		"MQ connection configuration prepared for host={}, port={}, queueManager={}, channel={}, queue={}, appName={}, tlsCipherSuite={}",
                        		mqHost,
                        		Integer.valueOf(mqPort),
                        		mqQueueManager,
                        		mqChannel,
                        		mqQueueName,
                        		mqAppName,
                        		mqCipherSuite != null && !mqCipherSuite.isEmpty() ? mqCipherSuite : "<not set>");
                        LOGGER.debug("Producer send loop configured with continuousMode={}, messageCount={}, sendSleepMillis={} ms, logFrequency={}",
                        		Boolean.valueOf(continuousMode),
                        		Integer.valueOf(messageCount),
                        		Long.valueOf(sendSleepMillis),
                        		Integer.valueOf(logFrequency));
                     
                        LOGGER.info("Starting producer connection");
                        // Use appropriate authentication method based on configuration
                        if (hasPassword) {
                        	// Username/password authentication (with or without mTLS)
                        	connection = connectionFactory.createConnection(mqUsername, mqAppPassword);
                        	LOGGER.debug("MQ connection created successfully using username/password");
                        } else {
                        	// mTLS only: client certificate provides authentication
                        	connection = connectionFactory.createConnection();
                        	LOGGER.debug("MQ connection created successfully using mTLS client certificate only");
                        }
                     
                        LOGGER.debug("Creating JMS session");
                        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                        Destination sendTo = session.createQueue(mqQueueName);
                        producer = session.createProducer(sendTo);
                        LOGGER.debug("JMS producer created for queue {}", mqQueueName);
                     
                        Message message = session.createTextMessage(mqMessage);
                        LOGGER.debug("Prepared JMS text message with payload length={}", Integer.valueOf(mqMessage.length()));
                        
                        if (continuousMode) {
                        	LOGGER.info("Starting message send loop in CONTINUOUS mode (will run indefinitely)");
                        	int messagesSent = 0;
                        	while (true) {
                        		messagesSent++;
                        		LOGGER.debug("Sending message iteration {}", Integer.valueOf(messagesSent));
                        		producer.send(message);
                        		if (messagesSent % logFrequency == 0) {
                        			LOGGER.info("Sent {} messages (continuous mode)", Integer.valueOf(messagesSent));
                        		}
                        		Thread.sleep(sendSleepMillis);
                        	}
                        } else {
                        	LOGGER.info("Starting message send loop for {} messages", Integer.valueOf(messageCount));
                        	for (int i = 0; i < messageCount; i++) {
                        		LOGGER.debug("Sending message iteration {}", Integer.valueOf(i + 1));
                        		producer.send(message);
                        		if ((i + 1) % logFrequency == 0 || i + 1 == messageCount) {
                        			LOGGER.info("Sent {} messages", Integer.valueOf(i + 1));
                        		}
                        		Thread.sleep(sendSleepMillis);
                        	}
                        	LOGGER.info("Producer finished successfully");
                        }
                     
                       } catch (JMSException e) {
                        LOGGER.error("JMS error occurred in producer", e);
                        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                        	LOGGER.error("Caused by", cause);
                        }
                       } catch (NumberFormatException e) {
                        LOGGER.error("Invalid numeric configuration value", e);
                       } catch (IllegalArgumentException e) {
                        LOGGER.error("Invalid configuration", e);
                       } catch (InterruptedException e) {
                        LOGGER.warn("Producer interrupted", e);
                        Thread.currentThread().interrupt();
                       } catch (Exception e) {
                        LOGGER.error("Unexpected error in producer", e);
                        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                        	LOGGER.error("Caused by", cause);
                        }
                       } finally {
                        // Ensure resources are properly closed
                        closeQuietly(producer, "MessageProducer");
                        closeQuietly(session, "Session");
                        closeQuietly(connection, "Connection");
                       }
                      }

        private static String getEnvOrDefault(String name, String defaultValue) {
        	String value = System.getenv(name);
        	if (value == null || value.isEmpty()) {
        		return defaultValue;
        	}
        	return value;
        }
       
        private static int parseIntWithValidation(String paramName, String value, int min, int max) {
        	try {
        		int parsed = Integer.parseInt(value);
        		if (parsed < min || parsed > max) {
        			throw new IllegalArgumentException(
        				String.format("Parameter %s value %d is out of valid range [%d, %d]", paramName, parsed, min, max));
        		}
        		return parsed;
        	} catch (NumberFormatException e) {
        		throw new IllegalArgumentException(
        			String.format("Parameter %s has invalid integer value: %s", paramName, value), e);
        	}
        }
       
        private static long parseLongWithValidation(String paramName, String value, long min, long max) {
        	try {
        		long parsed = Long.parseLong(value);
        		if (parsed < min || parsed > max) {
        			throw new IllegalArgumentException(
        				String.format("Parameter %s value %d is out of valid range [%d, %d]", paramName, parsed, min, max));
        		}
        		return parsed;
        	} catch (NumberFormatException e) {
        		throw new IllegalArgumentException(
        			String.format("Parameter %s has invalid long value: %s", paramName, value), e);
        	}
        }
       
        private static void closeQuietly(AutoCloseable resource, String resourceName) {
        	if (resource != null) {
        		try {
        			resource.close();
        			LOGGER.debug("{} closed successfully", resourceName);
        		} catch (Exception e) {
        			LOGGER.warn("Failed to close {}", resourceName, e);
        		}
        	}
        }
       }
       
       // Made with Bob
