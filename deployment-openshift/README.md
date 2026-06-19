# OpenShift/Kubernetes Deployment

ArgoCD-ready deployment manifests for IBM MQ JMS Producer and Consumer applications.

## Files

- **`kustomization.yaml`** - Kustomize configuration for ArgoCD
- **`secret.yaml`** - MQ client authentication secret
- **`producer-deployment.yaml`** - JMS Producer deployment
- **`consumer-deployment.yaml`** - JMS Consumer deployment
- **`ccdt-configmap.yaml`** - CCDT configuration for uniform clusters (optional)

## Prerequisites

1. **IBM MQ Queue Manager** running and accessible
2. **GHCR Access** - Images are public, but for private repos configure image pull secrets
3. **Secret Configuration** - Update MQ password in `secret.yaml`

## Quick Start

### Using kubectl

```bash
# Create namespace (optional)
kubectl create namespace ibmmq-jms-demo

# Apply all manifests
kubectl apply -f deployment-openshift/

# Or using kustomize
kubectl apply -k deployment-openshift/
```

### Using ArgoCD

#### Option 1: ArgoCD CLI

```bash
argocd app create ibmmq-jms-demo \
  --repo https://github.com/fabien-sanglier-ibm/ibmmq-demoapps-jms.git \
  --path deployment-openshift \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace ibmmq-jms-demo \
  --sync-policy automated \
  --auto-prune \
  --self-heal
```

#### Option 2: ArgoCD Application Manifest

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: ibmmq-jms-demo
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/fabien-sanglier-ibm/ibmmq-demoapps-jms.git
    targetRevision: main
    path: deployment-openshift
  destination:
    server: https://kubernetes.default.svc
    namespace: ibmmq-jms-demo
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
```

## Configuration

### Environment Variables

Both deployments can be customized via environment variables:

#### Common Variables
- `MQ_CCDT_URL` - CCDT file URL for uniform cluster (optional, e.g., `file:///etc/mq/ccdt.json`)
- `MQ_HOST` - MQ host (default: `qmdemo-ibm-mq`, ignored if CCDT is used)
- `MQ_PORT` - MQ port (default: `1414`, ignored if CCDT is used)
- `MQ_QUEUE_MANAGER` - Queue manager name (default: `QMDEMO`, use `*` for uniform cluster)
- `MQ_CHANNEL` - Server connection channel (default: `DEV.APP.SVRCONN.0TLS`)
- `MQ_QUEUE_NAME` - Queue name (default: `DEV.QUEUE.1`)
- `MQ_APP_USERNAME` - Application username (default: `app`)
- `MQ_APP_PASSWORD` - From secret `mq-client-auth`

#### Producer-Specific
- `MQ_APP_NAME` - Application name (default: `MY-PRODUCER`)
- `MQ_MESSAGE` - Message content (default: `Test message from JMS Producer`)
- `MQ_MESSAGE_COUNT` - Number of messages (default: `4000`)
- `MQ_SEND_SLEEP_MILLIS` - Sleep between sends in ms (default: `3000`)
- `MQ_LOG_FREQUENCY` - Log progress every N messages (default: `100`)

#### Consumer-Specific
- `MQ_APP_NAME` - Application name (default: `MY-CONSUMER`)
- `MQ_RECEIVE_SLEEP_MILLIS` - Sleep after receive in ms (default: `500`)
- `MQ_RECEIVE_TIMEOUT_MILLIS` - Receive timeout in ms (default: `1000`)

### Updating the Secret

**Important**: Update the password before deploying to production!

```bash
# Edit secret.yaml and change the password
kubectl apply -f deployment-openshift/secret.yaml

# Or create from command line
kubectl create secret generic mq-client-auth \
  --from-literal=password='your-secure-password' \
  --dry-run=client -o yaml > deployment-openshift/secret.yaml
```

### Using Specific Image Versions

Edit `kustomization.yaml` to use specific versions:

```yaml
images:
  - name: ghcr.io/fabien-sanglier-ibm/ibmmq-demoapps-jms/jmsproducer
    newTag: 1.0.0  # Use specific version
  - name: ghcr.io/fabien-sanglier-ibm/ibmmq-demoapps-jms/jmsconsumer
    newTag: 1.0.0  # Use specific version
```

## CCDT Configuration (Uniform Cluster)

For uniform clusters or multi-instance queue managers, use CCDT (Client Channel Definition Table) instead of direct host/port configuration.

### Step 1: Create CCDT ConfigMap

