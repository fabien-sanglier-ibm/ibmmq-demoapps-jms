# Debug Logging Guide

This guide explains how to enable debug logging to troubleshoot SSL/TLS and IBM MQ connection issues.

## Quick Reference

### Enable SSL Debug Logging

Uncomment this in your deployment YAML:

```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.debug=ssl:handshake:verbose"
```

Then restart:
```bash
kubectl rollout restart deployment/jms-consumer
kubectl rollout restart deployment/jms-producer
```

View logs:
```bash
kubectl logs -f deployment/jms-consumer
kubectl logs -f deployment/jms-producer
```

---

## Debug Logging Options

### Option 1: SSL Handshake Debug (Recommended for Certificate Issues)

**Use when:** You have SSL/TLS certificate validation errors

**Configuration:**
```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.debug=ssl:handshake:verbose"
```

**What you'll see:**
- Certificate chain being presented by server
- Certificate validation steps
- Which certificates are trusted
- Why validation fails
- Cipher suite negotiation

**Example output:**
```
*** ClientHello, TLSv1.2
*** ServerHello, TLSv1.2
*** Certificate chain
chain [0] = [
[
  Version: V3
  Subject: CN=devnhaqmgr-ibm-mq
  Signature Algorithm: SHA256withRSA
  Key: RSA
  Validity: [From: ..., To: ...]
  Issuer: CN=My CA
  ...
]
]
*** CertificateVerify
Found trusted certificate:
[
  Subject: CN=My CA
  ...
]
```

---

### Option 2: All SSL Debug (Very Verbose)

**Use when:** You need complete SSL/TLS details

**Configuration:**
```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.debug=all"
```

**What you'll see:**
- Everything from Option 1
- Key exchange details
- Encryption/decryption operations
- Protocol version negotiation
- Session management

**Warning:** This produces A LOT of output. Use only when necessary.

---

### Option 3: Specific SSL Categories

**Use when:** You want to focus on specific aspects

**Configuration:**
```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.debug=ssl,handshake,data,trustmanager"
```

**Available categories:**
- `ssl` - General SSL/TLS information
- `handshake` - Handshake messages
- `data` - Application data
- `trustmanager` - Trust manager operations (certificate validation)
- `keymanager` - Key manager operations
- `session` - Session management
- `defaultctx` - Default SSL context
- `sslctx` - SSL context
- `sessioncache` - Session cache
- `record` - SSL record layer
- `plaintext` - Plaintext data (before encryption)

**Example combinations:**
```yaml
# Focus on certificate validation
- name: JAVA_TOOL_OPTIONS
  value: "-Djavax.net.debug=trustmanager,handshake"

# Focus on handshake and data
- name: JAVA_TOOL_OPTIONS
  value: "-Djavax.net.debug=handshake,data"

# Everything except data
- name: JAVA_TOOL_OPTIONS
  value: "-Djavax.net.debug=ssl,handshake,trustmanager,keymanager"
```

---

### Option 4: IBM MQ Trace (For MQ-Specific Issues)

**Use when:** You have IBM MQ connection or protocol issues

**Configuration:**
```yaml
env:
  - name: MQJMS_TRACE_LEVEL
    value: "9"  # 0-9, where 9 is most verbose
  - name: MQJMS_TRACE_DIR
    value: "/tmp"
```

**What you'll see:**
- IBM MQ JMS client operations
- Queue manager connection details
- Channel operations
- Message handling

**Access trace files:**
```bash
# List trace files
kubectl exec -it deployment/jms-consumer -- ls -la /tmp/*.trc

# View trace file
kubectl exec -it deployment/jms-consumer -- cat /tmp/mqjms_PID.trc

# Copy trace file locally
kubectl cp deployment/jms-consumer:/tmp/mqjms_PID.trc ./mqjms.trc
```

**Trace levels:**
- `0` - No trace
- `1` - Minimal trace
- `5` - Medium trace
- `9` - Maximum trace (very verbose)

