# IBM MQ Demo Apps - JMS

Simple project demonstrating JMS Producer and Consumer applications for IBM MQ.

## Overview

This project contains two Java applications that demonstrate messaging with IBM MQ using JMS:
- **Producer**: Sends messages to an IBM MQ queue
- **Consumer**: Receives messages from an IBM MQ queue

## Recent Updates (2026-06-18)

### Security & Dependency Updates
- **Java Version**: Upgraded from Java 1.7 to **Java 17 (LTS)** for active security support
- **IBM MQ Client**: Updated from 9.3.5.0 to **9.4.1.0** (latest stable)
- **JUnit**: Updated from 4.13.1 to **4.13.2** (security fixes)
- **SLF4J**: Updated from 1.7.36 to **2.0.16** (latest stable)
- **Maven JAR Plugin**: Updated from 2.4 to **3.4.2**

### Code Robustness Improvements
- **Resource Management**: Proper cleanup of JMS resources (Connection, Session, Producer/Consumer) using try-finally blocks
- **Configuration Validation**: Added validation for numeric environment variables with range checks
- **Error Handling**: Specific exception handling for JMS, configuration, and interruption errors
- **Graceful Shutdown**: Consumer now supports graceful shutdown via shutdown hooks
- **Null Safety**: Consumer handles null messages from receive() timeouts
- **Code Quality**: Removed magic numbers, improved variable naming, better logging practices

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Access to an IBM MQ Queue Manager

## Project Structure

```
ibmmq-demoapps-jms/
├── jmsproducer/              # Producer application
│   ├── src/main/java/
│   │   └── com/ibm/integration/qmdemo/
│   │       ├── Producer.java
│   │       └── App.java
│   ├── pom.xml
│   └── Dockerfile
├── jmsconsumer/              # Consumer application
│   ├── src/main/java/
│   │   └── com/ibm/integration/qmdemo/
│   │       ├── Consumer.java
│   │       └── App.java
│   ├── pom.xml
│   └── Dockerfile
└── deployment-openshift/     # OpenShift deployment files
```

## Configuration

Both applications are configured via environment variables:

### Common Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `MQ_HOST` | MQ host name | `qmdemo-ibm-mq` |
| `MQ_PORT` | MQ port number (1-65535) | `1414` |
| `MQ_QUEUE_MANAGER` | Queue manager name | `QMDEMO` |
| `MQ_CHANNEL` | Server connection channel | `DEV.APP.SVRCONN.0TLS` |
| `MQ_QUEUE_NAME` | Queue name | `DEV.QUEUE.1` |
| `MQ_APP_USERNAME` | Application username | `app` |
| `MQ_APP_PASSWORD` | Application password (required unless using mTLS) | - |

### TLS/mTLS Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `MQ_SSL_CIPHER_SUITE` | TLS cipher suite (e.g., `TLS_RSA_WITH_AES_128_CBC_SHA256`) | - |
| `MQ_SSL_CA_CERT_PATH` | Path to CA certificate (PEM format) - **recommended approach** | - |
| `MQ_SSL_TRUSTSTORE_PATH` | Path to truststore (JKS) - alternative to CA cert | - |
| `MQ_SSL_TRUSTSTORE_PASSWORD` | Password for truststore (not needed with CA cert) | - |
| `MQ_SSL_KEYSTORE_PATH` | Path to client keystore (JKS/PKCS12) for mTLS | - |
| `MQ_SSL_KEYSTORE_PASSWORD` | Password for client keystore | - |
| `MQ_SSL_PEER_NAME_ENABLED` | Enable SSL peer name verification (true/false) | `true` |
| `MQ_SSL_HOSTNAME_VERIFICATION_ENABLED` | Enable hostname verification (true/false) | `true` |

### Producer-Specific Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `MQ_APP_NAME` | Application name | `MY-PRODUCER` |
| `MQ_MESSAGE` | Message content to send | `Test some data here` |
| `MQ_CONTINUOUS_MODE` | Run continuously (true/false) | `true` |
| `MQ_MESSAGE_COUNT` | Number of messages to send (ignored in continuous mode) | `4000` |
| `MQ_SEND_SLEEP_MILLIS` | Sleep between sends (ms) | `1000` |
| `MQ_LOG_FREQUENCY` | Log progress every N messages | `10` |

