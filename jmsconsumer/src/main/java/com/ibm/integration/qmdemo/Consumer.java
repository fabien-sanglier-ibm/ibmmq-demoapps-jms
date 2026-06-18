package com.ibm.integration.qmdemo;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

public class Consumer {

	private static final Logger LOGGER = LoggerFactory.getLogger(Consumer.class);
	private static volatile boolean running = true;

        public static void main(String[] args) {
        	Connection connection = null;
        	Session session = null;
        	MessageConsumer consumer = null;
       
        	// Add shutdown hook for graceful termination
        	Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        		LOGGER.info("Shutdown signal received, stopping consumer gracefully");
        		running = false;
        	}));
       
        	try {
        		// Load and validate configuration
        		String mqHost = getEnvOrDefault("MQ_HOST", "qmdemo-ibm-mq");
        		int mqPort = parseIntWithValidation("MQ_PORT", getEnvOrDefault("MQ_PORT", "1414"), 1, 65535);
        		String mqQueueManager = getEnvOrDefault("MQ_QUEUE_MANAGER", "QMDEMO");
        		String mqChannel = getEnvOrDefault("MQ_CHANNEL", "DEV.APP.SVRCONN.0TLS");
        		String mqAppName = getEnvOrDefault("MQ_APP_NAME", "MY-CONSUMER");
        		String mqQueueName = getEnvOrDefault("MQ_QUEUE_NAME", "DEV.QUEUE.1");
        		String mqUsername = getEnvOrDefault("MQ_APP_USERNAME", "app");
        		String mqCipherSuite = System.getenv("MQ_SSL_CIPHER_SUITE");
        		String mqSslKeyStore = System.getenv("MQ_SSL_KEYSTORE_PATH");
        		String mqSslKeyStorePassword = System.getenv("MQ_SSL_KEYSTORE_PASSWORD");
        		String mqSslTrustStore = System.getenv("MQ_SSL_TRUSTSTORE_PATH");
        		String mqSslTrustStorePassword = System.getenv("MQ_SSL_TRUSTSTORE_PASSWORD");
        		boolean mqSslPeerNameEnabled = Boolean.parseBoolean(getEnvOrDefault("MQ_SSL_PEER_NAME_ENABLED", "true"));
        		long receiveSleepMillis = parseLongWithValidation("MQ_RECEIVE_SLEEP_MILLIS", getEnvOrDefault("MQ_RECEIVE_SLEEP_MILLIS", "500"), 0, Long.MAX_VALUE);
        		long receiveTimeoutMillis = parseLongWithValidation("MQ_RECEIVE_TIMEOUT_MILLIS", getEnvOrDefault("MQ_RECEIVE_TIMEOUT_MILLIS", "1000"), 100, Long.MAX_VALUE);
        		String mqAppPassword = System.getenv("MQ_APP_PASSWORD");
       
        		LOGGER.debug("Resolved MQ environment configuration for consumer startup");
       
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
                        LOGGER.debug("Consumer receive sleep configured to {} ms", Long.valueOf(receiveSleepMillis));
                        LOGGER.debug("Consumer receive timeout configured to {} ms", Long.valueOf(receiveTimeoutMillis));
                     
                        LOGGER.info("Starting consumer connection");
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
                        Destination getFrom = session.createQueue(mqQueueName);
                        consumer = session.createConsumer(getFrom);
                        LOGGER.debug("JMS consumer created for queue {}", mqQueueName);
                     
                        int messageCount = 1;
                        connection.start();
                        LOGGER.info("Consumer started and waiting for messages");
                     
                        while (running) {
                        	LOGGER.debug("Waiting to receive message {}", Integer.valueOf(messageCount));
                        	Message message = consumer.receive(receiveTimeoutMillis);
                        	
                        	if (message != null) {
                        		LOGGER.info("Received message, count={}", Integer.valueOf(messageCount));
                        		LOGGER.debug("Received JMS message type={}", message.getClass().getName());
                        		messageCount++;
                        		Thread.sleep(receiveSleepMillis);
                        	} else {
                        		LOGGER.debug("No message received within timeout, checking if still running");
                        	}
                        }
                     
                        LOGGER.info("Consumer stopped gracefully after processing {} messages", Integer.valueOf(messageCount - 1));
                     
                       } catch (JMSException e) {
                        LOGGER.error("JMS error occurred in consumer", e);
                        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                        	LOGGER.error("Caused by", cause);
                        }
                       } catch (NumberFormatException e) {
                        LOGGER.error("Invalid numeric configuration value", e);
                       } catch (IllegalArgumentException e) {
                        LOGGER.error("Invalid configuration", e);
                       } catch (InterruptedException e) {
                        LOGGER.warn("Consumer interrupted", e);
                        Thread.currentThread().interrupt();
                       } catch (Exception e) {
                        LOGGER.error("Unexpected error in consumer", e);
                        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                        	LOGGER.error("Caused by", cause);
                        }
                       } finally {
                        // Ensure resources are properly closed
                        closeQuietly(consumer, "MessageConsumer");
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
