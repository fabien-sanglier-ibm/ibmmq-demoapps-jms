# Docker Compose Local Development Guide

This guide explains how to run the IBM MQ JMS demo applications locally using Docker Compose.

## Overview

The Docker Compose setup includes:
- **IBM MQ Server** - Queue manager with pre-configured queues
- **JMS Producer** - Sends messages to the queue
- **JMS Consumer** - Receives messages from the queue

All services run in isolated containers with automatic networking and health checks.

## Prerequisites

- Docker Desktop or Docker Engine (20.10+)
- Docker Compose (2.0+)
- 4GB RAM available for containers
- Ports 1414 and 9443 available on your machine

## Quick Start

### 1. Start All Services

```bash
# From the project root directory
docker-compose up -d
```

This will:
1. Pull the IBM MQ image
2. Build the Producer and Consumer images
3. Start all three services
4. Wait for IBM MQ to be healthy before starting applications

### 2. View Logs

```bash
# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f jms-producer
docker-compose logs -f jms-consumer
docker-compose logs -f ibmmq
```

### 3. Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

## Configuration

### Default Configuration

The `docker-compose.yml` file includes sensible defaults:

**IBM MQ:**
- Queue Manager: `QMDEMO`
- Port: `1414` (MQ), `9443` (Web Console)
- Username: `app`
- Password: `passw0rd`

**Producer:**
- Continuous mode: `true`
- Send interval: `2000ms` (2 seconds)
- Log frequency: Every 10 messages

**Consumer:**
- Receive timeout: `1000ms`
- Message counting: `false` (disabled)

### Customizing Configuration

#### Option 1: Edit docker-compose.yml

Modify the environment variables directly in `docker-compose.yml`:

```yaml
services:
  jms-producer:
    environment:
      - MQ_SEND_SLEEP_MILLIS=5000  # Change to 5 seconds
      - MQ_LOG_FREQUENCY=50         # Log every 50 messages
```

#### Option 2: Use Environment File

Create a `.env` file in the project root:

```bash
# .env file
MQ_PASSWORD=mypassword
PRODUCER_SLEEP=3000
CONSUMER_TIMEOUT=2000
```

Update `docker-compose.yml` to use these variables:

```yaml
environment:
  - MQ_APP_PASSWORD=${MQ_PASSWORD}
  - MQ_SEND_SLEEP_MILLIS=${PRODUCER_SLEEP}
```

#### Option 3: Override with Command Line

```bash
docker-compose up -d \
  -e MQ_SEND_SLEEP_MILLIS=5000 \
  -e MQ_LOG_FREQUENCY=100
```

## Common Scenarios

### Scenario 1: Testing Message Flow

**Goal:** Send a specific number of messages and verify receipt

```yaml
# In docker-compose.yml
jms-producer:
  environment:
    - MQ_CONTINUOUS_MODE=false
    - MQ_MESSAGE_COUNT=100
    - MQ_SEND_SLEEP_MILLIS=100

jms-consumer:
  environment:
    - MQ_ENABLE_MESSAGE_COUNT=true  # Enable counting
```

```bash
docker-compose up
# Watch logs to verify 100 messages sent and received
docker-compose logs -f
```

### Scenario 2: High-Volume Testing

**Goal:** Test with high message throughput

```yaml
jms-producer:
  environment:
    - MQ_CONTINUOUS_MODE=true
    - MQ_SEND_SLEEP_MILLIS=10  # Send every 10ms
    - MQ_LOG_FREQUENCY=1000     # Log every 1000 messages

jms-consumer:
  environment:
    - MQ_RECEIVE_TIMEOUT_MILLIS=100
    - MQ_RECEIVE_SLEEP_MILLIS=0  # No sleep between receives
```

### Scenario 3: Development with Live Reload

**Goal:** Test code changes without rebuilding

```bash
# Rebuild and restart specific service
docker-compose up -d --build jms-producer

# Or rebuild all
docker-compose up -d --build
```

### Scenario 4: Multiple Consumers

**Goal:** Scale consumers for load balancing

```bash
# Scale to 3 consumer instances
docker-compose up -d --scale jms-consumer=3

# View logs from all consumers
docker-compose logs -f jms-consumer
```

## Accessing IBM MQ Web Console

The IBM MQ Web Console is available at: `https://localhost:9443/ibmmq/console/`

**Credentials:**
- Username: `admin`
- Password: `passw0rd`

**Note:** You'll see a security warning because the certificate is self-signed. This is normal for local development.

### What You Can Do:
- View queue depths
- Browse messages
- Monitor connections
- View queue manager status
- Configure channels and queues

## Troubleshooting

### Issue 1: Port Already in Use

**Error:** `Bind for 0.0.0.0:1414 failed: port is already allocated`

**Solution:**
```bash
# Check what's using the port
lsof -i :1414

# Change port in docker-compose.yml
ports:
  - "11414:1414"  # Use different host port

# Update producer/consumer to use new port
environment:
  - MQ_PORT=1414  # Keep container port the same
```

### Issue 2: IBM MQ Not Starting

**Error:** Container exits immediately

**Solution:**
```bash
# Check logs
docker-compose logs ibmmq

# Ensure you accepted the license
environment:
  - LICENSE=accept

# Remove old volumes and try again
docker-compose down -v
docker-compose up -d
```

### Issue 3: Applications Can't Connect

**Error:** `JMSWMQ0018: Failed to connect to queue manager`

