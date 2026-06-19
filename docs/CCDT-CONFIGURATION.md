# CCDT (Client Channel Definition Table) Configuration Guide

This guide explains how to use CCDT for connecting to IBM MQ uniform clusters or multi-instance queue managers with automatic failover.

## What is CCDT?

CCDT (Client Channel Definition Table) is a JSON file that defines:
- Multiple queue manager endpoints
- Connection channels
- Load balancing options
- Automatic failover configuration

**Benefits:**
- ✅ Automatic failover to backup queue managers
- ✅ Load balancing across uniform cluster members
- ✅ Centralized connection configuration
- ✅ No code changes needed for endpoint updates
- ✅ Simplified client configuration

## When to Use CCDT

### Use CCDT When:
- ✅ Connecting to a **uniform cluster** (multiple queue managers)
- ✅ Using **multi-instance queue managers** (active/standby)
- ✅ Need **automatic failover** on connection loss
- ✅ Want **load balancing** across queue managers
- ✅ Managing **multiple environments** with different endpoints

### Use Direct Connection When:
- ⚠️ Single queue manager with no failover
- ⚠️ Simple development/testing scenarios
- ⚠️ Static, unchanging connection details

## Configuration

### Environment Variables

When using CCDT, you only need to set:

```yaml
env:
  # Required: Point to CCDT file
  - name: MQ_CCDT_URL
    value: "file:///etc/mq/ccdt.json"
  
  # Optional: Override queue manager (if not set, uses value from CCDT)
  # - name: MQ_QUEUE_MANAGER
  #   value: "*"  # Use * for uniform cluster, or specific QM name
  
  # Optional: Override channel (if not set, uses value from CCDT)
  # - name: MQ_CHANNEL
  #   value: "SYSTEM.DEF.SVRCONN"
  
  # NOT NEEDED - these are defined in the CCDT file:
  # MQ_HOST - ❌ Ignored when CCDT is used
  # MQ_PORT - ❌ Ignored when CCDT is used
```

**Important:** When using CCDT, `MQ_HOST`, `MQ_PORT`, `MQ_QUEUE_MANAGER`, and `MQ_CHANNEL` are **all optional** - all connection details are read from the CCDT file. You can optionally override queue manager and channel if needed.

## CCDT File Format

### Basic CCDT (Single Queue Manager)

```json
{
  "channel": [
    {
      "name": "DEV.APP.SVRCONN",
      "clientConnection": {
        "connection": [
          {
            "host": "qm1.example.com",
            "port": 1414
          }
        ],
        "queueManager": "QM1"
      },
      "type": "clientConnection"
    }
  ]
}
```

### Uniform Cluster CCDT (Multiple Queue Managers)

```json
{
  "channel": [
    {
      "name": "SYSTEM.DEF.SVRCONN",
      "clientConnection": {
        "connection": [
          {
            "host": "qm1.example.com",
            "port": 1414
          },
          {
            "host": "qm2.example.com",
            "port": 1414
          },
          {
            "host": "qm3.example.com",
            "port": 1414
          }
        ],
        "queueManager": "*"
      },
      "transmissionSecurity": {
        "cipherSpecification": "TLS_RSA_WITH_AES_128_CBC_SHA256"
      },
      "type": "clientConnection"
    }
  ]
}
```

### Multi-Instance Queue Manager CCDT

```json
{
  "channel": [
    {
      "name": "DEV.APP.SVRCONN",
      "clientConnection": {
        "connection": [
          {
            "host": "qm-active.example.com",
            "port": 1414
          },
          {
            "host": "qm-standby.example.com",
            "port": 1414
          }
        ],
        "queueManager": "QM1"
      },
      "type": "clientConnection"
    }
  ]
}
```

### CCDT with TLS/SSL

```json
{
  "channel": [
    {
      "name": "SECURE.APP.SVRCONN",
      "clientConnection": {
        "connection": [
          {
            "host": "qm1.example.com",
            "port": 1414
          }
        ],
        "queueManager": "QM1"
      },
      "transmissionSecurity": {
        "cipherSpecification": "TLS_RSA_WITH_AES_128_CBC_SHA256",
        "certificateLabel": "ibmwebspheremq"
      },
      "type": "clientConnection"
    }
  ]
}
```

## Deployment Examples

### Kubernetes/OpenShift with ConfigMap

**1. Create CCDT ConfigMap:**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mq-ccdt
data:
  ccdt.json: |
    {
      "channel": [
        {
          "name": "SYSTEM.DEF.SVRCONN",
          "clientConnection": {
            "connection": [
              {
                "host": "qm1-ibm-mq.mq-namespace.svc.cluster.local",
                "port": 1414
              },
              {
                "host": "qm2-ibm-mq.mq-namespace.svc.cluster.local",
                "port": 1414
              },
              {
                "host": "qm3-ibm-mq.mq-namespace.svc.cluster.local",
                "port": 1414
              }
            ],
            "queueManager": "*"
          },
          "type": "clientConnection"
        }
      ]
    }
