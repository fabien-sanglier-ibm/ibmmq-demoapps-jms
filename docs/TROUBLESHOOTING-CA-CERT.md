# Troubleshooting: Unable to Find Valid Certification Path

## Error Message
```
sun.security.provider.certpath.SunCertPathBuilderException: 
unable to find valid certification path to requested target
```

## What This Means
The Java application cannot validate the MQ server's certificate because:
1. The CA certificate file is not found
2. The CA certificate is not in the correct format
3. The CA certificate doesn't match the server certificate
4. The certificate file is not being loaded by the application

## Diagnostic Steps

### Step 1: Verify the CA Certificate File Exists

```bash
# Check if the file is mounted in the pod
kubectl exec -it deployment/jms-consumer -- ls -la /etc/ssl/certs/

# Check the specific file
kubectl exec -it deployment/jms-consumer -- cat /etc/ssl/certs/ca.crt
```

**Expected output:** You should see the certificate starting with `-----BEGIN CERTIFICATE-----`

**If file not found:**
- Check the secret exists: `kubectl get secret mq-tls-certs`
- Check the key name in the secret matches your volume mount
- Verify the volume is mounted correctly

---

### Step 2: Verify the Secret Contains the CA Certificate

```bash
# List all keys in the secret
kubectl get secret mq-tls-certs -o jsonpath='{.data}' | jq 'keys'

# View the CA certificate (decode from base64)
kubectl get secret mq-tls-certs -o jsonpath='{.data.ca\.crt}' | base64 -d
```

**Common key names:**
- `ca.crt`
- `tls.crt`
- `ca-cert.pem`
- `qm.crt` (for IBM MQ)

**If the key name is different**, update your deployment:

```yaml
env:
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/YOUR-ACTUAL-KEY-NAME.crt"

volumes:
  - name: tls-certs
    secret:
      secretName: mq-tls-certs
      items:
        - key: YOUR-ACTUAL-KEY-NAME  # Must match the secret key
          path: YOUR-ACTUAL-KEY-NAME.crt
```

---

### Step 3: Check Application Logs for Certificate Loading

```bash
# View consumer logs
kubectl logs -f deployment/jms-consumer

# View producer logs
kubectl logs -f deployment/jms-producer
```

**Look for these messages:**

✅ **Success:**
```
Successfully configured CA certificate from: /etc/ssl/certs/ca.crt
SSL CA certificate path configured: /etc/ssl/certs/ca.crt
```

❌ **Failure:**
```
Failed to configure CA certificate from: /etc/ssl/certs/ca.crt
java.io.FileNotFoundException: /etc/ssl/certs/ca.crt
```

---

### Step 4: Verify Certificate Format

The CA certificate must be in PEM format (X.509):

```bash
# Check certificate format
kubectl exec -it deployment/jms-consumer -- openssl x509 -in /etc/ssl/certs/ca.crt -text -noout
```

**Expected output:** Certificate details including Subject, Issuer, Validity dates

**If error:** The file is not a valid PEM certificate

---

### Step 5: Verify the CA Certificate Matches MQ Server Certificate

```bash
# Get the MQ server's certificate
openssl s_client -connect qmdemo-ibm-mq:1414 -showcerts

# Compare the issuer with your CA certificate
kubectl exec -it deployment/jms-consumer -- openssl x509 -in /etc/ssl/certs/ca.crt -noout -subject -issuer
```

The CA certificate should be the issuer of the MQ server certificate.

---

## Common Issues and Solutions

### Issue 1: Wrong Secret Key Name

**Problem:** Secret key is `tls.crt` but deployment expects `ca.crt`

**Solution:** Update deployment to match actual key name:

```yaml
env:
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/tls.crt"  # Changed from ca.crt

volumes:
  - name: tls-certs
    secret:
      secretName: mq-tls-certs
      items:
        - key: tls.crt  # Changed from ca.crt
          path: tls.crt
```

---

### Issue 2: Certificate Chain Issue

**Problem:** MQ server uses an intermediate CA, but you only have the root CA

**Solution:** Get the full certificate chain:

```bash
# Get full chain from MQ server
openssl s_client -connect qmdemo-ibm-mq:1414 -showcerts > fullchain.pem

# Extract all certificates
# Create a file with both root and intermediate CAs
cat intermediate-ca.crt root-ca.crt > ca-chain.crt

# Update secret
kubectl create secret generic mq-tls-certs \
  --from-file=ca.crt=ca-chain.crt \
  --dry-run=client -o yaml | kubectl apply -f -
```

---

### Issue 3: IBM MQ Specific Certificate Location

**Problem:** IBM MQ Operator stores CA cert with different key name

**Solution:** Check IBM MQ secret structure:

```bash
# For IBM MQ Operator, the CA might be in the queue manager secret
kubectl get secret qmdemo-ibm-mq-tls -o jsonpath='{.data}' | jq 'keys'

# Common IBM MQ key names:
# - qm.crt (queue manager certificate)
# - ca.crt (CA certificate)
# - tls.crt (TLS certificate)
```

Update your deployment to use the correct secret and key:

```yaml
volumes:
  - name: tls-certs
    secret:
      secretName: qmdemo-ibm-mq-tls  # IBM MQ secret name
      items:
        - key: ca.crt  # or qm.crt, or tls.crt
          path: ca.crt
```

---

