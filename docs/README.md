# IBM MQ JMS Demo Apps - Documentation

This directory contains comprehensive guides for configuring and troubleshooting the IBM MQ JMS applications.

## Quick Start

- **[DOCKER-COMPOSE-GUIDE.md](DOCKER-COMPOSE-GUIDE.md)** - Run the complete stack locally with Docker Compose (recommended for beginners)
- **[QUICK-START-CA-CERT.md](QUICK-START-CA-CERT.md)** - Get started quickly with CA certificate configuration

## Getting Started

### Local Development

- **[DOCKER-COMPOSE-GUIDE.md](DOCKER-COMPOSE-GUIDE.md)** - Complete guide for running locally with Docker Compose
  - Quick start with IBM MQ + Producer + Consumer
  - Configuration examples and scenarios
  - Troubleshooting common issues
  - Performance tuning tips
  - CI/CD integration examples

## Configuration Guides

### SSL/TLS Configuration

- **[CA-CERTIFICATE-SETUP.md](CA-CERTIFICATE-SETUP.md)** - Complete guide for using CA certificates directly (recommended approach)
  - How to use PEM certificates without JKS truststore
  - No password needed for truststore
  - Kubernetes-native secret management
  - Step-by-step configuration examples

- **[JAVA-SSL-PROPERTIES.md](JAVA-SSL-PROPERTIES.md)** - Alternative SSL configuration using JAVA_TOOL_OPTIONS
  - Three different approaches to SSL configuration
  - When to use each approach
  - Comparison table and examples
  - Standard Java SSL properties

- **[SECURE-PASSWORD-OPTIONS.md](SECURE-PASSWORD-OPTIONS.md)** - Secure password management strategies
  - How to avoid clear text passwords
  - Password-less JKS truststore approach
  - Using Kubernetes secrets securely
  - Security best practices

### Verification and Security

- **[HOSTNAME-VERIFICATION.md](HOSTNAME-VERIFICATION.md)** - Hostname and peer name verification options
  - Two verification mechanisms explained
  - When to enable/disable each
  - Common scenarios and configurations
  - Security considerations

## Troubleshooting

- **[TROUBLESHOOTING-CA-CERT.md](TROUBLESHOOTING-CA-CERT.md)** - Troubleshoot certificate validation issues
  - Step-by-step diagnostic process
  - Common issues and solutions
  - How to verify certificates
  - Complete working configuration examples

- **[DEBUG-LOGGING-GUIDE.md](DEBUG-LOGGING-GUIDE.md)** - Enable debug logging for troubleshooting
  - SSL/TLS debug logging options
  - IBM MQ trace configuration
  - How to read debug output
  - Performance impact considerations

## Document Overview

### By Topic

#### Certificate Management
1. [CA-CERTIFICATE-SETUP.md](CA-CERTIFICATE-SETUP.md) - Main guide for CA certificates
2. [QUICK-START-CA-CERT.md](QUICK-START-CA-CERT.md) - Quick reference
3. [SECURE-PASSWORD-OPTIONS.md](SECURE-PASSWORD-OPTIONS.md) - Password security
4. [TROUBLESHOOTING-CA-CERT.md](TROUBLESHOOTING-CA-CERT.md) - Fix certificate issues

#### SSL/TLS Configuration
1. [JAVA-SSL-PROPERTIES.md](JAVA-SSL-PROPERTIES.md) - Java SSL properties
2. [HOSTNAME-VERIFICATION.md](HOSTNAME-VERIFICATION.md) - Verification options
3. [CA-CERTIFICATE-SETUP.md](CA-CERTIFICATE-SETUP.md) - Certificate setup

#### Troubleshooting
1. [TROUBLESHOOTING-CA-CERT.md](TROUBLESHOOTING-CA-CERT.md) - Certificate issues
2. [DEBUG-LOGGING-GUIDE.md](DEBUG-LOGGING-GUIDE.md) - Debug logging

### By Use Case

#### "I want to run this locally on my laptop"
→ Start with [DOCKER-COMPOSE-GUIDE.md](DOCKER-COMPOSE-GUIDE.md)
→ Quick setup: `docker-compose up -d`

#### "I want to use CA certificates directly"
→ Start with [QUICK-START-CA-CERT.md](QUICK-START-CA-CERT.md)
→ Detailed guide: [CA-CERTIFICATE-SETUP.md](CA-CERTIFICATE-SETUP.md)

