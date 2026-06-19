# SSL/TLS Hostname Verification Configuration

This guide explains the hostname verification options available for IBM MQ SSL/TLS connections.

## Overview

There are two separate verification mechanisms for SSL/TLS connections to IBM MQ:

1. **Peer Name Verification** (`MQ_SSL_PEER_NAME_ENABLED`) - IBM MQ specific
2. **Hostname Verification** (`MQ_SSL_HOSTNAME_VERIFICATION_ENABLED`) - Standard SSL/TLS

Both are enabled by default for security, but can be disabled for testing or specific scenarios.

---

## Configuration Options

### Option 1: Peer Name Verification (IBM MQ Specific)

**Environment Variable:** `MQ_SSL_PEER_NAME_ENABLED`

**Default:** `true`

**What it does:**
- Verifies the Distinguished Name (DN) in the server certificate
- Uses IBM MQ's `setSSLPeerName()` method
- When disabled, sets peer name to `*` (accept any)

**Configuration:**
```yaml
env:
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "true"  # Enable peer name verification
```

**Disable for testing:**
```yaml
env:
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "false"  # Disable peer name verification
```

---

### Option 2: Hostname Verification (Standard SSL/TLS)

**Environment Variable:** `MQ_SSL_HOSTNAME_VERIFICATION_ENABLED`

**Default:** `true`

**What it does:**
- Verifies the hostname matches the certificate's Common Name (CN) or Subject Alternative Name (SAN)
- Uses IBM MQ's `setTargetClientMatching()` method
- Standard SSL/TLS hostname verification

**Configuration:**
```yaml
env:
  - name: MQ_SSL_HOSTNAME_VERIFICATION_ENABLED
    value: "true"  # Enable hostname verification
```

**Disable for testing:**
```yaml
env:
  - name: MQ_SSL_HOSTNAME_VERIFICATION_ENABLED
    value: "false"  # Disable hostname verification
```

---

## How They Work Together

### Both Enabled (Default - Most Secure)

```yaml
env:
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "true"
  - name: MQ_SSL_HOSTNAME_VERIFICATION_ENABLED
    value: "true"
```

**Behavior:**
- ✅ Verifies certificate DN (peer name)
- ✅ Verifies hostname matches certificate CN/SAN
- ✅ Most secure configuration
- ✅ Recommended for production

**Use when:**
- Production environments
- Security is critical
- Certificates are properly configured with correct hostnames

---

### Peer Name Disabled, Hostname Enabled

```yaml
env:
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "false"
  - name: MQ_SSL_HOSTNAME_VERIFICATION_ENABLED
    value: "true"
```

**Behavior:**
- ❌ Does not verify certificate DN
- ✅ Verifies hostname matches certificate CN/SAN
- ⚠️ Less secure than both enabled

**Use when:**
- Certificate DN doesn't match expected pattern
- Still want hostname verification
- Testing with self-signed certificates

---

### Peer Name Enabled, Hostname Disabled

```yaml
env:
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "true"
  - name: MQ_SSL_HOSTNAME_VERIFICATION_ENABLED
    value: "false"
```

**Behavior:**
- ✅ Verifies certificate DN (peer name)
- ❌ Does not verify hostname matches certificate CN/SAN
- ⚠️ Less secure than both enabled

**Use when:**
- Certificate hostname doesn't match connection hostname
- Using IP addresses instead of hostnames
- Load balancer or proxy scenarios

---

### Both Disabled (Least Secure)

```yaml
env:
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "false"
  - name: MQ_SSL_HOSTNAME_VERIFICATION_ENABLED
    value: "false"
```

**Behavior:**
- ❌ Does not verify certificate DN
- ❌ Does not verify hostname
- ⚠️ Least secure - only verifies certificate is signed by trusted CA
- ⚠️ Vulnerable to man-in-the-middle attacks

**Use when:**
- Development/testing only
- Troubleshooting certificate issues
- **NEVER use in production**

---

## Common Scenarios

### Scenario 1: Production with Proper Certificates

**Configuration:**
```yaml
env:
  - name: MQ_HOST
    value: "qmdemo-ibm-mq.example.com"
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/ca.crt"
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "true"
  - name: MQ_SSL_HOSTNAME_VERIFICATION_ENABLED
    value: "true"
```

**Certificate Requirements:**
- CN or SAN must include `qmdemo-ibm-mq.example.com`
- Certificate must be signed by the CA in `ca.crt`
- DN must match expected pattern

---

### Scenario 2: Testing with Self-Signed Certificate