```

**2. Update Deployment:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jms-consumer
spec:
  template:
    spec:
      containers:
        - name: jms-consumer
          env:
            - name: MQ_CCDT_URL
              value: "file:///etc/mq/ccdt.json"
            - name: MQ_QUEUE_MANAGER
              value: "*"
            - name: MQ_CHANNEL
              value: "SYSTEM.DEF.SVRCONN"
          volumeMounts:
            - name: ccdt
              mountPath: /etc/mq
              readOnly: true
      volumes:
        - name: ccdt
          configMap:
            name: mq-ccdt
```

### Docker with Volume Mount

```bash
# Create CCDT file
cat > ccdt.json << 'EOF'
{
  "channel": [
    {
      "name": "DEV.APP.SVRCONN",
      "clientConnection": {
        "connection": [
          {
            "host": "qm1.example.com",
            "port": 1414
          },
          {
            "host": "qm2.example.com",
            "port": 1414
          }
        ],
        "queueManager": "*"
      },
      "type": "clientConnection"
    }
  ]
}
EOF

# Run with CCDT
docker run --rm \
  -v $(pwd)/ccdt.json:/etc/mq/ccdt.json:ro \
  -e MQ_CCDT_URL=file:///etc/mq/ccdt.json \
  -e MQ_QUEUE_MANAGER="*" \
  -e MQ_CHANNEL=DEV.APP.SVRCONN \
  -e MQ_APP_PASSWORD=your-password \
  jmsproducer:latest
```

### Docker Compose

```yaml
version: '3.8'

services:
  jms-producer:
    build: ./jmsproducer
    environment:
      - MQ_CCDT_URL=file:///etc/mq/ccdt.json
      - MQ_QUEUE_MANAGER=*
      - MQ_CHANNEL=SYSTEM.DEF.SVRCONN
      - MQ_APP_PASSWORD=passw0rd
    volumes:
      - ./ccdt.json:/etc/mq/ccdt.json:ro

  jms-consumer:
    build: ./jmsconsumer
    environment:
      - MQ_CCDT_URL=file:///etc/mq/ccdt.json
      - MQ_QUEUE_MANAGER=*
      - MQ_CHANNEL=SYSTEM.DEF.SVRCONN
      - MQ_APP_PASSWORD=passw0rd
    volumes:
      - ./ccdt.json:/etc/mq/ccdt.json:ro
```

## CCDT URL Formats

### File System

```bash
# Local file
MQ_CCDT_URL=file:///etc/mq/ccdt.json

# Relative path
MQ_CCDT_URL=file://./config/ccdt.json

# Windows
MQ_CCDT_URL=file:///C:/mq/ccdt.json
```

### HTTP/HTTPS

```bash
# HTTP server
MQ_CCDT_URL=http://config-server.example.com/ccdt.json

# HTTPS server
MQ_CCDT_URL=https://config-server.example.com/ccdt.json
```

### FTP

```bash
# FTP server
MQ_CCDT_URL=ftp://ftp.example.com/mq/ccdt.json
```

## Load Balancing Options

### Round Robin (Default)

Connections are distributed evenly across all queue managers:

```json
{
  "channel": [
    {
      "name": "SYSTEM.DEF.SVRCONN",
      "clientConnection": {
        "connection": [
          {"host": "qm1.example.com", "port": 1414},
          {"host": "qm2.example.com", "port": 1414},
          {"host": "qm3.example.com", "port": 1414}
        ],
        "queueManager": "*"
      },
      "type": "clientConnection"
    }
  ]
}
```

### Weighted Load Balancing

Assign weights to prioritize certain queue managers:

```json
{
  "channel": [
    {
      "name": "SYSTEM.DEF.SVRCONN",
      "clientConnection": {
        "connection": [
          {"host": "qm1.example.com", "port": 1414, "weight": 5},
          {"host": "qm2.example.com", "port": 1414, "weight": 3},
          {"host": "qm3.example.com", "port": 1414, "weight": 2}
        ],
        "queueManager": "*"
      },
      "type": "clientConnection"
    }
  ]
}
```

## Automatic Reconnection

The IBM MQ JMS client automatically:
- Tries all endpoints in the CCDT
- Reconnects on connection failure
- Balances load across available queue managers

**Application configuration:**
```yaml
env:
  - name: MQ_CCDT_URL
    value: "file:///etc/mq/ccdt.json"
  # Client reconnect is enabled by default in the code
```

## Generating CCDT Files

### From IBM MQ Explorer

1. Open IBM MQ Explorer
2. Right-click on Queue Manager → Properties
3. Go to "Client Connection" tab
4. Click "Export CCDT"
5. Save as JSON file

### Using runmqsc

```bash
# Define channel
echo "DEFINE CHANNEL(DEV.APP.SVRCONN) CHLTYPE(SVRCONN)" | runmqsc QM1

# Export CCDT
runmqsc QM1 << EOF
ALTER QMGR CHLAUTH(DISABLED)
REFRESH SECURITY TYPE(CONNAUTH)
EOF
```

### Manually

Create a JSON file following the format shown in the examples above.

## Troubleshooting

### Issue 1: CCDT File Not Found