Edit `ccdt-configmap.yaml` with your queue manager endpoints:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mq-ccdt-config
data:
  ccdt.json: |
    {
      "channel": [
        {
          "name": "DEV.APP.SVRCONN.0TLS",
          "clientConnection": {
            "connection": [
              {
                "host": "qm1-ibm-mq.mq-namespace.svc.cluster.local",
                "port": 1414
              }
            ],
            "queueManager": "QM1"
          },
          "transmissionSecurity": {
            "cipherSpecification": "TLS_RSA_WITH_AES_128_CBC_SHA256"
          },
          "type": "clientConnection"
        }
      ]
    }
```

Apply the ConfigMap:

```bash
kubectl apply -f deployment-openshift/ccdt-configmap.yaml
```

### Step 2: Enable CCDT in Deployments

In both `producer-deployment.yaml` and `consumer-deployment.yaml`, uncomment the CCDT configuration:

```yaml
env:
  # CCDT Configuration
  - name: MQ_CCDT_URL
    value: "file:///etc/mq/ccdt.json"
  - name: MQ_QUEUE_MANAGER
    value: "*"  # Use * for uniform cluster
```

And uncomment the volume mounts:

```yaml
volumeMounts:
  - name: ccdt-config
    mountPath: /etc/mq
    readOnly: true

volumes:
  - name: ccdt-config
    configMap:
      name: mq-ccdt-config
      items:
        - key: ccdt.json
          path: ccdt.json
```

### Step 3: Deploy

```bash
kubectl apply -k deployment-openshift/
```

For detailed CCDT configuration options, see [CCDT-CONFIGURATION.md](../docs/CCDT-CONFIGURATION.md).

## Image Pull Secrets (for Private Repos)

If your GHCR images are private, create an image pull secret:

```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=<github-username> \
  --docker-password=<github-token> \
  --docker-email=<email>
```

Then add to deployments:

```yaml
spec:
  template:
    spec:
      imagePullSecrets:
        - name: ghcr-secret
```

## Resource Requirements

### Default Resources
- **Requests**: 100m CPU, 256Mi memory
- **Limits**: 500m CPU, 512Mi memory

Adjust based on your workload in the deployment files.

## Security

### Security Context
Both deployments include security best practices:
- `runAsNonRoot: true`
- `allowPrivilegeEscalation: false`
- Drop all capabilities
- Runtime default seccomp profile

### Network Policies
Consider adding network policies to restrict traffic:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: jms-apps-policy
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/part-of: ibmmq-jms-demo
  policyTypes:
    - Egress
  egress:
    - to:
        - podSelector:
            matchLabels:
              app: ibm-mq
      ports:
        - protocol: TCP
          port: 1414
```

## Monitoring

### View Logs

```bash
# Producer logs
kubectl logs -l app=jms-producer -f

# Consumer logs
kubectl logs -l app=jms-consumer -f
```

### Check Status

```bash
# Get deployments
kubectl get deployments -l app.kubernetes.io/part-of=ibmmq-jms-demo

# Get pods
kubectl get pods -l app.kubernetes.io/part-of=ibmmq-jms-demo

# Describe pod for troubleshooting
kubectl describe pod -l app=jms-producer
```

## Scaling

```bash
# Scale producer
kubectl scale deployment jms-producer --replicas=3

# Scale consumer
kubectl scale deployment jms-consumer --replicas=2
```

## Troubleshooting

### Common Issues

1. **Connection Refused**
   - Check MQ_HOST is correct
   - Verify MQ service is running
   - Check network policies

2. **Authentication Failed**
   - Verify secret is created: `kubectl get secret mq-client-auth`
   - Check password is correct
   - Verify MQ user permissions

3. **Image Pull Errors**
   - For private repos, ensure image pull secret is configured
   - Verify GHCR image exists: `docker pull ghcr.io/fabien-sanglier-ibm/ibmmq-demoapps-jms/jmsproducer:latest`

4. **Pod CrashLoopBackOff**
   - Check logs: `kubectl logs <pod-name>`
   - Verify all environment variables are set
   - Check MQ connectivity

## Cleanup

```bash
# Delete all resources
kubectl delete -k deployment-openshift/

# Or individually
kubectl delete -f deployment-openshift/
```

## ArgoCD Sync Waves

For controlled deployment order, add sync waves:

```yaml
metadata:
  annotations:
    argocd.argoproj.io/sync-wave: "0"  # Secret first
    argocd.argoproj.io/sync-wave: "1"  # Then deployments