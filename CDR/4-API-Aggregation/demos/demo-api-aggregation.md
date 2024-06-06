Pour fournir un exemple complet de l'utilisation de l'API Aggregation pour le scaling d'un pod en utilisant des métriques personnalisées de Prometheus, nous allons suivre les étapes suivantes :

1. **Déployer Prometheus**
2. **Déployer l'adapter Prometheus**
3. **Déployer une application (pod)**
4. **Configurer l'APIService pour l'adapter Prometheus**
5. **Configurer un Horizontal Pod Autoscaler (HPA) pour utiliser les métriques personnalisées**

### 1. Déployer Prometheus

Créez un fichier `prometheus-deployment.yaml` pour déployer Prometheus.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  labels:
    app: prometheus
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
    scrape_configs:
      - job_name: 'kubernetes-pods'
        kubernetes_sd_configs:
        - role: pod
        relabel_configs:
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
          action: keep
          regex: true
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
          action: replace
          target_label: __metrics_path__
          regex: (.+)
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_port]
          action: replace
          target_label: __address__
          regex: (.+)
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prometheus
  labels:
    app: prometheus
spec:
  replicas: 1
  selector:
    matchLabels:
      app: prometheus
  template:
    metadata:
      labels:
        app: prometheus
    spec:
      containers:
      - name: prometheus
        image: prom/prometheus:v2.30.0
        args:
        - --config.file=/etc/prometheus/prometheus.yml
        - --storage.tsdb.path=/prometheus/
        ports:
        - containerPort: 9090
        volumeMounts:
        - name: prometheus-config-volume
          mountPath: /etc/prometheus/
      volumes:
      - name: prometheus-config-volume
        configMap:
          name: prometheus-config
---
apiVersion: v1
kind: Service
metadata:
  name: prometheus
  labels:
    app: prometheus
spec:
  type: ClusterIP
  ports:
  - port: 9090
    targetPort: 9090
  selector:
    app: prometheus
```

Appliquez ce fichier pour déployer Prometheus.

```sh
kubectl apply -f prometheus-deployment.yaml
```

### 2. Déployer l'adapter Prometheus

Créez un fichier `prometheus-adapter.yaml` pour déployer l'adapter Prometheus.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-adapter-config
  namespace: custom-metrics
data:
  config.yaml: |
    rules:
    - seriesQuery: 'http_requests_total{kubernetes_namespace!="",kubernetes_pod_name!=""}'
      resources:
        overrides:
          kubernetes_namespace: {resource: "namespace"}
          kubernetes_pod_name: {resource: "pod"}
      name:
        matches: "^(.*)_total"
        as: "${1}_per_second"
      metricsQuery: sum(rate(<<.Series>>{<<.LabelMatchers>>}[5m])) by (<<.GroupBy>>)
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prometheus-adapter
  namespace: custom-metrics
  labels:
    app: prometheus-adapter
spec:
  replicas: 1
  selector:
    matchLabels:
      app: prometheus-adapter
  template:
    metadata:
      labels:
        app: prometheus-adapter
    spec:
      containers:
      - name: prometheus-adapter
        image: quay.io/prometheus/prometheus-adapter:v0.8.3
        volumeMounts:
        - name: config
          mountPath: /etc/adapter/
        ports:
        - name: https
          containerPort: 6443
        args:
        - /adapter
        - --config.file=/etc/adapter/config.yaml
        - --secure-port=6443
        - --tls-cert-file=/etc/adapter/certs/tls.crt
        - --tls-private-key-file=/etc/adapter/certs/tls.key
        - --client-ca-file=/etc/adapter/certs/ca.crt
        - --logtostderr=true
        - --v=4
      volumes:
      - name: config
        configMap:
          name: prometheus-adapter-config
---
apiVersion: v1
kind: Service
metadata:
  name: custom-metrics-apiserver
  namespace: custom-metrics
spec:
  ports:
  - port: 443
    targetPort: 6443
  selector:
    app: prometheus-adapter
---
apiVersion: apiregistration.k8s.io/v1
kind: APIService
metadata:
  name: v1beta1.custom.metrics.k8s.io
spec:
  service:
    name: custom-metrics-apiserver
    namespace: custom-metrics
    port: 443
  group: custom.metrics.k8s.io
  version: v1beta1
  insecureSkipTLSVerify: true
  groupPriorityMinimum: 100
  versionPriority: 100
```

Appliquez ce fichier pour déployer l'adapter Prometheus.

```sh
kubectl create namespace custom-metrics
kubectl apply -f prometheus-adapter.yaml
```

### 3. Déployer une application (pod)

Créez un fichier `app-deployment.yaml` pour déployer une application simple.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
  labels:
    app: my-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
    spec:
      containers:
      - name: my-app
        image: k8s.gcr.io/echoserver:1.4
        ports:
        - containerPort: 8080
```

Appliquez ce fichier pour déployer l'application.

```sh
kubectl apply -f app-deployment.yaml
```

### 4. Configurer l'APIService pour l'adapter Prometheus

Cela a déjà été fait dans le fichier `prometheus-adapter.yaml` ci-dessus avec la section `APIService`.

### 5. Configurer un Horizontal Pod Autoscaler (HPA) pour utiliser les métriques personnalisées

Créez un fichier `hpa.yaml` pour configurer un HPA.

```yaml
apiVersion: autoscaling/v2beta2
kind: HorizontalPodAutoscaler
metadata:
  name: custom-metric-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-app
  minReplicas: 1
  maxReplicas: 10
  metrics:
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "10"
```

Appliquez ce fichier pour créer le HPA.

```sh
kubectl apply -f hpa.yaml
```

### Conclusion

Vous avez maintenant un cluster Kubernetes avec Prometheus collectant des métriques, un adapter Prometheus exposant ces métriques via l'API Aggregation, une application déployée, et un HPA utilisant des métriques personnalisées pour le scaling automatique des pods.