---

## Combining Debug Options

You can combine SSL debug with MQ trace:

```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.debug=ssl:handshake:verbose"
  - name: MQJMS_TRACE_LEVEL
    value: "5"
  - name: MQJMS_TRACE_DIR
    value: "/tmp"
```

---

## Practical Examples

### Example 1: Troubleshoot Certificate Validation

**Problem:** `unable to find valid certification path to requested target`

**Enable debug:**
```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.debug=trustmanager,handshake"
```

**What to look for in logs:**
```
trustStore is: /etc/ssl/certs/ca.crt
trustStore type is: jks
init truststore
adding as trusted cert:
  Subject: CN=My CA
  Issuer:  CN=My CA
  ...

*** Certificate chain
chain [0] = [
  Subject: CN=devnhaqmgr-ibm-mq
  Issuer:  CN=Different CA  <-- MISMATCH!
]

PKIX path building failed
```

**Solution:** The CA certificate doesn't match. Get the correct CA from IBM MQ.

---

### Example 2: Troubleshoot Cipher Suite Mismatch

**Problem:** SSL handshake fails with cipher suite error

**Enable debug:**
```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.debug=ssl:handshake"
```

**What to look for in logs:**
```
*** ClientHello, TLSv1.2
Cipher Suites: [TLS_RSA_WITH_AES_128_CBC_SHA256, ...]

*** ServerHello, TLSv1.2
Cipher Suite: TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384  <-- Server chose different cipher
```

**Solution:** Update `MQ_SSL_CIPHER_SUITE` to match what the server supports.

---

### Example 3: Troubleshoot Connection Issues

**Problem:** Connection refused or timeout

**Enable debug:**
```yaml
env:
  - name: MQJMS_TRACE_LEVEL
    value: "5"
  - name: MQJMS_TRACE_DIR
    value: "/tmp"
```

**What to look for in trace:**
```
Attempting connection to: devnhaqmgr-ibm-mq:1414
Channel: IBM.APP.SVRCONN
Queue Manager: DEVNHAQMGR
Connection refused / timeout
```

**Solution:** Check MQ host, port, channel name, and queue manager name.

---

## Step-by-Step Troubleshooting Process

### Step 1: Enable SSL Debug

```bash
# Edit deployment
kubectl edit deployment jms-consumer

# Add under env:
- name: JAVA_TOOL_OPTIONS
  value: "-Djavax.net.debug=ssl:handshake:verbose"

# Or apply updated YAML
kubectl apply -f deployment-openshift/consumer-deployment.yaml
```

### Step 2: Restart Deployment

```bash
kubectl rollout restart deployment/jms-consumer
```

### Step 3: Watch Logs

```bash
kubectl logs -f deployment/jms-consumer
```

### Step 4: Analyze Output

Look for:
- Certificate chain presented by server
- Trusted certificates in your truststore
- Certificate validation errors
- Cipher suite negotiation

### Step 5: Fix Issues

Based on what you find:
- Update CA certificate if mismatch
- Update cipher suite if needed
- Check MQ configuration

### Step 6: Disable Debug

Once fixed, remove debug logging:
```bash
# Comment out JAVA_TOOL_OPTIONS
kubectl edit deployment jms-consumer

# Or apply original YAML
kubectl apply -f deployment-openshift/consumer-deployment.yaml
```

---

## Reading SSL Debug Output

### Understanding Certificate Chain

```
*** Certificate chain
chain [0] = [
  Version: V3
  Subject: CN=devnhaqmgr-ibm-mq, O=IBM, C=US
  Signature Algorithm: SHA256withRSA
  Issuer: CN=Intermediate CA, O=IBM, C=US
  Validity: [From: 2024-01-01, To: 2025-01-01]
]
chain [1] = [
  Subject: CN=Intermediate CA, O=IBM, C=US
  Issuer: CN=Root CA, O=IBM, C=US
]
chain [2] = [
  Subject: CN=Root CA, O=IBM, C=US
  Issuer: CN=Root CA, O=IBM, C=US  <-- Self-signed root
]
```

