Un `Dynamic Admission Controller` dans Kubernetes est un mécanisme qui permet d'intercepter les requêtes vers l'API Kubernetes et de les modifier ou de les valider avant qu'elles ne soient persistées dans l'étcd. Il existe deux principaux types de Dynamic Admission Controllers dans Kubernetes:

1. **Mutating Admission Webhook**: Il permet de modifier les objets avant qu'ils ne soient enregistrés.
2. **Validating Admission Webhook**: Il permet de valider les objets avant qu'ils ne soient enregistrés, sans les modifier.

### Fonctionnement des Admission Controllers Dynamiques

1. **Requête API**: Lorsqu'une requête arrive à l'API Server de Kubernetes pour créer, mettre à jour, ou supprimer une ressource, l'API Server fait appel aux webhooks configurés avant de persister la requête dans le datastore (etcd).
2. **Mutating Webhooks**: Si un webhook de mutation est configuré, il peut modifier la requête, par exemple en ajoutant ou en changeant des champs.
3. **Validating Webhooks**: Après la mutation (le cas échéant), les webhooks de validation sont appelés pour valider la requête. Ils ne peuvent pas modifier la requête, mais peuvent rejeter la requête si elle ne satisfait pas certaines conditions.
4. **Décision**: Si tous les webhooks acceptent la requête (ou la modifient de manière acceptable), l'API Server persiste la ressource. Sinon, la requête est rejetée.

### Configuration des Admission Controllers Dynamiques

Voici les étapes pour configurer un Dynamic Admission Controller :

1. **Écrire le Serveur Webhook**: Créez un serveur HTTP(s) pour traiter les requêtes d'admission. Ce serveur doit répondre aux requêtes d'admission envoyées par le serveur API Kubernetes.

2. **Déployer le Serveur Webhook**: Déployez ce serveur en tant que service dans votre cluster Kubernetes.

3. **Configurer les Webhooks**: Créez des objets de configuration `MutatingWebhookConfiguration` et/ou `ValidatingWebhookConfiguration` pour enregistrer votre webhook auprès de l'API Server.

### Exemple Complet

#### 1. Écrire un Serveur Webhook en Go

**main.go** :

```go
package main

import (
    "encoding/json"
    "fmt"
    "log"
    "net/http"

    admissionv1 "k8s.io/api/admission/v1"
    metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func main() {
    http.HandleFunc("/mutate", handleMutate)
    http.HandleFunc("/validate", handleValidate)
    log.Fatal(http.ListenAndServeTLS(":443", "/tls/tls.crt", "/tls/tls.key", nil))
}

func handleMutate(w http.ResponseWriter, r *http.Request) {
    var admissionReview admissionv1.AdmissionReview
    err := json.NewDecoder(r.Body).Decode(&admissionReview)
    if err != nil {
        http.Error(w, err.Error(), http.StatusBadRequest)
        return
    }

    // Exemple de mutation: Ajouter un label "mutated=true"
    admissionReview.Response = &admissionv1.AdmissionResponse{
        UID:     admissionReview.Request.UID,
        Allowed: true,
        Patch: []byte(`[{"op": "add", "path": "/metadata/labels/mutated", "value": "true"}]`),
        PatchType: func() *admissionv1.PatchType {
            pt := admissionv1.PatchTypeJSONPatch
            return &pt
        }(),
    }

    respBytes, err := json.Marshal(admissionReview)
    if err != nil {
        http.Error(w, err.Error(), http.StatusInternalServerError)
        return
    }
    w.Header().Set("Content-Type", "application/json")
    w.Write(respBytes)
}

func handleValidate(w http.ResponseWriter, r *http.Request) {
    var admissionReview admissionv1.AdmissionReview
    err := json.NewDecoder(r.Body).Decode(&admissionReview)
    if err != nil {
        http.Error(w, err.Error(), http.StatusBadRequest)
        return
    }

    // Exemple de validation: Vérifier que les pods ont un label "valid=true"
    allowed := true
    message := "validation succeeded"
    if admissionReview.Request.Kind.Kind == "Pod" {
        var pod metav1.ObjectMeta
        err := json.Unmarshal(admissionReview.Request.Object.Raw, &pod)
        if err != nil {
            http.Error(w, err.Error(), http.StatusInternalServerError)
            return
        }

        if pod.Labels["valid"] != "true" {
            allowed = false
            message = "pod must have label 'valid=true'"
        }
    }

    admissionReview.Response = &admissionv1.AdmissionResponse{
        UID:     admissionReview.Request.UID,
        Allowed: allowed,
        Result: &metav1.Status{
            Message: message,
        },
    }

    respBytes, err := json.Marshal(admissionReview)
    if err != nil {
        http.Error(w, err.Error(), http.StatusInternalServerError)
        return
    }
    w.Header().Set("Content-Type", "application/json")
    w.Write(respBytes)
}
```