### Issue 4: Certificate Not Loaded by Application

**Problem:** File exists but application doesn't load it

**Solution:** Check if `MQ_SSL_CA_CERT_PATH` is being read:

```bash
# Check environment variables in pod
kubectl exec -it deployment/jms-consumer -- env | grep MQ_SSL

# Should show:
# MQ_SSL_CIPHER_SUITE=TLS_RSA_WITH_AES_128_CBC_SHA256
# MQ_SSL_CA_CERT_PATH=/etc/ssl/certs/ca.crt
```

If not set, check your deployment YAML is applied correctly:

```bash
kubectl get deployment jms-consumer -o yaml | grep -A 5 MQ_SSL
```

---

### Issue 5: Using Wrong CA Certificate

**Problem:** You have the CA certificate, but it's not the one that signed the MQ server certificate

**Solution:** Get the correct CA from IBM MQ:

```bash
# Option A: Extract from IBM MQ pod
kubectl exec -it qmdemo-ibm-mq-0 -- cat /run/runmqserver/tls/ca.crt > mq-ca.crt

# Option B: Get from IBM MQ secret
kubectl get secret qmdemo-ibm-mq-tls -o jsonpath='{.data.ca\.crt}' | base64 -d > mq-ca.crt

# Update your secret with the correct CA
kubectl create secret generic mq-tls-certs \
  --from-file=ca.crt=mq-ca.crt \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart pods to pick up new secret
kubectl rollout restart deployment/jms-consumer
kubectl rollout restart deployment/jms-producer
```

---

## Quick Fix Checklist

Run through this checklist:

- [ ] Secret `mq-tls-certs` exists: `kubectl get secret mq-tls-certs`
- [ ] Secret contains CA certificate: `kubectl get secret mq-tls-certs -o jsonpath='{.data}' | jq 'keys'`
- [ ] Key name in secret matches deployment: Check both match (e.g., `ca.crt`)
- [ ] Volume is mounted: `kubectl describe pod <pod-name> | grep -A 5 Mounts`
- [ ] File exists in pod: `kubectl exec -it deployment/jms-consumer -- ls -la /etc/ssl/certs/ca.crt`
- [ ] File is valid PEM: `kubectl exec -it deployment/jms-consumer -- openssl x509 -in /etc/ssl/certs/ca.crt -text -noout`
- [ ] Environment variable is set: `kubectl exec -it deployment/jms-consumer -- env | grep MQ_SSL_CA_CERT_PATH`
- [ ] Application logs show certificate loaded: `kubectl logs deployment/jms-consumer | grep "Successfully configured CA certificate"`
- [ ] CA certificate matches MQ server: Compare issuer/subject

---

## Example: Complete Working Configuration

### 1. Get CA Certificate from IBM MQ

```bash
# Get the CA certificate from IBM MQ
kubectl get secret qmdemo-ibm-mq-tls -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt

# Verify it's valid
openssl x509 -in ca.crt -text -noout
```

### 2. Create/Update Your Secret

```bash
kubectl create secret generic mq-tls-certs \
  --from-file=ca.crt=ca.crt \
  --dry-run=client -o yaml | kubectl apply -f -
```

### 3. Verify Deployment Configuration

```yaml
env:
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/ca.crt"

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

### 4. Apply and Restart

```bash
kubectl apply -f consumer-deployment.yaml
kubectl apply -f producer-deployment.yaml

# Force restart to pick up changes
kubectl rollout restart deployment/jms-consumer
kubectl rollout restart deployment/jms-producer
```

### 5. Verify Success

```bash
# Check logs for success message
kubectl logs -f deployment/jms-consumer | grep -i "certificate\|ssl\|tls"

# Should see:
# Successfully configured CA certificate from: /etc/ssl/certs/ca.crt
# SSL CA certificate path configured: /etc/ssl/certs/ca.crt
# MQ connection created successfully
```

---

## Still Not Working?

### Enable SSL Debug Logging

Add this to your deployment to see detailed SSL handshake:

```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-Djavax.net.debug=ssl:handshake:verbose"
```

This will show:
- Certificate chain being validated
- Which certificates are trusted
- Why validation fails

### Check MQ Server Configuration

```bash
# Verify MQ is configured for TLS
kubectl exec -it qmdemo-ibm-mq-0 -- dspmqchl -m QMDEMO -c DEV.APP.SVRCONN.0TLS

# Should show:
# SSLCIPH(TLS_RSA_WITH_AES_128_CBC_SHA256)
# SSLCAUTH(OPTIONAL) or SSLCAUTH(REQUIRED)
```

### Test Connection Without TLS First

Temporarily test without TLS to isolate the issue:

```yaml
env:
  - name: MQ_CHANNEL
    value: "DEV.APP.SVRCONN"  # Non-TLS channel
  # Comment out SSL configuration
  # - name: MQ_SSL_CIPHER_SUITE
  #   value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
```

If this works, the issue is definitely with the TLS/certificate configuration.

---

## Need More Help?

Provide these details:
1. Output of: `kubectl get secret mq-tls-certs -o jsonpath='{.data}' | jq 'keys'`
2. Output of: `kubectl exec -it deployment/jms-consumer -- ls -la /etc/ssl/certs/`
3. Output of: `kubectl logs deployment/jms-consumer | grep -i "certificate\|ssl\|error"`
4. Your deployment YAML (env and volumes sections)