**Configuration:**
```yaml
env:
  - name: MQ_HOST
    value: "qmdemo-ibm-mq"
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/ca.crt"
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "false"
  - name: MQ_SSL_HOSTNAME_VERIFICATION_ENABLED
    value: "false"
```

**Why disable both:**
- Self-signed certificates often don't have proper CN/SAN
- DN may not match expected pattern
- Only for testing - not production

---

### Scenario 3: Connecting via IP Address

**Configuration:**
```yaml
env:
  - name: MQ_HOST
    value: "172.30.227.21"
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/ca.crt"
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "true"
  - name: MQ_SSL_HOSTNAME_VERIFICATION_ENABLED
    value: "false"  # Disable because using IP
```

**Why disable hostname verification:**
- Certificate CN/SAN contains hostname, not IP
- Hostname verification would fail
- Peer name verification still provides some security

---

### Scenario 4: Behind Load Balancer

**Configuration:**
```yaml
env:
  - name: MQ_HOST
    value: "mq-loadbalancer.example.com"
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/ca.crt"
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "true"
  - name: MQ_SSL_HOSTNAME_VERIFICATION_ENABLED
    value: "false"  # Disable if backend hostname differs
```

**Why disable hostname verification:**
- Load balancer hostname differs from backend MQ hostname
- Certificate is for backend, not load balancer
- Consider using load balancer's certificate instead

---

## Troubleshooting

### Error: Certificate hostname mismatch

**Error message:**
```
javax.net.ssl.SSLPeerUnverifiedException: Certificate for <hostname> doesn't match any of the subject alternative names
```

**Solution 1: Fix the certificate**
- Add correct hostname to certificate SAN
- Regenerate certificate with proper CN/SAN

**Solution 2: Disable hostname verification (testing only)**
```yaml
- name: MQ_SSL_HOSTNAME_VERIFICATION_ENABLED
  value: "false"
```

---

### Error: Peer name verification failed

**Error message:**
```
JMSWMQ2013: The security authentication was not valid that was supplied for QueueManager
```

**Solution 1: Check peer name configuration**
- Verify certificate DN matches expected pattern

**Solution 2: Disable peer name verification (testing only)**
```yaml
- name: MQ_SSL_PEER_NAME_ENABLED
  value: "false"
```

---

## Security Best Practices

### Production Environments

✅ **DO:**
- Enable both peer name and hostname verification
- Use certificates with proper CN/SAN
- Use fully qualified domain names (FQDN)
- Regularly rotate certificates
- Monitor certificate expiration

❌ **DON'T:**
- Disable verification in production
- Use self-signed certificates
- Connect via IP addresses
- Ignore certificate warnings

### Testing Environments

✅ **DO:**
- Document why verification is disabled
- Re-enable verification before production
- Use proper certificates when possible
- Test with verification enabled

❌ **DON'T:**
- Leave verification disabled permanently
- Copy testing config to production
- Ignore certificate issues

---

## Configuration Matrix

| Scenario | Peer Name | Hostname | Security Level | Use Case |
|----------|-----------|----------|----------------|----------|
| Production | ✅ true | ✅ true | ⭐⭐⭐⭐⭐ | Production with proper certs |
| Testing | ❌ false | ❌ false | ⭐ | Development/testing only |
| IP Address | ✅ true | ❌ false | ⭐⭐⭐ | Connecting via IP |
| Load Balancer | ✅ true | ❌ false | ⭐⭐⭐ | Behind load balancer |
| Self-Signed | ❌ false | ❌ false | ⭐ | Testing with self-signed |

---

## Log Messages

### When Both Enabled

```
SSL hostname verification enabled (certificate CN/SAN will be validated)
TLS configuration applied (peerNameEnabled=true, hostnameVerificationEnabled=true)
```

### When Hostname Disabled

```
SSL hostname verification disabled - certificate CN/SAN will not be validated - not recommended for production
TLS configuration applied (peerNameEnabled=true, hostnameVerificationEnabled=false)
```

### When Peer Name Disabled

```
SSL peer name verification disabled - not recommended for production
TLS configuration applied (peerNameEnabled=false, hostnameVerificationEnabled=true)
```

### When Both Disabled

```
SSL peer name verification disabled - not recommended for production
SSL hostname verification disabled - certificate CN/SAN will not be validated - not recommended for production
TLS configuration applied (peerNameEnabled=false, hostnameVerificationEnabled=false)
```

---

## Summary

- **Both enabled** = Most secure (production)
- **Hostname disabled** = Use when hostname doesn't match certificate
- **Peer name disabled** = Use when DN doesn't match expected pattern
- **Both disabled** = Testing only, never production

Always prefer enabling both verification mechanisms in production environments.