**Error:** `JMSWMQ2007: Failed to send data to MQ queue manager`

**Solution:**
```bash
# Verify file exists
kubectl exec -it deployment/jms-consumer -- ls -la /etc/mq/ccdt.json

# Check file permissions
kubectl exec -it deployment/jms-consumer -- cat /etc/mq/ccdt.json

# Verify URL format
echo $MQ_CCDT_URL
```

### Issue 2: Invalid CCDT Format

**Error:** `JMSCMQ0001: IBM MQ call failed with compcode '2'`

**Solution:**
```bash
# Validate JSON
cat ccdt.json | jq .

# Check for common issues:
# - Missing commas
# - Incorrect quotes
# - Invalid field names
```

### Issue 3: Cannot Connect to Any Queue Manager

**Error:** All endpoints fail to connect

**Solution:**
```bash
# Test connectivity to each endpoint
for host in qm1 qm2 qm3; do
  nc -zv $host.example.com 1414
done

# Check queue manager status
# Verify at least one QM is running

# Enable debug logging
kubectl set env deployment/jms-consumer \
  JAVA_TOOL_OPTIONS="-Dorg.slf4j.simpleLogger.defaultLogLevel=debug"
```

### Issue 4: Wrong Queue Manager Selected

**Problem:** Connecting to unexpected queue manager

**Solution:**
```bash
# Check CCDT connection order
cat ccdt.json | jq '.channel[].clientConnection.connection'

# Verify queue manager name
# Use "*" for uniform cluster
# Use specific name for multi-instance
```

## Best Practices

1. **Use ConfigMaps/Secrets** - Store CCDT in Kubernetes ConfigMaps for easy updates
2. **Version Control** - Keep CCDT files in version control
3. **Test Failover** - Regularly test automatic failover by stopping queue managers
4. **Monitor Connections** - Track which queue manager clients connect to
5. **Update Centrally** - Update CCDT file instead of changing application config
6. **Use HTTPS** - When serving CCDT over HTTP, use HTTPS for security
7. **Validate JSON** - Always validate CCDT JSON before deployment
8. **Document Endpoints** - Keep documentation of all queue manager endpoints

## Security Considerations

1. **File Permissions** - Ensure CCDT file is readable but not writable by application
2. **TLS Configuration** - Include cipher specifications in CCDT for secure connections
3. **Credential Management** - CCDT doesn't contain credentials - use environment variables
4. **Network Security** - Ensure network policies allow connections to all endpoints
5. **Access Control** - Limit who can modify CCDT files

## Examples

### Complete Kubernetes Example

```yaml
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: mq-ccdt
  namespace: mq-apps
data:
  ccdt.json: |
    {
      "channel": [
        {
          "name": "SYSTEM.DEF.SVRCONN",
          "clientConnection": {
            "connection": [
              {"host": "qm1-ibm-mq.mq.svc.cluster.local", "port": 1414},
              {"host": "qm2-ibm-mq.mq.svc.cluster.local", "port": 1414},
              {"host": "qm3-ibm-mq.mq.svc.cluster.local", "port": 1414}
            ],
            "queueManager": "*"
          },
          "transmissionSecurity": {
            "cipherSpecification": "TLS_RSA_WITH_AES_128_CBC_SHA256"
          },
          "type": "clientConnection"
        }
      ]
    }

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jms-producer
  namespace: mq-apps
spec:
  replicas: 1
  selector:
    matchLabels:
      app: jms-producer
  template:
    metadata:
      labels:
        app: jms-producer
    spec:
      containers:
        - name: jms-producer
          image: jmsproducer:latest
          env:
            - name: MQ_CCDT_URL
              value: "file:///etc/mq/ccdt.json"
            - name: MQ_QUEUE_MANAGER
              value: "*"
            - name: MQ_CHANNEL
              value: "SYSTEM.DEF.SVRCONN"
            - name: MQ_QUEUE_NAME
              value: "DEV.QUEUE.1"
            - name: MQ_APP_USERNAME
              value: "app"
            - name: MQ_APP_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: mq-credentials
                  key: password
            - name: MQ_SSL_CIPHER_SUITE
              value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
            - name: MQ_SSL_CA_CERT_PATH
              value: "/etc/ssl/certs/ca.crt"
          volumeMounts:
            - name: ccdt
              mountPath: /etc/mq
              readOnly: true
            - name: ca-cert
              mountPath: /etc/ssl/certs
              readOnly: true
      volumes:
        - name: ccdt
          configMap:
            name: mq-ccdt
        - name: ca-cert
          secret:
            secretName: mq-ca-cert
```

## Summary

- **CCDT** enables automatic failover and load balancing
- **Use for** uniform clusters and multi-instance queue managers
- **Configure via** `MQ_CCDT_URL` environment variable
- **Store in** ConfigMaps for easy updates
- **Test** failover scenarios regularly
- **Monitor** connection distribution across queue managers

For more information, see the [IBM MQ CCDT documentation](https://www.ibm.com/docs/en/ibm-mq/latest?topic=applications-using-client-channel-definition-table).