**Solution:**
```bash
# Check if IBM MQ is healthy
docker-compose ps

# Wait for health check to pass
docker-compose logs ibmmq | grep "Started web server"

# Verify network connectivity
docker-compose exec jms-producer ping ibmmq
```

### Issue 4: Messages Not Being Received

**Problem:** Producer sends but consumer doesn't receive

**Solution:**
```bash
# Check queue depth in MQ console
# Or use command line
docker-compose exec ibmmq dspmq
docker-compose exec ibmmq runmqsc QMDEMO << EOF
DISPLAY QLOCAL(DEV.QUEUE.1)
EOF

# Check consumer logs
docker-compose logs jms-consumer

# Verify queue name matches
# Producer and Consumer must use same queue name
```

## Advanced Usage

### Using TLS/SSL

Create a `docker-compose.override.yml` file:

```yaml
version: '3.8'

services:
  jms-producer:
    environment:
      - MQ_SSL_CIPHER_SUITE=TLS_RSA_WITH_AES_128_CBC_SHA256
      - MQ_SSL_CA_CERT_PATH=/etc/ssl/certs/ca.crt
    volumes:
      - ./certs/ca.crt:/etc/ssl/certs/ca.crt:ro

  jms-consumer:
    environment:
      - MQ_SSL_CIPHER_SUITE=TLS_RSA_WITH_AES_128_CBC_SHA256
      - MQ_SSL_CA_CERT_PATH=/etc/ssl/certs/ca.crt
    volumes:
      - ./certs/ca.crt:/etc/ssl/certs/ca.crt:ro
```

### Custom IBM MQ Configuration

Mount custom MQ configuration:

```yaml
services:
  ibmmq:
    volumes:
      - ./mq-config:/etc/mqm
      - qm-data:/mnt/mqm
```

### Persistent Queue Manager Data

The queue manager data is stored in a Docker volume by default:

```bash
# List volumes
docker volume ls | grep mq

# Inspect volume
docker volume inspect ibmmq-demoapps-jms_qm-data

# Backup volume
docker run --rm -v ibmmq-demoapps-jms_qm-data:/data -v $(pwd):/backup alpine tar czf /backup/qm-backup.tar.gz /data

# Restore volume
docker run --rm -v ibmmq-demoapps-jms_qm-data:/data -v $(pwd):/backup alpine tar xzf /backup/qm-backup.tar.gz -C /
```

## Performance Tuning

### For High Throughput

```yaml
services:
  ibmmq:
    environment:
      - MQ_QMGR_NAME=QMDEMO
      - MQ_APP_PASSWORD=passw0rd
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G

  jms-producer:
    environment:
      - MQ_SEND_SLEEP_MILLIS=0
      - MQ_LOG_FREQUENCY=10000
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 512M

  jms-consumer:
    environment:
      - MQ_RECEIVE_SLEEP_MILLIS=0
      - MQ_RECEIVE_TIMEOUT_MILLIS=100
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 512M
```

### For Development (Low Resources)

```yaml
services:
  ibmmq:
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 512M

  jms-producer:
    environment:
      - MQ_SEND_SLEEP_MILLIS=5000
    deploy:
      resources:
        limits:
          cpus: '0.25'
          memory: 256M

  jms-consumer:
    deploy:
      resources:
        limits:
          cpus: '0.25'
          memory: 256M
```

## Useful Commands

### Service Management

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose stop

# Restart specific service
docker-compose restart jms-producer

# Remove services and networks
docker-compose down

# Remove everything including volumes
docker-compose down -v

# View service status
docker-compose ps

# View resource usage
docker-compose stats
```

### Debugging

```bash
# Execute command in container
docker-compose exec jms-producer env | grep MQ_

# Access container shell
docker-compose exec ibmmq bash

# View real-time logs
docker-compose logs -f --tail=100

# Check service health
docker-compose ps
docker inspect ibmmq-demo | grep Health
```

### Building and Testing

```bash
# Build without cache
docker-compose build --no-cache

# Build specific service
docker-compose build jms-producer

# Pull latest images
docker-compose pull

# Validate docker-compose.yml
docker-compose config
```

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: Test with Docker Compose

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Start services
        run: docker-compose up -d
      
      - name: Wait for services
        run: |
          timeout 60 bash -c 'until docker-compose ps | grep healthy; do sleep 2; done'
      
      - name: Run tests
        run: |
          # Your test commands here
          docker-compose logs
      
      - name: Cleanup
        run: docker-compose down -v
```

## Best Practices

1. **Use Health Checks** - Ensure IBM MQ is ready before starting applications
2. **Resource Limits** - Set appropriate CPU and memory limits
3. **Logging** - Use structured logging for better debugging
4. **Volumes** - Use named volumes for persistent data
5. **Networks** - Use custom networks for service isolation
6. **Environment Variables** - Use `.env` file for sensitive data
7. **Version Control** - Don't commit `.env` files with secrets

## Cleanup

### Remove Everything

```bash
# Stop and remove containers, networks, volumes
docker-compose down -v

# Remove images
docker-compose down --rmi all

# Remove orphaned containers
docker-compose down --remove-orphans
```

### Prune Docker System

```bash
# Remove unused containers, networks, images
docker system prune -a

# Remove unused volumes
docker volume prune
```

## Next Steps

- Explore the [IBM MQ documentation](https://www.ibm.com/docs/en/ibm-mq)
- Check out other guides in the [`docs/`](.) directory
- Try different configuration scenarios
- Integrate with your application

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review Docker Compose logs
3. Consult the main [README](../README.md)
4. Check IBM MQ documentation