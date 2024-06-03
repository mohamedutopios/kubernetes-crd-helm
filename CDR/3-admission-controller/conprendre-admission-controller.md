Les Admission Controllers sont des plugins dans Kubernetes qui interceptent les requêtes envoyées au serveur API Kubernetes avant que les objets persistent dans l'etcd. Ils permettent de modifier ou de rejeter des requêtes en fonction de règles définies. Ils jouent un rôle crucial dans la validation, la modification et la sécurité des objets créés dans le cluster Kubernetes.

Voici une vue d'ensemble détaillée des Admission Controllers dans Kubernetes :

### 1. Rôle et Fonctionnement

#### a. **Admission Control Flow**
Lorsqu'une requête est envoyée au serveur API Kubernetes, elle passe par plusieurs étapes :
1. **Authentification** : Vérifie l'identité de l'utilisateur.
2. **Autorisation** : Vérifie les permissions de l'utilisateur.
3. **Admission Controllers** : Interceptent et potentiellement modifient ou rejettent la requête.
4. **Validation** : Assure que l'objet respecte les schémas et les règles définies.
5. **Persisting** : L'objet est enregistré dans l'etcd si toutes les étapes précédentes sont réussies.

#### b. **Types d'Admission Controllers**
Il existe deux types principaux d'Admission Controllers :
- **Mutating Admission Controllers** : Ils peuvent modifier les objets en cours de création ou de mise à jour.
- **Validating Admission Controllers** : Ils valident les objets sans les modifier, et peuvent rejeter les requêtes qui ne respectent pas certaines conditions.

### 2. Admission Controllers Courants

#### a. **NamespaceLifecycle**
Il empêche la création d'objets dans des namespaces qui sont en cours de suppression.

#### b. **LimitRanger**
Il applique des limites de ressources (comme les CPU et la mémoire) aux objets dans un namespace.

#### c. **ResourceQuota**
Il assure que les quotas de ressources définis pour un namespace ne sont pas dépassés.

#### d. **PodSecurityPolicy**
Il impose des règles de sécurité sur les pods, comme l'utilisation de volumes, les utilisateurs, etc.

#### e. **DefaultStorageClass**
Il assigne une classe de stockage par défaut aux PersistentVolumeClaims qui n'en spécifient pas.

### 3. Activation et Configuration

Les Admission Controllers sont activés et configurés dans le fichier de configuration du serveur API (kube-apiserver). Vous pouvez les activer en utilisant le paramètre `--enable-admission-plugins` et les configurer avec `--admission-control-config-file`.

Exemple de configuration dans le fichier kube-apiserver :

```yaml
apiVersion: kubeadm.k8s.io/v1beta2
kind: ClusterConfiguration
apiServer:
  extraArgs:
    enable-admission-plugins: "NamespaceLifecycle,LimitRanger,ResourceQuota,PodSecurityPolicy,DefaultStorageClass"
```

### 4. Webhook Admission Controllers

En plus des Admission Controllers intégrés, Kubernetes permet de créer des Admission Controllers personnalisés en utilisant des webhooks. Il existe deux types de webhooks :

#### a. **Mutating Admission Webhooks**
Ils permettent de modifier les objets en utilisant des règles personnalisées définies par les utilisateurs.

#### b. **Validating Admission Webhooks**
Ils valident les objets sans les modifier et peuvent rejeter des requêtes qui ne respectent pas les règles définies.

### Exemple de Configuration d'un Webhook

Voici un exemple de configuration pour un Validating Admission Webhook :

1. **Déploiement du webhook** : Déployez un service qui gère les requêtes du webhook.

2. **Configuration du webhook** : Créez une ressource ValidatingWebhookConfiguration pour indiquer au serveur API où envoyer les requêtes d'admission.

```yaml
apiVersion: admissionregistration.k8s.io/v1
kind: ValidatingWebhookConfiguration
metadata:
  name: example-webhook
webhooks:
  - name: example.com
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


Pour des détails supplémentaires et des exemples spécifiques, vous pouvez consulter la documentation officielle de Kubernetes sur les Admission Controllers [ici](https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/).