### Consumer-Specific Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `MQ_APP_NAME` | Application name | `MY-CONSUMER` |
| `MQ_RECEIVE_SLEEP_MILLIS` | Sleep after receive (ms) | `500` |
| `MQ_RECEIVE_TIMEOUT_MILLIS` | Receive timeout (ms) | `1000` |

## Building

Build both applications using Maven with the OpenShift profile:

```bash
# Build Producer
cd jmsproducer
mvn clean package -Popenshift

# Build Consumer
cd jmsconsumer
mvn clean package -Popenshift
```

This creates executable JAR files with all dependencies:
- `jmsproducer/target/producer-1.0-SNAPSHOT-jar-with-dependencies.jar`
- `jmsconsumer/target/consumer-1.0-SNAPSHOT-jar-with-dependencies.jar`

## Running Locally

### Producer

**Continuous Mode (run indefinitely - default):**
```bash
export MQ_HOST=localhost
export MQ_PORT=1414
export MQ_QUEUE_MANAGER=QM1
export MQ_CHANNEL=DEV.APP.SVRCONN
export MQ_QUEUE_NAME=DEV.QUEUE.1
export MQ_APP_USERNAME=app
export MQ_APP_PASSWORD=your-password
export MQ_SEND_SLEEP_MILLIS=1000

java -jar jmsproducer/target/producer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

**Standard Mode (send a specific number of messages):**
```bash
export MQ_HOST=localhost
export MQ_PORT=1414
export MQ_QUEUE_MANAGER=QM1
export MQ_CHANNEL=DEV.APP.SVRCONN
export MQ_QUEUE_NAME=DEV.QUEUE.1
export MQ_APP_USERNAME=app
export MQ_APP_PASSWORD=your-password
export MQ_CONTINUOUS_MODE=false
export MQ_MESSAGE_COUNT=100
export MQ_SEND_SLEEP_MILLIS=1000

java -jar jmsproducer/target/producer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

To stop the producer, use `Ctrl+C` or send a SIGTERM signal.

### Consumer

```bash
export MQ_HOST=localhost
export MQ_PORT=1414
export MQ_QUEUE_MANAGER=QM1
export MQ_CHANNEL=DEV.APP.SVRCONN
export MQ_QUEUE_NAME=DEV.QUEUE.1
export MQ_APP_USERNAME=app
export MQ_APP_PASSWORD=your-password

java -jar jmsconsumer/target/consumer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

To stop the consumer gracefully, use `Ctrl+C` or send a SIGTERM signal.

## Docker

Build and run using Docker:

```bash
# Build Producer image
cd jmsproducer
docker build -t jmsproducer:latest .

# Build Consumer image
cd jmsconsumer
docker build -t jmsconsumer:latest .

# Run Producer
docker run --rm \
  -e MQ_HOST=your-mq-host \
  -e MQ_APP_PASSWORD=your-password \
  jmsproducer:latest

# Run Consumer
docker run --rm \
  -e MQ_HOST=your-mq-host \
  -e MQ_APP_PASSWORD=your-password \
  jmsconsumer:latest
```

## GitHub Container Registry (GHCR)

Container images are automatically built and published to GitHub Container Registry via GitHub Actions.

### Available Images

- **Producer**: `ghcr.io/<owner>/<repo>/jmsproducer:latest`
- **Consumer**: `ghcr.io/<owner>/<repo>/jmsconsumer:latest`

### Image Tags

The workflow creates multiple tags for each build:
- `latest` - Latest build from the default branch
- `<branch-name>` - Branch-specific builds
- `<version>` - Semantic version tags (on releases)
- `<branch>-<sha>` - Commit-specific builds

### Using Pre-built Images

```bash
# Pull and run Producer from GHCR
docker pull ghcr.io/<owner>/<repo>/jmsproducer:latest
docker run --rm \
  -e MQ_HOST=your-mq-host \
  -e MQ_APP_PASSWORD=your-password \
  ghcr.io/<owner>/<repo>/jmsproducer:latest

# Pull and run Consumer from GHCR
docker pull ghcr.io/<owner>/<repo>/jmsconsumer:latest
docker run --rm \
  -e MQ_HOST=your-mq-host \
  -e MQ_APP_PASSWORD=your-password \
  ghcr.io/<owner>/<repo>/jmsconsumer:latest
