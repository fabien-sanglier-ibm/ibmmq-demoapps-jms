# Secure Password Configuration Options

This guide explains how to avoid storing truststore/keystore passwords in clear text.

## Problem

When using `JAVA_TOOL_OPTIONS` with `-Djavax.net.ssl.trustStorePassword`, the password is visible in:
- Environment variables
- Process listings
- Container inspect output
- Kubernetes pod specs

## Solutions (Ranked by Security)

---

## ✅ Solution 1: Use CA Certificate Directly (RECOMMENDED - Current Setup)

**Security Level:** ⭐⭐⭐⭐⭐ (Best)

**How it works:** Load PEM certificate directly, no password needed

**Configuration:**
```yaml
env:
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/ca.crt"

volumes:
  - name: tls-certs
    secret:
      secretName: mq-tls-certs
      items:
        - key: ca.crt
          path: ca.crt
```

**Why this is best:**
- ✅ No password needed at all
- ✅ PEM certificates don't require passwords
- ✅ Application code handles certificate loading
- ✅ **This is your current configuration**

**Limitations:**
- Requires application code support (already implemented)

---

## ✅ Solution 2: Use Password-less JKS Truststore

**Security Level:** ⭐⭐⭐⭐ (Excellent)

**How it works:** Create JKS truststore without a password

**Create password-less truststore:**
```bash
# Import CA certificate without password
keytool -import -trustcacerts -noprompt \
  -alias ca-cert \
  -file ca.crt \
  -keystore truststore.jks \
  -storepass "" \
  -storetype JKS

# Or use existing truststore and remove password
keytool -storepasswd -new "" -keystore truststore.jks -storepass oldpassword
```

**Configuration:**
```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.ssl.trustStore=/etc/ssl/certs/truststore.jks"
  # No trustStorePassword needed!

volumes:
  - name: tls-certs
    secret:
      secretName: mq-tls-certs
      items:
        - key: truststore.jks
          path: truststore.jks
```

**Why this works:**
- ✅ No password in environment
- ✅ Standard Java approach
- ✅ Truststore only contains public certificates (no private keys)
- ✅ File permissions protect the truststore

**Limitations:**
- Only works for truststore (not keystore with private keys)
- Some security policies may require passwords even for truststores

---

## ⚠️ Solution 3: Password from Kubernetes Secret (Better than Clear Text)

**Security Level:** ⭐⭐⭐ (Good, but password still visible in process)

**How it works:** Reference secret in environment variable

**Configuration:**
```yaml
env:
  - name: TRUSTSTORE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: mq-tls-certs
        key: truststore-password
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.ssl.trustStore=/etc/ssl/certs/truststore.jks -Djavax.net.ssl.trustStorePassword=$(TRUSTSTORE_PASSWORD)"
```

**Why this is better:**
- ✅ Password not in YAML files
- ✅ Password stored in Kubernetes secret
- ✅ RBAC controls access to secret

**Limitations:**
- ⚠️ Password still visible in process environment
- ⚠️ Can be seen with `kubectl exec` or `docker inspect`

---

## ⚠️ Solution 4: Mount Password as File

**Security Level:** ⭐⭐⭐ (Good, requires custom script)

**How it works:** Mount password as file, read in startup script

**Configuration:**
```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.ssl.trustStore=/etc/ssl/certs/truststore.jks -Djavax.net.ssl.trustStorePassword=$(cat /etc/ssl/secrets/truststore-password)"

volumeMounts:
  - name: tls-certs
    mountPath: /etc/ssl/certs
    readOnly: true
  - name: tls-passwords
    mountPath: /etc/ssl/secrets
    readOnly: true

volumes:
  - name: tls-certs
    secret:
      secretName: mq-tls-certs
      items:
        - key: truststore.jks
          path: truststore.jks
  - name: tls-passwords
    secret:
      secretName: mq-tls-certs
      items:
        - key: truststore-password
          path: truststore-password
```

**Why this is better:**
- ✅ Password not in environment variables
- ✅ File permissions protect password

**Limitations:**
- ⚠️ Requires shell evaluation
- ⚠️ More complex configuration
- ⚠️ Password still ends up in JVM system properties

---

## ❌ Solution 5: External Secret Management (Most Complex)

**Security Level:** ⭐⭐⭐⭐⭐ (Best, but complex)

**How it works:** Use external secret manager (Vault, AWS Secrets Manager, etc.)

**Example with HashiCorp Vault:**
```yaml
annotations:
  vault.hashicorp.com/agent-inject: "true"
  vault.hashicorp.com/role: "mq-client"
  vault.hashicorp.com/agent-inject-secret-truststore-password: "secret/data/mq/truststore"
  vault.hashicorp.com/agent-inject-template-truststore-password: |
    {{- with secret "secret/data/mq/truststore" -}}
    {{ .Data.data.password }}
    {{- end }}
```

