# Using CA Certificate Directly (Without JKS Truststore)

This guide explains how to configure the JMS applications to use a CA certificate directly from your MQ cluster secret, instead of creating a JKS truststore.

## Overview

Instead of packaging the CA certificate into a JKS truststore file, you can mount the CA certificate directly as a PEM file and configure the Java applications to trust it. This approach is simpler and more aligned with Kubernetes/OpenShift secret management practices.

## Prerequisites

Your MQ cluster secret should contain the CA certificate in PEM format with a key like `ca.crt` or `tls.crt`.

## Configuration Steps

### 1. Verify Your Secret Contains the CA Certificate

Check that your MQ TLS secret contains the CA certificate:

```bash
kubectl get secret mq-tls-certs -o yaml
```

Look for a key like `ca.crt`, `tls.crt`, or similar containing the CA certificate in PEM format.

### 2. Update Deployment Configuration

In both `consumer-deployment.yaml` and `producer-deployment.yaml`, uncomment and configure **Option 2** (Using CA certificate directly):

```yaml
env:
  # Option 2: Using CA certificate directly (PEM format)
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"  # Or your preferred cipher suite
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/ca.crt"
  # If using mTLS (client certificate authentication), also configure:
  - name: MQ_SSL_KEYSTORE_PATH
    value: "/etc/ssl/certs/client-keystore.jks"
  - name: MQ_SSL_KEYSTORE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: mq-tls-certs
        key: keystore-password
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "true"

volumeMounts:
  - name: tls-certs
    mountPath: /etc/ssl/certs
    readOnly: true
```

### 3. Configure Volume to Mount CA Certificate

Update the volumes section to mount the CA certificate:

```yaml
volumes:
  # Option 2: Mount CA certificate directly (PEM format)
  - name: tls-certs
    secret:
      secretName: mq-tls-certs
      items:
        - key: ca.crt  # Use the actual key name from your secret
          path: ca.crt
        # If using mTLS, also mount the client keystore:
        - key: client-keystore.jks
          path: client-keystore.jks
```

### 4. Apply the Configuration

```bash
kubectl apply -f consumer-deployment.yaml
kubectl apply -f producer-deployment.yaml
```

## How It Works

The Java applications now include a `configureCaCertificate()` method that:

1. Reads the CA certificate from the PEM file
2. Creates an in-memory Java KeyStore
3. Adds the CA certificate to the KeyStore
4. Configures a TrustManager with this KeyStore
5. Sets it as the default SSL context for the JVM

This approach eliminates the need to:
- Create a JKS truststore file
- Manage truststore passwords
- Convert PEM certificates to JKS format

## Configuration Options

### TLS Only (Server Authentication)

For TLS where only the server (MQ) is authenticated:

```yaml
env:
  - name: MQ_SSL_CIPHER_SUITE
    value: "TLS_RSA_WITH_AES_128_CBC_SHA256"
  - name: MQ_SSL_CA_CERT_PATH
    value: "/etc/ssl/certs/ca.crt"
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "true"

volumes:
  - name: tls-certs
    secret:
      secretName: mq-tls-certs
      items:
        - key: ca.crt
          path: ca.crt
```

### mTLS (Mutual Authentication)

For mTLS where both client and server are authenticated:

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
  - name: MQ_SSL_PEER_NAME_ENABLED
    value: "true"

volumes:
  - name: tls-certs
    secret:
      secretName: mq-tls-certs
      items:
        - key: ca.crt
          path: ca.crt
        - key: client-keystore.jks
          path: client-keystore.jks
```

## Supported Certificate Formats

The CA certificate must be in PEM format (X.509). Common file extensions:
- `.crt`
- `.pem`
- `.cer`

The certificate should start with:
```
-----BEGIN CERTIFICATE-----
```

## Troubleshooting

### Certificate Not Found

If you see errors about certificate file not found:
1. Verify the secret exists: `kubectl get secret mq-tls-certs`
2. Check the key name in the secret matches your volume mount
3. Verify the mount path in the deployment

### Certificate Validation Errors

If you see SSL handshake or certificate validation errors:
1. Verify the CA certificate is the correct one that signed the MQ server certificate
2. Check that the certificate is in valid PEM format
3. Ensure `MQ_SSL_PEER_NAME_ENABLED` is set appropriately for your environment

### View Application Logs

```bash
# Consumer logs
kubectl logs -f deployment/jms-consumer

# Producer logs
kubectl logs -f deployment/jms-producer
```

Look for log messages like:
- `Successfully configured CA certificate from: /etc/ssl/certs/ca.crt`
- `SSL CA certificate path configured: /etc/ssl/certs/ca.crt`

## Comparison: JKS Truststore vs CA Certificate

| Aspect | JKS Truststore (Option 1) | CA Certificate (Option 2) |
|--------|---------------------------|---------------------------|
| Setup Complexity | Higher - requires creating JKS file | Lower - use certificate directly |
| Password Management | Required | Not required |
| Secret Size | Larger (binary JKS file) | Smaller (text PEM file) |
| Kubernetes Native | Less aligned | More aligned |
| Certificate Updates | Requires JKS recreation | Direct secret update |
| Recommended For | Legacy systems, multiple CAs | Modern deployments, single CA |

## Security Considerations

1. **Peer Name Verification**: Keep `MQ_SSL_PEER_NAME_ENABLED=true` in production to verify the server's identity
2. **Secret Access**: Ensure proper RBAC controls on the secret containing certificates
3. **Certificate Rotation**: Update the secret when certificates are rotated; pods will need to be restarted
4. **Read-Only Mounts**: Always mount certificate volumes as `readOnly: true`

## Example Secret Structure

Your MQ TLS secret should look like this:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mq-tls-certs
type: Opaque
data:
  ca.crt: <base64-encoded-ca-certificate>
  # Optional: for mTLS
  client-keystore.jks: <base64-encoded-keystore>
  keystore-password: <base64-encoded-password>
```

To create the secret from files:

```bash
kubectl create secret generic mq-tls-certs \
  --from-file=ca.crt=/path/to/ca.crt \
  --from-file=client-keystore.jks=/path/to/client-keystore.jks \
  --from-literal=keystore-password='your-password'
```

## Additional Resources

- [IBM MQ TLS Configuration](https://www.ibm.com/docs/en/ibm-mq/latest?topic=mechanisms-tls-security-protocols-in-mq)
- [Java SSL/TLS Configuration](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html)
- [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)