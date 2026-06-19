# Java SSL Properties Configuration Guide

This guide explains the different ways to configure Java SSL/TLS properties for IBM MQ JMS connections.

## Three Approaches to Configure SSL/TLS

### Approach 1: Application Environment Variables (Current Default)

**How it works:** Application reads custom env vars and configures SSL programmatically

**Configuration in deployment YAML:**
```yaml
env:
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/ca.crt"
  - name: MQ_SSL_KEYSTORE_PATH
    value: "/etc/ssl/certs/client-keystore.jks"
  - name: MQ_SSL_KEYSTORE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: mq-tls-certs
        key: keystore-password
```

**Pros:**
- ✅ Application-level control
- ✅ Can use PEM certificates directly
- ✅ Passwords from secrets
- ✅ Flexible configuration

**Cons:**
- ❌ Requires application code support
- ❌ Custom environment variables

---

### Approach 2: Java System Properties via JAVA_TOOL_OPTIONS (Alternative)

**How it works:** JVM automatically sets system properties before application starts

**Configuration in deployment YAML:**
```yaml
env:
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.ssl.trustStore=/etc/ssl/certs/truststore.jks -Djavax.net.ssl.trustStorePassword=changeit"
```

**For mTLS, add keystore properties:**
```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.ssl.trustStore=/etc/ssl/certs/truststore.jks -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.keyStore=/etc/ssl/certs/client-keystore.jks -Djavax.net.ssl.keyStorePassword=changeit"
```

**Pros:**
- ✅ Standard Java approach
- ✅ Works with any Java application
- ✅ No code changes needed
- ✅ JVM-level configuration

