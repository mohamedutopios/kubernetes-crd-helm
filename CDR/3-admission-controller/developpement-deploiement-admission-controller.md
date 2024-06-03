Développer et déployer un Admission Controller en utilisant Java implique plusieurs étapes similaires à celles décrites pour Go, mais en utilisant des outils et des bibliothèques Java. Voici un guide détaillé pour créer et déployer un Admission Controller en Java.

### Étapes pour le Développement et le Déploiement d'un Admission Controller en Java

#### 1. Développement de l'Admission Controller

##### a. Écrire le code de l'Admission Controller

1. **Choisir un framework Java** : Spring Boot est un bon choix pour créer des applications web en Java.

2. **Créer une application Spring Boot** : Créez une nouvelle application Spring Boot et ajoutez les dépendances nécessaires.

##### b. Configuration du projet Maven

Voici un exemple de fichier `pom.xml` pour configurer votre projet avec Spring Boot et les dépendances nécessaires :

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>admission-controller</artifactId>
    <version>1.0.0</version>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.6.2</version>
        <relativePath/>
    </parent>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

##### c. Écrire le contrôleur Admission Controller

Voici un exemple de code Java pour un Mutating Admission Controller utilisant Spring Boot :

```java
package com.example.admissioncontroller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

@SpringBootApplication
public class AdmissionControllerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdmissionControllerApplication.class, args);
    }
}

@RestController
class AdmissionController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/mutate")
    public JsonNode mutate(@RequestBody JsonNode admissionReviewRequest) throws Exception {
        JsonNode request = admissionReviewRequest.get("request");
        JsonNode object = request.get("object");

        // Modifiez l'objet ici (par exemple, ajoutez une annotation)
        ((ObjectNode) object.get("metadata").get("annotations")).put("mutated", "true");

        JsonNode patch = objectMapper.createObjectNode().putArray("patch")
                .addObject()
                .put("op", "add")
                .put("path", "/metadata/annotations/mutated")
                .put("value", "true");

        String patchBase64 = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(patch));

        ObjectNode response = objectMapper.createObjectNode();
        response.put("uid", request.get("uid").asText());
        response.put("allowed", true);
        response.put("patchType", "JSONPatch");
        response.put("patch", patchBase64);

        ObjectNode admissionReviewResponse = objectMapper.createObjectNode();
        admissionReviewResponse.set("response", response);

        return admissionReviewResponse;
    }
}
```

#### 2. Création des Certificats

Pour sécuriser la communication entre Kubernetes et votre Admission Controller, vous aurez besoin de certificats TLS. Vous pouvez générer des certificats auto-signés comme suit :

```sh
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes -subj "/CN=example.com"
```

#### 3. Déploiement de l'Admission Controller dans Kubernetes

##### a. Créer un Deployment et un Service

Créez un Deployment et un Service pour votre Admission Controller.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: admission-controller
  namespace: default
spec:
  replicas: 1
  selector:
    matchLabels:
      app: admission-controller
  template:
    metadata:
      labels:
        app: admission-controller
    spec:
      containers:
      - name: admission-controller
        image: <your-image>
        ports:
        - containerPort: 8443
        volumeMounts:
        - name: tls-certs
          mountPath: "/tls"
          readOnly: true
      volumes:
      - name: tls-certs
        secret:
          secretName: admission-controller-tls
---
apiVersion: v1
kind: Service
metadata:
  name: admission-controller
  namespace: default
spec:
  ports:
  - port: 443
    targetPort: 8443
  selector:
    app: admission-controller
```

##### b. Créer le Secret contenant les certificats TLS

```sh
kubectl create secret generic admission-controller-tls --from-file=key.pem --from-file=cert.pem -n default
```

##### c. Configurer l'APIService

Créez un MutatingWebhookConfiguration (ou ValidatingWebhookConfiguration) pour enregistrer votre Admission Controller avec Kubernetes.

```yaml
apiVersion: admissionregistration.k8s.io/v1
kind: MutatingWebhookConfiguration
metadata:
  name: example-mutating-webhook
webhooks:
  - name: example.mutating.webhook.com
    clientConfig:
      service:
        name: admission-controller
        namespace: default
        path: "/mutate"
      caBundle: <base64-encoded-ca-cert>
    rules:
      - operations: ["CREATE", "UPDATE"]
        apiGroups: [""]
        apiVersions: ["v1"]
        resources: ["pods"]
    admissionReviewVersions: ["v1", "v1beta1"]
    sideEffects: None
```

### Conclusion

En suivant ces étapes, vous pouvez développer et déployer un Admission Controller en Java dans Kubernetes. Les Admission Controllers sont des outils puissants pour appliquer des politiques et des contrôles supplémentaires dans votre cluster Kubernetes. Assurez-vous de bien tester votre Admission Controller avant de le déployer en production pour éviter toute perturbation dans votre cluster.