**What this means:**
- `chain [0]` is the server certificate (devnhaqmgr-ibm-mq)
- `chain [1]` is the intermediate CA
- `chain [2]` is the root CA (self-signed)

**You need:** Either the root CA or intermediate CA in your truststore

---

### Understanding Trust Validation

```
trustStore is: /etc/ssl/certs/ca.crt
adding as trusted cert:
  Subject: CN=Root CA, O=IBM, C=US
  Issuer:  CN=Root CA, O=IBM, C=US

Validating certificate chain...
Found trusted certificate:
  Subject: CN=Root CA, O=IBM, C=US
  
Certificate chain validation successful
```

**What this means:**
- Your truststore contains the Root CA
- The server's certificate chain includes this Root CA
- Validation succeeds

---

### Understanding Validation Failure

```
trustStore is: /etc/ssl/certs/ca.crt
adding as trusted cert:
  Subject: CN=Wrong CA, O=Other, C=US

Validating certificate chain...
PKIX path building failed: unable to find valid certification path
```

**What this means:**
- Your truststore contains "Wrong CA"
- The server's certificate chain doesn't include this CA
- Validation fails

**Solution:** Get the correct CA certificate from the server

---

## Performance Impact

Debug logging has performance impact:

| Debug Level | Performance Impact | Use Case |
|-------------|-------------------|----------|
| None | None | Production |
| `ssl:handshake` | Low | Troubleshooting certificates |
| `ssl:handshake:verbose` | Medium | Detailed certificate issues |
| `all` | High | Deep troubleshooting |
| MQ Trace Level 1-5 | Low-Medium | MQ connection issues |
| MQ Trace Level 9 | High | Deep MQ troubleshooting |

**Recommendation:** Enable debug only when troubleshooting, disable in production.

---

## Common Debug Scenarios

### Scenario 1: Certificate Not Trusted

**Enable:**
```yaml
- name: JAVA_TOOL_OPTIONS
  value: "-Djavax.net.debug=trustmanager"
```

**Look for:**
- Which certificates are in your truststore
- Which certificate the server presents
- Why validation fails

---

### Scenario 2: Wrong Cipher Suite

**Enable:**
```yaml
- name: JAVA_TOOL_OPTIONS
  value: "-Djavax.net.debug=ssl:handshake"
```

**Look for:**
- Client cipher suites offered
- Server cipher suite selected
- Cipher suite negotiation failure

---

### Scenario 3: Protocol Version Mismatch

**Enable:**
```yaml
- name: JAVA_TOOL_OPTIONS
  value: "-Djavax.net.debug=ssl"
```

**Look for:**
- Client protocol versions supported
- Server protocol version selected
- Protocol negotiation failure

---

## Cleanup After Debugging

Once you've fixed the issue:

1. **Remove debug environment variables**
2. **Restart deployments**
3. **Verify normal operation**
4. **Document the solution**

```bash
# Remove debug config
kubectl edit deployment jms-consumer
# Comment out JAVA_TOOL_OPTIONS

# Restart
kubectl rollout restart deployment/jms-consumer

# Verify
kubectl logs -f deployment/jms-consumer
```

---

## Summary

**For certificate issues:**
```yaml
- name: JAVA_TOOL_OPTIONS
  value: "-Djavax.net.debug=ssl:handshake:verbose"
```

**For MQ connection issues:**
```yaml
- name: MQJMS_TRACE_LEVEL
  value: "5"
- name: MQJMS_TRACE_DIR
  value: "/tmp"
```

**For both:**
```yaml
- name: JAVA_TOOL_OPTIONS
  value: "-Djavax.net.debug=ssl:handshake:verbose"
- name: MQJMS_TRACE_LEVEL
  value: "5"
- name: MQJMS_TRACE_DIR
  value: "/tmp"
```

Remember to disable debug logging after troubleshooting!