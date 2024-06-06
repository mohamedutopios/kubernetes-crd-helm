Pour la création d'un serveur d'API personnalisé qui interagit avec Vault et expose une API via l'API Aggregation de Kubernetes, voici les étapes détaillées, y compris le code, le Dockerfile, et les manifestes Kubernetes nécessaires.

### Étapes pour Créer un Serveur d'API Personnalisé

1. **Écrire le Serveur d'API Personnalisé en Go**
2. **Construire et Pousser l'Image Docker**
3. **Déployer le Serveur d'API Personnalisé dans Kubernetes**
4. **Enregistrer l'API Personnalisée avec l'API Aggregation de Kubernetes**
5. **Déployer une Application Utilisant l'API Personnalisée pour Récupérer les Secrets**

### 1. Écrire le Serveur d'API Personnalisé en Go

**Fichier `main.go`**:

```go
package main

import (
    "encoding/json"
    "log"
    "net/http"
    "os"

    "github.com/hashicorp/vault/api"
)

func main() {
    http.HandleFunc("/v1alpha1/secrets", handleSecrets)
    log.Fatal(http.ListenAndServe(":8443", nil))
}

func handleSecrets(w http.ResponseWriter, r *http.Request) {
    config := api.DefaultConfig()
    config.Address = os.Getenv("VAULT_ADDR")

    client, err := api.NewClient(config)
    if err != nil {
        http.Error(w, err.Error(), http.StatusInternalServerError)
        return
    }

    client.SetToken(os.Getenv("VAULT_TOKEN"))

    secret, err := client.Logical().Read("secret/data/myapp/config")
    if err != nil {
        http.Error(w, err.Error(), http.StatusInternalServerError)
        return
    }

    json.NewEncoder(w).Encode(secret.Data)
}

func init() {
    os.Setenv("VAULT_ADDR", "http://vault.default.svc:8200")
    os.Setenv("VAULT_TOKEN", "root-token")
}
```

### 2. Construire et Pousser l'Image Docker

**Dockerfile**:

```Dockerfile
FROM golang:1.16 as builder
WORKDIR /app
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o custom-api .

FROM alpine:latest
COPY --from=builder /app/custom-api /custom-api
EXPOSE 8443
ENTRYPOINT ["/custom-api"]
```

**Construire et Pousser l'Image Docker**:

```sh
docker build -t your-repo/custom-api .
docker push your-repo/custom-api
```

### 3. Déployer le Serveur d'API Personnalisé dans Kubernetes

**custom-api-deployment.yaml**:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: custom-api
  namespace: custom-metrics
  labels:
    app: custom-api
spec:
  replicas: 1
  selector:
    matchLabels:
      app: custom-api
  template:
    metadata:
      labels:
        app: custom-api
    spec:
      containers:
        - name: custom-api
          image: your-repo/custom-api
          ports:
            - containerPort: 8443
---
apiVersion: v1
kind: Service
metadata:
  name: custom-api
  namespace: custom-metrics
spec:
  ports:
    - port: 443
      targetPort: 8443
  selector:
    app: custom-api
```

Déployer le serveur d'API personnalisé:

```sh
kubectl apply -f custom-api-deployment.yaml
```

### 4. Enregistrer l'API Personnalisée avec l'API Aggregation de Kubernetes

**apiservice.yaml**:

```yaml
apiVersion: apiregistration.k8s.io/v1
kind: APIService
metadata:
  name: v1alpha1.custom.secrets.k8s.io
spec:
  service:
    name: custom-api
    namespace: custom-metrics
    port: 443
  group: custom.secrets.k8s.io
  version: v1alpha1
  insecureSkipTLSVerify: true
  groupPriorityMinimum: 100
  versionPriority: 100
```

Enregistrer l'API personnalisée:

```sh
kubectl apply -f apiservice.yaml
```

### 5. Déployer une Application Utilisant l'API Personnalisée pour Récupérer les Secrets

**myapp-deployment.yaml**:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: myapp-sa
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
  labels:
    app: myapp
spec:
  replicas: 1
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      serviceAccountName: myapp-sa
      containers:
        - name: myapp
          image: k8s.gcr.io/echoserver:1.4
          env:
            - name: VAULT_SECRET
              valueFrom:
                secretKeyRef:
                  name: vault-config
                  key: secret
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: vault-config
data:
  secret: |
    $(kubectl get custom-secrets.k8s.io/v1alpha1 secrets -o jsonpath="{.data}")
```

Déployer l'application:

```sh
kubectl apply -f myapp-deployment.yaml
```

### Conclusion

L'API Aggregation permet de créer et enregistrer des APIs personnalisées auprès du serveur API Kubernetes. Dans cet exemple, nous avons créé un serveur d'API personnalisé qui interagit avec HashiCorp Vault pour récupérer des secrets et exposer cette API via l'API Aggregation. Cela permet aux applications Kubernetes d'accéder aux secrets de Vault via une API native Kubernetes, en utilisant une interface unifiée et sécurisée.

Alternativement, des méthodes comme Vault Agent Injector peuvent simplifier l'intégration sans nécessiter l'API Aggregation. Le choix de la méthode dépend des besoins spécifiques et des cas d'utilisation de votre infrastructure.