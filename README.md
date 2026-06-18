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
| `MQ_APP_PASSWORD` | Application password (required) | - |
| `MQ_SSL_CIPHER_SUITE` | TLS cipher suite (optional) | - |

### Producer-Specific Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `MQ_APP_NAME` | Application name | `MY-PRODUCER` |
| `MQ_MESSAGE` | Message content to send | `Test some data here` |
| `MQ_MESSAGE_COUNT` | Number of messages to send | `4000` |
| `MQ_SEND_SLEEP_MILLIS` | Sleep between sends (ms) | `3000` |

### Consumer-Specific Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `MQ_APP_NAME` | Application name | `MY-CONSUMER` |
| `MQ_RECEIVE_SLEEP_MILLIS` | Sleep after receive (ms) | `500` |

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

```bash
export MQ_HOST=localhost
export MQ_PORT=1414
export MQ_QUEUE_MANAGER=QM1
export MQ_CHANNEL=DEV.APP.SVRCONN
export MQ_QUEUE_NAME=DEV.QUEUE.1
export MQ_APP_USERNAME=app
export MQ_APP_PASSWORD=your-password
export MQ_MESSAGE_COUNT=100
export MQ_SEND_SLEEP_MILLIS=1000

java -jar jmsproducer/target/producer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

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
- Configurable message count and send rate
- Automatic reconnection on connection loss
- Progress logging every 100 messages
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

- Passwords must be provided via `MQ_APP_PASSWORD` environment variable
- TLS/SSL support via `MQ_SSL_CIPHER_SUITE` configuration
- No sensitive data logged at INFO level
- Modern Java 17 with active security updates
- Latest dependency versions with security patches

## License

See [LICENSE](LICENSE) file for details.

## Contributing

This is a demonstration project. For production use, consider:
- Implementing connection pooling
- Adding metrics and monitoring
- Implementing message acknowledgment strategies
- Adding transaction support
- Implementing retry logic with exponential backoff
- Adding health check endpoints