**Why this is best:**
- ✅ Centralized secret management
- ✅ Audit logging
- ✅ Secret rotation
- ✅ Fine-grained access control

**Limitations:**
- ❌ Requires external infrastructure
- ❌ Complex setup
- ❌ Additional dependencies

---

## Comparison Table

| Solution | Security | Complexity | Password Visible | Recommended |
|----------|----------|------------|------------------|-------------|
| CA Cert Direct (PEM) | ⭐⭐⭐⭐⭐ | Low | No password needed | ✅ **YES** |
| Password-less JKS | ⭐⭐⭐⭐ | Low | No password needed | ✅ Yes |
| Secret Reference | ⭐⭐⭐ | Low | In process env | ⚠️ OK |
| Password File | ⭐⭐⭐ | Medium | In file system | ⚠️ OK |
| External Secret Mgr | ⭐⭐⭐⭐⭐ | High | No | ⚠️ If needed |

---

## Recommended Approach for Your Setup

### For Truststore (Server Certificate Validation)

**Option A: Use Current Setup (Best)**
```yaml
# Already configured - uses PEM certificate directly
env:
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/ca.crt"
```
✅ No password needed  
✅ Already implemented  
✅ Most secure

**Option B: Use Password-less JKS**
```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.ssl.trustStore=/etc/ssl/certs/truststore.jks"
```
✅ No password needed  
✅ Standard Java approach

### For Keystore (Client Certificate - mTLS)

**Challenge:** Keystores with private keys typically require passwords

**Best Option: Use Application Code (Current Setup)**
```yaml
env:
  - name: MQ_SSL_KEYSTORE_PATH
    value: "/etc/ssl/certs/client-keystore.jks"
  - name: MQ_SSL_KEYSTORE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: mq-tls-certs
        key: keystore-password
```

**Why this is better than JAVA_TOOL_OPTIONS:**
- ✅ Password read by application code
- ✅ Not visible in JAVA_TOOL_OPTIONS
- ✅ Used only when needed
- ✅ Can be cleared from memory after use

---

## Implementation Examples

### Example 1: Current Setup (Recommended)

**Deployment YAML:**
```yaml
env:
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/ca.crt"
  # For mTLS, password from secret (not in JAVA_TOOL_OPTIONS)
  - name: MQ_SSL_KEYSTORE_PATH
    value: "/etc/ssl/certs/client-keystore.jks"
  - name: MQ_SSL_KEYSTORE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: mq-tls-certs
        key: keystore-password
```

**Security Benefits:**
- ✅ No truststore password needed (PEM certificate)
- ✅ Keystore password from secret
- ✅ Application code handles passwords securely
- ✅ Passwords not in environment variables visible to all processes

---

### Example 2: Password-less JKS Truststore

**Create the truststore:**
```bash
# Extract CA cert from your MQ cluster
kubectl get secret mq-tls-certs -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt

# Create password-less truststore
keytool -import -trustcacerts -noprompt \
  -alias mq-ca \
  -file ca.crt \
  -keystore truststore.jks \
  -storepass "" \
  -storetype JKS

# Update secret with new truststore
kubectl create secret generic mq-tls-certs \
  --from-file=truststore.jks=truststore.jks \
  --dry-run=client -o yaml | kubectl apply -f -
```

**Deployment YAML:**
```yaml
env:
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.ssl.trustStore=/etc/ssl/certs/truststore.jks"
  # No password needed!

volumes:
  - name: tls-certs
    secret:
      secretName: mq-tls-certs
      items:
        - key: truststore.jks
          path: truststore.jks
```

---

## Security Best Practices

1. **Prefer no password** when possible (PEM certs, password-less JKS)
2. **Use application code** for password handling (current setup)
3. **Avoid JAVA_TOOL_OPTIONS** for passwords (visible in process)
4. **Use Kubernetes secrets** for any passwords needed
5. **Limit secret access** with RBAC
6. **Rotate certificates** regularly
7. **Monitor secret access** with audit logs
8. **Use read-only mounts** for all certificate volumes

---

## Why Current Setup is Best

Your current configuration using `MQ_SSL_CA_CERT_PATH` is the most secure because:

1. **No truststore password needed** - PEM certificates don't require passwords
2. **Application-level control** - Passwords handled in code, not environment
3. **Keystore password from secret** - When mTLS is needed
4. **Not visible in JAVA_TOOL_OPTIONS** - Cleaner process environment
5. **Flexible** - Can use different approaches for different certificates

---

## Summary

**For your use case:**

✅ **Keep current setup** - It's already the most secure option  
✅ **No changes needed** - CA certificate approach avoids password issues  
✅ **For mTLS** - Keystore password from secret is handled securely by application code  

**If you must use JAVA_TOOL_OPTIONS:**

✅ **Use password-less JKS** for truststore (no password needed)  
⚠️ **Accept password visibility** for keystore (or use application code instead)  

The current implementation is already following security best practices!