**Cons:**
- ❌ Passwords visible in env vars (use secrets carefully)
- ❌ Requires JKS files (can't use PEM directly)
- ❌ Less flexible than application code

---

### Approach 3: Using Secrets for Passwords with JAVA_TOOL_OPTIONS

**How it works:** Combine JAVA_TOOL_OPTIONS with secret references

**Configuration in deployment YAML:**
```yaml
env:
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: TRUSTSTORE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: mq-tls-certs
        key: truststore-password
  - name: KEYSTORE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: mq-tls-certs
        key: keystore-password
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.ssl.trustStore=/etc/ssl/certs/truststore.jks -Djavax.net.ssl.trustStorePassword=$(TRUSTSTORE_PASSWORD) -Djavax.net.ssl.keyStore=/etc/ssl/certs/client-keystore.jks -Djavax.net.ssl.keyStorePassword=$(KEYSTORE_PASSWORD)"
```

**Note:** Environment variable substitution `$(VAR)` works in Kubernetes but the password is still visible in the process environment.

**Pros:**
- ✅ Passwords from secrets
- ✅ Standard Java approach
- ✅ No code changes needed

**Cons:**
- ❌ Passwords still visible in process env
- ❌ Requires JKS files
- ❌ More complex configuration

---

## Comparison Table

| Feature | App Env Vars | JAVA_TOOL_OPTIONS | JAVA_TOOL_OPTIONS + Secrets |
|---------|--------------|-------------------|----------------------------|
| PEM Certificate Support | ✅ Yes | ❌ No (JKS only) | ❌ No (JKS only) |
| Password Security | ✅ Good (in code) | ⚠️ Visible in env | ⚠️ Visible in process |
| Code Changes Required | ✅ Yes (already done) | ❌ No | ❌ No |
| Flexibility | ✅ High | ⚠️ Medium | ⚠️ Medium |
| Standard Java | ⚠️ Custom | ✅ Yes | ✅ Yes |
| Complexity | ⚠️ Medium | ✅ Low | ⚠️ Medium |

---

## When to Use Each Approach

### Use Approach 1 (Application Env Vars) When:
- ✅ You want to use PEM certificates directly (no JKS conversion)
- ✅ You want application-level control
- ✅ You prefer cleaner secret management
- ✅ **This is the current default and recommended approach**

### Use Approach 2 (JAVA_TOOL_OPTIONS) When:
- ✅ You want standard Java SSL configuration
- ✅ You don't want to modify application code
- ✅ You already have JKS truststore files
- ✅ Password visibility in env vars is acceptable

### Use Approach 3 (JAVA_TOOL_OPTIONS + Secrets) When:
- ✅ You want standard Java SSL configuration
- ✅ You want passwords from secrets
- ✅ You understand passwords are still visible in process env
- ✅ You already have JKS truststore files

---

## Examples

### Example 1: Current Configuration (Approach 1)

**Deployment YAML:**
```yaml
env:
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/ca.crt"
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "true"

volumeMounts:
  - name: tls-certs
    mountPath: /etc/ssl/certs
    readOnly: true

volumes:
  - name: tls-certs
    secret:
      secretName: mq-tls-certs
      items:
        - key: ca.crt
          path: ca.crt
```

**What happens:**
1. Application reads `MQ_SSL_CA_CERT_PATH`
2. Loads PEM certificate from `/etc/ssl/certs/ca.crt`
3. Creates in-memory truststore
4. Configures SSL context

---

### Example 2: Using JAVA_TOOL_OPTIONS (Approach 2)

**Deployment YAML:**
```yaml
env:
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.ssl.trustStore=/etc/ssl/certs/truststore.jks -Djavax.net.ssl.trustStorePassword=changeit"

volumeMounts:
  - name: tls-certs
    mountPath: /etc/ssl/certs
    readOnly: true

volumes:
  - name: tls-certs
    secret:
      secretName: mq-tls-certs
      items:
        - key: truststore.jks
          path: truststore.jks
```

**What happens:**
1. JVM sets system properties before app starts
2. `javax.net.ssl.trustStore` points to JKS file
3. `javax.net.ssl.trustStorePassword` provides password
4. All Java SSL connections use this truststore

---

### Example 3: JAVA_TOOL_OPTIONS with Secret References (Approach 3)

**Deployment YAML:**
```yaml
env:
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: TRUSTSTORE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: mq-tls-certs
        key: truststore-password
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.ssl.trustStore=/etc/ssl/certs/truststore.jks -Djavax.net.ssl.trustStorePassword=$(TRUSTSTORE_PASSWORD)"

volumeMounts:
  - name: tls-certs
    mountPath: /etc/ssl/certs
    readOnly: true

volumes:
  - name: tls-certs
    secret:
      secretName: mq-tls-certs
      items:
        - key: truststore.jks
          path: truststore.jks
```

**What happens:**
1. Kubernetes injects `TRUSTSTORE_PASSWORD` from secret
2. Environment variable substitution replaces `$(TRUSTSTORE_PASSWORD)`
3. JVM sets system properties with actual password
4. All Java SSL connections use this truststore

---

## Available Java SSL System Properties

### Truststore Properties
```
-Djavax.net.ssl.trustStore=/path/to/truststore.jks
-Djavax.net.ssl.trustStorePassword=password
-Djavax.net.ssl.trustStoreType=JKS
```

### Keystore Properties (for mTLS)
```
-Djavax.net.ssl.keyStore=/path/to/keystore.jks
-Djavax.net.ssl.keyStorePassword=password
-Djavax.net.ssl.keyStoreType=JKS
```

### Debug Properties (for troubleshooting)
```
-Djavax.net.debug=ssl:handshake:verbose
```

---

## Switching from Approach 1 to Approach 2

If you want to use `JAVA_TOOL_OPTIONS` instead of the current application env vars:

1. **Comment out** the application-specific env vars:
   ```yaml
   # - name: MQ_SSL_CA_CERT_PATH
   #   value: "/etc/ssl/certs/ca.crt"
   ```

2. **Add** JAVA_TOOL_OPTIONS:
   ```yaml
   - name: JAVA_TOOL_OPTIONS
     value: "-Djavax.net.ssl.trustStore=/etc/ssl/certs/truststore.jks -Djavax.net.ssl.trustStorePassword=changeit"
   ```

3. **Update** volume mount to use JKS file:
   ```yaml
   items:
     - key: truststore.jks
       path: truststore.jks
   ```

4. **Keep** MQ_SSL_CIPHER_SUITE (still needed by application):
   ```yaml
   - name: MQ_SSL_CIPHER_SUITE
     value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
   ```

---

## Security Best Practices

1. **Always use secrets** for passwords when possible
2. **Use read-only volume mounts** for certificates
3. **Enable peer name verification** in production
4. **Rotate certificates regularly**
5. **Use strong cipher suites**
6. **Monitor certificate expiration**

---

## Troubleshooting

### Check if JAVA_TOOL_OPTIONS is applied

```bash
kubectl exec -it deployment/jms-consumer -- env | grep JAVA_TOOL_OPTIONS
```

### View JVM startup with debug

```yaml
- name: JAVA_TOOL_OPTIONS
  value: "-Djavax.net.ssl.trustStore=/etc/ssl/certs/truststore.jks -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.debug=ssl:handshake"
```

### Verify truststore is accessible

```bash
kubectl exec -it deployment/jms-consumer -- ls -la /etc/ssl/certs/
kubectl exec -it deployment/jms-consumer -- keytool -list -keystore /etc/ssl/certs/truststore.jks -storepass changeit
```

---

## Summary

- **Current setup uses Approach 1** (Application Env Vars with PEM certificates)
- **JAVA_TOOL_OPTIONS (Approach 2)** is available as an alternative for standard Java SSL
- **Both approaches work** - choose based on your requirements
- **Approach 1 is recommended** for this application as it supports PEM certificates directly