#### "I'm getting certificate validation errors"
→ Start with [TROUBLESHOOTING-CA-CERT.md](TROUBLESHOOTING-CA-CERT.md)  
→ Enable debug: [DEBUG-LOGGING-GUIDE.md](DEBUG-LOGGING-GUIDE.md)

#### "I want to avoid clear text passwords"
→ Read [SECURE-PASSWORD-OPTIONS.md](SECURE-PASSWORD-OPTIONS.md)  
→ Use CA certificates: [CA-CERTIFICATE-SETUP.md](CA-CERTIFICATE-SETUP.md)

#### "I need to disable hostname verification"
→ Read [HOSTNAME-VERIFICATION.md](HOSTNAME-VERIFICATION.md)  
→ Understand security implications

#### "I want to use JAVA_TOOL_OPTIONS"
→ Read [JAVA-SSL-PROPERTIES.md](JAVA-SSL-PROPERTIES.md)  
→ Compare approaches and choose best fit

## Recommended Reading Order

### For New Users
1. [DOCKER-COMPOSE-GUIDE.md](DOCKER-COMPOSE-GUIDE.md) - Run locally first
2. [QUICK-START-CA-CERT.md](QUICK-START-CA-CERT.md) - Get started with certificates
3. [CA-CERTIFICATE-SETUP.md](CA-CERTIFICATE-SETUP.md) - Understand the details
4. [HOSTNAME-VERIFICATION.md](HOSTNAME-VERIFICATION.md) - Configure security

### For Troubleshooting
1. [TROUBLESHOOTING-CA-CERT.md](TROUBLESHOOTING-CA-CERT.md) - Diagnose issues
2. [DEBUG-LOGGING-GUIDE.md](DEBUG-LOGGING-GUIDE.md) - Enable debug output
3. [CA-CERTIFICATE-SETUP.md](CA-CERTIFICATE-SETUP.md) - Verify configuration

### For Advanced Configuration
1. [JAVA-SSL-PROPERTIES.md](JAVA-SSL-PROPERTIES.md) - Alternative approaches
2. [SECURE-PASSWORD-OPTIONS.md](SECURE-PASSWORD-OPTIONS.md) - Security options
3. [HOSTNAME-VERIFICATION.md](HOSTNAME-VERIFICATION.md) - Fine-tune verification

## Key Concepts

### CA Certificate vs JKS Truststore

| Aspect | CA Certificate (PEM) | JKS Truststore |
|--------|---------------------|----------------|
| Format | PEM (text) | JKS (binary) |
| Password | Not needed | Required |
| Setup | Simple | Complex |
| Kubernetes | Native | Less aligned |
| Recommended | ✅ Yes | ⚠️ Legacy |

### Verification Mechanisms

1. **Peer Name Verification** (`MQ_SSL_PEER_NAME_ENABLED`)
   - IBM MQ specific
   - Verifies certificate DN
   - Default: enabled

2. **Hostname Verification** (`MQ_SSL_HOSTNAME_VERIFICATION_ENABLED`)
   - Standard SSL/TLS
   - Verifies CN/SAN matches hostname
   - Default: enabled

Both should be enabled in production for maximum security.

## Quick Reference

### Environment Variables

```yaml
# Recommended: CA Certificate approach
MQ_SSL_CIPHER_SUITE=TLS_RSA_WITH_AES_128_CBC_SHA256
MQ_SSL_CA_CERT_PATH=/etc/ssl/certs/ca.crt
MQ_SSL_PEER_NAME_ENABLED=true
MQ_SSL_HOSTNAME_VERIFICATION_ENABLED=true

# Optional: For mTLS
MQ_SSL_KEYSTORE_PATH=/etc/ssl/certs/client-keystore.jks
MQ_SSL_KEYSTORE_PASSWORD=<from-secret>
```

### Common Commands

```bash
# Check certificate
kubectl exec -it deployment/jms-consumer -- \
  openssl x509 -in /etc/ssl/certs/ca.crt -text -noout

# View logs
kubectl logs -f deployment/jms-consumer

# Enable debug
kubectl set env deployment/jms-consumer \
  JAVA_TOOL_OPTIONS="-Djavax.net.debug=ssl:handshake:verbose"

# Restart deployment
kubectl rollout restart deployment/jms-consumer
```

## Support

For issues or questions:
1. Check the troubleshooting guide
2. Enable debug logging
3. Review the relevant configuration guide
4. Check application logs for error messages

## Contributing

When adding new documentation:
- Follow the existing structure
- Include practical examples
- Add troubleshooting sections
- Update this README with links
- Keep security best practices in mind