#### 2. Construire et Pousser l'Image Docker

**Dockerfile** :

```Dockerfile
FROM golang:1.16 as builder
WORKDIR /app
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o admission-webhook .

FROM alpine:latest
COPY --from=builder /app/admission-webhook /admission-webhook
ENTRYPOINT ["/admission-webhook"]
```

Construisez et poussez l'image Docker.

```sh
docker build -t your-repo/admission-webhook .
docker push your-repo/admission-webhook
```

#### 3. Déployer le Serveur Webhook dans Kubernetes

Créez les fichiers de déploiement Kubernetes pour votre serveur webhook.

**deployment.yaml** :

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: admission-webhook
  namespace: default
spec:
  replicas: 1
  selector:
    matchLabels:
      app: admission-webhook
  template:
    metadata:
      labels:
        app: admission-webhook
    spec:
      containers:
        - name: admission-webhook
          image: your-repo/admission-webhook
          ports:
            - containerPort: 443
          volumeMounts:
            - name: tls
              mountPath: /tls
      volumes:
        - name: tls
          secret:
            secretName: admission-webhook-tls
---
apiVersion: v1
kind: Service
metadata:
  name: admission-webhook
  namespace: default
spec:
  ports:
    - port: 443
      targetPort: 443
  selector:
    app: admission-webhook
```

Déployez le webhook :

```sh
kubectl apply -f deployment.yaml
```

#### 4. Configurer les Webhooks

Créez des objets `MutatingWebhookConfiguration` et `ValidatingWebhookConfiguration` pour enregistrer votre webhook auprès du serveur API Kubernetes.

**mutating-webhook.yaml** :

```yaml
apiVersion: admissionregistration.k8s.io/v1
kind: MutatingWebhookConfiguration
metadata:
  name: example-mutating-webhook
webhooks:
  - name: mutating.webhook.example.com
    clientConfig:
      service:
        name: admission-webhook
        namespace: default
        path: "/mutate"
      caBundle: <base64-encoded-ca-cert>
    rules:
      - operations: ["CREATE"]
        apiGroups: [""]
        apiVersions: ["v1"]
        resources: ["pods"]
    admissionReviewVersions: ["v1"]
    sideEffects: None
    timeoutSeconds: 5
```

**validating-webhook.yaml** :

```yaml
apiVersion: admissionregistration.k8s.io/v1
kind: ValidatingWebhookConfiguration
metadata:
  name: example-validating-webhook
webhooks:
  - name: validating.webhook.example.com
    clientConfig:
      service:
        name: admission-webhook
        namespace: default
        path: "/validate"
      caBundle: <base64-encoded-ca-cert>
    rules:
      - operations: ["CREATE"]
        apiGroups: [""]
        apiVersions: ["v1"]
        resources: ["pods"]
    admissionReviewVersions: ["v1"]
    sideEffects: None
    timeoutSeconds: 5
```

Déployez les configurations des webhooks :

```sh
kubectl apply -f mutating-webhook.yaml
kubectl apply -f validating-webhook.yaml
```

### Conclusion

Les Dynamic Admission Controllers permettent de personnaliser et d'étendre le comportement par défaut de Kubernetes en interceptant et en modifiant ou en validant les requêtes API. Ils offrent une flexibilité et une puissance accrues pour la gestion des ressources Kubernetes, en particulier dans des environnements où des politiques de sécurité et des contrôles de conformité stricts sont nécessaires.