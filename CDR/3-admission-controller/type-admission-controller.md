Les Admission Controllers dans Kubernetes se divisent principalement en deux types : Mutating Admission Controllers et Validating Admission Controllers. Chacun de ces types a un rôle spécifique dans le traitement des requêtes envoyées au serveur API de Kubernetes.

### 1. Mutating Admission Controllers

Les Mutating Admission Controllers sont responsables de la modification des objets envoyés au serveur API avant qu'ils ne soient persistés dans etcd. Ces contrôleurs peuvent ajouter, modifier ou supprimer des champs dans les objets.

#### Fonctionnalités et Utilisations :
- **Ajout de labels ou d'annotations** : Ils peuvent ajouter automatiquement des labels ou des annotations à des ressources nouvellement créées.
- **Définition de valeurs par défaut** : Ils peuvent définir des valeurs par défaut pour les champs non spécifiés par l'utilisateur.
- **Injection de sidecars** : Ils peuvent ajouter automatiquement des conteneurs sidecar dans les pods (par exemple, pour la gestion des logs ou la sécurité).

#### Exemple :
Un exemple courant de Mutating Admission Controller est l'injection automatique de sidecars, comme les conteneurs Envoy pour les proxys de service mesh.

```yaml
apiVersion: admissionregistration.k8s.io/v1
kind: MutatingWebhookConfiguration
metadata:
  name: example-mutating-webhook
webhooks:
  - name: example.mutating.webhook.com
    clientConfig:
      service:
        name: example-service
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

### 2. Validating Admission Controllers

Les Validating Admission Controllers valident les objets sans les modifier. Ils vérifient que les objets respectent certaines conditions avant d'être persistés dans etcd. Si une validation échoue, la requête est rejetée.

#### Fonctionnalités et Utilisations :
- **Contrôle de conformité** : Ils s'assurent que les objets créés respectent les politiques de sécurité et les contraintes de l'organisation.
- **Validation des champs** : Ils vérifient que les champs des objets contiennent des valeurs valides et appropriées.
- **Enforcement des politiques** : Ils peuvent imposer des règles spécifiques, comme l'utilisation de certaines images de conteneurs ou des configurations réseau.

#### Exemple :
Un exemple de Validating Admission Controller est la vérification des spécifications de déploiement pour s'assurer qu'elles respectent les politiques de l'organisation.

```yaml
apiVersion: admissionregistration.k8s.io/v1
kind: ValidatingWebhookConfiguration
metadata:
  name: example-validating-webhook
webhooks:
  - name: example.validating.webhook.com
    clientConfig:
      service:
        name: example-service
        namespace: default
        path: "/validate"
      caBundle: <base64-encoded-ca-cert>
    rules:
      - operations: ["CREATE", "UPDATE"]
        apiGroups: [""]
        apiVersions: ["v1"]
        resources: ["pods"]
    admissionReviewVersions: ["v1", "v1beta1"]
    sideEffects: None
```

### Exemple Concret

Pour illustrer les différences entre Mutating et Validating Admission Controllers, considérons un scénario où nous voulons gérer les pods dans un cluster Kubernetes.

#### Scénario :

1. **Mutating Admission Controller** :
   - **Objectif** : Ajouter automatiquement une annotation de sécurité à chaque pod créé.
   - **Configuration** : Créer un webhook de mutation qui intercepte les requêtes de création de pod et ajoute l'annotation.

   ```yaml
   apiVersion: admissionregistration.k8s.io/v1
   kind: MutatingWebhookConfiguration
   metadata:
     name: security-annotation-webhook
   webhooks:
     - name: security.mutating.webhook.com
       clientConfig:
         service:
           name: security-webhook-service
           namespace: default
           path: "/mutate"
         caBundle: <base64-encoded-ca-cert>
       rules:
         - operations: ["CREATE"]
           apiGroups: [""]
           apiVersions: ["v1"]
           resources: ["pods"]
       admissionReviewVersions: ["v1", "v1beta1"]
       sideEffects: None
   ```

2. **Validating Admission Controller** :
   - **Objectif** : Valider que chaque pod respecte une politique de sécurité spécifique (par exemple, ne pas utiliser l'utilisateur root).
   - **Configuration** : Créer un webhook de validation qui intercepte les requêtes de création et de mise à jour de pod et rejette celles qui n'adhèrent pas à la politique.

   ```yaml
   apiVersion: admissionregistration.k8s.io/v1
   kind: ValidatingWebhookConfiguration
   metadata:
     name: security-policy-webhook
   webhooks:
     - name: security.validating.webhook.com
       clientConfig:
         service:
           name: security-webhook-service
           namespace: default
           path: "/validate"
         caBundle: <base64-encoded-ca-cert>
       rules:
         - operations: ["CREATE", "UPDATE"]
           apiGroups: [""]
           apiVersions: ["v1"]
           resources: ["pods"]
       admissionReviewVersions: ["v1", "v1beta1"]
       sideEffects: None
   ```