```

### GitHub Actions Workflow

The workflow (`.github/workflows/build-and-push.yml`) automatically:
- Builds both applications with Maven (Java 17)
- Creates multi-architecture container images (amd64, arm64)
- Pushes images to GHCR on push to main/master branch
- Validates builds on pull requests (without pushing)
- Supports manual workflow dispatch
- Creates versioned tags on releases

**Automatic Triggers:**
- **Push to `main` or `master` branch** - When changes are pushed to these branches affecting:
  - `jmsproducer/` directory
  - `jmsconsumer/` directory
  - The workflow file itself
- **Pull Requests** - Builds are validated but images are not pushed
- **Release Publication** - Creates versioned image tags

**Manual Trigger:**
- Go to GitHub repository → **Actions** tab
- Select **Build and Push Container Images** workflow
- Click **Run workflow** button
- Choose branch and click **Run workflow**

**First-Time Setup:**
1. Commit and push the workflow file to your repository:
   ```bash
   git add .github/workflows/build-and-push.yml
   git commit -m "Add GitHub Actions workflow for container builds"
   git push origin main
   ```
2. The workflow runs automatically on this push
3. Check the **Actions** tab on GitHub to monitor build progress
4. Once complete, images are available at GHCR

**Permissions:**
- Uses `GITHUB_TOKEN` automatically provided by GitHub Actions
- No additional secrets or configuration needed
- Images are published to your repository's package registry

## OpenShift Deployment

Deployment files are provided in the `deployment-openshift/` directory:

```bash
# Create secret with MQ credentials
kubectl apply -f deployment-openshift/qmdemo-secret.yaml

# Deploy Producer (using GHCR image)
kubectl apply -f deployment-openshift/qmdemo-producer-deployment.yaml

# Deploy Consumer (using GHCR image)
kubectl apply -f deployment-openshift/qmdemo-consumer-deployment.yaml
```

**Note**: Update the deployment YAML files to reference GHCR images instead of OpenShift internal registry if desired.

## Features

### Producer
- **Two operating modes**:
  - **Standard mode**: Send a specific number of messages then exit
  - **Continuous mode**: Run indefinitely until stopped
- Configurable message content and send rate
- Automatic reconnection on connection loss
- Configurable progress logging frequency
- Proper resource cleanup on shutdown
- Validated configuration parameters

### Consumer
- Continuous message consumption
- Graceful shutdown support (SIGTERM/Ctrl+C)
- Automatic reconnection on connection loss
- Null-safe message handling
- Proper resource cleanup on shutdown
- Validated configuration parameters

## Error Handling

Both applications implement robust error handling:
- **JMSException**: Specific handling for JMS-related errors
- **NumberFormatException**: Clear messages for invalid numeric configuration
- **IllegalArgumentException**: Validation errors for configuration parameters
- **InterruptedException**: Proper handling of thread interruption
- **Resource Cleanup**: Guaranteed cleanup in finally blocks

## Logging

Applications use SLF4J with simple logger:
- **INFO**: Connection status, message counts, important events
- **DEBUG**: Detailed operation information (enable with `-Dorg.slf4j.simpleLogger.defaultLogLevel=debug`)
- **ERROR**: Errors with full stack traces and cause chains
- **WARN**: Non-critical issues like resource cleanup failures

## Security Considerations

### Authentication
Both applications support flexible authentication with three modes:

1. **Username/Password Authentication Only**
   - Requires `MQ_APP_USERNAME` and `MQ_APP_PASSWORD` environment variables
   - Used when mTLS is not configured
   - No default passwords - applications fail to start without credentials
   - Can be combined with TLS for encrypted transport

2. **mTLS Client Certificate Authentication Only**
   - Client certificate in keystore provides authentication
   - Password (`MQ_APP_PASSWORD`) is **not required** when mTLS is configured
   - Requires both `MQ_SSL_CIPHER_SUITE` and `MQ_SSL_KEYSTORE_PATH` to be set
   - Most secure option - certificate-based authentication

3. **Combined mTLS + Username/Password**
   - Both client certificate and username/password authentication
   - Provides dual authentication factors
   - Useful when MQ is configured to require both methods

### TLS/SSL Support
Both applications support multiple TLS configurations with two approaches for certificate trust:

#### Approach 1: CA Certificate (Recommended)
Use a PEM-formatted CA certificate directly - no JKS truststore or password needed:

**TLS with CA Certificate + Username/Password:**
```bash
export MQ_SSL_CIPHER_SUITE=TLS_RSA_WITH_AES_128_CBC_SHA256
export MQ_SSL_CA_CERT_PATH=/etc/ssl/certs/ca.crt
export MQ_APP_USERNAME=app
export MQ_APP_PASSWORD=your-password
# Encrypted transport + password authentication
# No truststore password needed!
```

**mTLS with CA Certificate (Client Certificate Authentication):**
```bash
export MQ_SSL_CIPHER_SUITE=TLS_RSA_WITH_AES_128_CBC_SHA256
export MQ_SSL_CA_CERT_PATH=/etc/ssl/certs/ca.crt
export MQ_SSL_KEYSTORE_PATH=/path/to/client-keystore.jks
export MQ_SSL_KEYSTORE_PASSWORD=keystore-password
# MQ_APP_PASSWORD not required - certificate provides authentication
```

#### Approach 2: JKS Truststore (Traditional)
Use a JKS truststore file (requires password):

**TLS with JKS Truststore + Username/Password:**
```bash
export MQ_SSL_CIPHER_SUITE=TLS_RSA_WITH_AES_128_CBC_SHA256
export MQ_SSL_TRUSTSTORE_PATH=/path/to/truststore.jks
export MQ_SSL_TRUSTSTORE_PASSWORD=truststore-password
export MQ_APP_USERNAME=app
export MQ_APP_PASSWORD=your-password
```

**mTLS with JKS Truststore:**
```bash
export MQ_SSL_CIPHER_SUITE=TLS_RSA_WITH_AES_128_CBC_SHA256
export MQ_SSL_KEYSTORE_PATH=/path/to/client-keystore.jks
export MQ_SSL_KEYSTORE_PASSWORD=keystore-password
export MQ_SSL_TRUSTSTORE_PATH=/path/to/truststore.jks
export MQ_SSL_TRUSTSTORE_PASSWORD=truststore-password
```

#### Hostname Verification Control
Control certificate hostname validation (both enabled by default for security):

```bash
# Production (recommended - both enabled)
export MQ_SSL_PEER_NAME_ENABLED=true
export MQ_SSL_HOSTNAME_VERIFICATION_ENABLED=true

