# Quick Start: Using CA Certificate from MQ Secret

This guide shows you how to quickly configure your deployments to use the CA certificate directly from your MQ cluster secret.

## What Changed

Both [`consumer-deployment.yaml`](consumer-deployment.yaml:1) and [`producer-deployment.yaml`](producer-deployment.yaml:1) are now configured to:
- Use TLS with CA certificate directly (no JKS truststore needed)
- Mount the CA certificate from your MQ secret
- Support optional mTLS if needed

## Prerequisites

Your MQ secret must contain the CA certificate. Check with:

```bash
kubectl get secret mq-tls-certs -o jsonpath='{.data}' | jq 'keys'
```

Look for a key like `ca.crt`, `tls.crt`, or similar.

## Step 1: Update Secret Key Name (if needed)

If your CA certificate key is **not** named `ca.crt`, update both deployment files:

**In [`consumer-deployment.yaml`](consumer-deployment.yaml:50) and [`producer-deployment.yaml`](producer-deployment.yaml:56):**

```yaml
- name: MQ_SSL_CA_CERT_PATH
  value: "/etc/ssl/certs/YOUR-KEY-NAME.crt"  # Change ca.crt to your key name
```

**In the volumes section (line ~99 for consumer, ~105 for producer):**

```yaml
volumes:
  - name: tls-certs
    secret:
      secretName: mq-tls-certs
      items:
        - key: YOUR-KEY-NAME.crt  # Change ca.crt to your key name
          path: YOUR-KEY-NAME.crt
```

## Step 2: Update Cipher Suite (if needed)

The default cipher suite is `TLS_RSA_WITH_AES_128_CBC_SHA256`. If your MQ uses a different cipher suite, update:

```yaml
- name: MQ_SSL_CIPHER_SUITE
  value: "YOUR_CIPHER_SUITE"  # e.g., "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
```

Common cipher suites:
- `TLS_RSA_WITH_AES_128_CBC_SHA256`
- `TLS_RSA_WITH_AES_256_CBC_SHA256`
- `TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256`
- `TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384`

## Step 3: Enable mTLS (Optional)

If you need mutual TLS (client certificate authentication), uncomment these lines in both deployments:

```yaml
- name: MQ_SSL_KEYSTORE_PATH
  value: "/etc/ssl/certs/client-keystore.jks"
- name: MQ_SSL_KEYSTORE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: mq-tls-certs
      key: keystore-password
```

And in the volumes section:

```yaml
items:
  - key: ca.crt
    path: ca.crt
  - key: client-keystore.jks  # Uncomment this
    path: client-keystore.jks
```

## Step 4: Update Secret Name (if needed)

If your secret is **not** named `mq-tls-certs`, update the `secretName` in the volumes section:

```yaml
volumes:
  - name: tls-certs
    secret:
      secretName: YOUR-SECRET-NAME  # Change mq-tls-certs to your secret name
```

## Step 5: Deploy

Apply the updated configurations:

```bash
kubectl apply -f deployment-openshift/consumer-deployment.yaml
kubectl apply -f deployment-openshift/producer-deployment.yaml
```

## Step 6: Verify

Check the logs to confirm TLS is working:

```bash
# Consumer logs
kubectl logs -f deployment/jms-consumer

# Producer logs  
kubectl logs -f deployment/jms-producer
```

Look for these success messages:
- `Successfully configured CA certificate from: /etc/ssl/certs/ca.crt`
- `SSL CA certificate path configured: /etc/ssl/certs/ca.crt`
- `mTLS configuration applied` (if using mTLS)
- `MQ connection created successfully`

## Troubleshooting

### Error: Certificate file not found

**Problem:** `Failed to configure CA certificate from: /etc/ssl/certs/ca.crt`

**Solution:** Check that:
1. The secret exists: `kubectl get secret mq-tls-certs`
2. The key name matches in both the env var and volume mount
3. The pod has mounted the volume: `kubectl describe pod <pod-name>`

### Error: SSL handshake failed

**Problem:** `javax.net.ssl.SSLHandshakeException`

**Solution:** 
1. Verify the CA certificate is correct for your MQ server
2. Check the cipher suite matches your MQ configuration
3. Ensure the certificate is in valid PEM format

### Error: Connection refused

**Problem:** Can't connect to MQ

**Solution:**
1. Verify MQ host and port are correct
2. Check that the MQ channel accepts TLS connections
3. Confirm the queue manager is running

## Configuration Summary

### Current Configuration (TLS Only)

```yaml
Environment Variables:
  MQ_SSL_CIPHER_SUITE: TLS_RSA_WITH_AES_128_CBC_SHA256
  MQ_SSL_CA_CERT_PATH: /etc/ssl/certs/ca.crt
  MQ_SSL_PEER_NAME_ENABLED: true

Volume Mount:
  Secret: mq-tls-certs
  Key: ca.crt → /etc/ssl/certs/ca.crt
```

### For mTLS (Uncomment Additional Lines)

```yaml
Additional Environment Variables:
  MQ_SSL_KEYSTORE_PATH: /etc/ssl/certs/client-keystore.jks
  MQ_SSL_KEYSTORE_PASSWORD: (from secret)

Additional Volume Mount:
  Key: client-keystore.jks → /etc/ssl/certs/client-keystore.jks
```

## Key Benefits

✅ **No JKS truststore needed** - Use CA certificate directly  
✅ **No truststore password** - Simpler secret management  
✅ **Kubernetes-native** - Direct secret mounting  
✅ **Easy updates** - Update secret, restart pods  
✅ **Less complexity** - Fewer files to manage  

## Next Steps

- Review the full documentation: [`CA-CERTIFICATE-SETUP.md`](CA-CERTIFICATE-SETUP.md:1)
- Check your MQ configuration matches the cipher suite
- Test the connection after deployment
- Monitor logs for any SSL/TLS errors

## Need Help?

Common issues and solutions are in [`CA-CERTIFICATE-SETUP.md`](CA-CERTIFICATE-SETUP.md:1) under the Troubleshooting section.