# Development/Testing (disable if needed)
export MQ_SSL_PEER_NAME_ENABLED=false
export MQ_SSL_HOSTNAME_VERIFICATION_ENABLED=false
```

**Note:** Disabling verification is not recommended for production environments.

### Certificate Management
- **CA Certificate (PEM)**: Recommended - no password needed, simpler management
- **JKS Truststore**: Traditional approach - requires password
- **Keystore formats**: JKS (Java KeyStore) or PKCS12 for client certificates
- **Client certificates**: Required for mTLS authentication
- **Hostname verification**: Validates certificate CN/SAN against connection hostname
- **Peer name verification**: IBM MQ specific DN verification
- Both verification mechanisms enabled by default for security

### Additional Security
- No sensitive data logged at INFO level
- Modern Java 17 with active security updates
- Latest dependency versions with security patches
- Secure defaults for all configurations

## License

See [LICENSE](LICENSE) file for details.

## Additional Documentation

Detailed guides are available in the `docs/` directory:

- **[CA-CERTIFICATE-SETUP.md](docs/CA-CERTIFICATE-SETUP.md)** - Complete guide for using CA certificates directly (recommended)
- **[QUICK-START-CA-CERT.md](docs/QUICK-START-CA-CERT.md)** - Quick start guide for CA certificate configuration
- **[JAVA-SSL-PROPERTIES.md](docs/JAVA-SSL-PROPERTIES.md)** - Using JAVA_TOOL_OPTIONS for SSL configuration
- **[SECURE-PASSWORD-OPTIONS.md](docs/SECURE-PASSWORD-OPTIONS.md)** - Secure password management strategies
- **[HOSTNAME-VERIFICATION.md](docs/HOSTNAME-VERIFICATION.md)** - Hostname and peer name verification options
- **[DEBUG-LOGGING-GUIDE.md](docs/DEBUG-LOGGING-GUIDE.md)** - Enable debug logging for troubleshooting
- **[TROUBLESHOOTING-CA-CERT.md](docs/TROUBLESHOOTING-CA-CERT.md)** - Troubleshoot certificate validation issues

## Contributing

This is a demonstration project. For production use, consider:
- Implementing connection pooling
- Adding metrics and monitoring
- Implementing message acknowledgment strategies
- Adding transaction support
- Implementing retry logic with exponential backoff
- Adding health check endpoints
