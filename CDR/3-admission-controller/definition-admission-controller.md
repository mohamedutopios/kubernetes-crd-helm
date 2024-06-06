### Admission Controllers dans Kubernetes

#### Qu'est-ce qu'un Admission Controller ?

Les Admission Controllers sont des plugins au sein de Kubernetes qui interceptent les requêtes au serveur d'API après leur authentification et autorisation, mais avant qu'elles ne soient persistées dans etcd (la base de données de Kubernetes). Ces plugins peuvent modifier ou rejeter des requêtes. Ils jouent un rôle crucial pour appliquer des politiques de sécurité, valider des configurations et implémenter des contraintes spécifiques.

#### Fonctionnement des Admission Controllers

1. **Authentification** : La requête est authentifiée pour vérifier l'identité de l'utilisateur ou du service.
2. **Autorisation** : La requête est autorisée pour s'assurer que l'utilisateur ou le service a les permissions nécessaires pour effectuer l'action demandée.
3. **Admission Control** : Les Admission Controllers interceptent la requête. Ils peuvent la modifier ou la rejeter en fonction des politiques définies.

Il existe deux types principaux d'Admission Controllers :

1. **Mutating Admission Controllers** : Ils peuvent modifier les objets avant qu'ils ne soient persistés.
2. **Validating Admission Controllers** : Ils valident les objets et peuvent rejeter les requêtes non conformes.

#### Exemple d'Admission Controllers courants

- **NamespaceLifecycle** : Empêche la création d'objets dans des namespaces qui sont en cours de suppression.
- **ResourceQuota** : Applique des quotas de ressources pour limiter la quantité de ressources qu'un namespace peut consommer.
- **PodSecurityPolicy** : Contrôle les spécifications des Pods pour des raisons de sécurité.
- **DefaultStorageClass** : Assigne une classe de stockage par défaut aux PersistentVolumeClaims qui n'en spécifient pas.

#### Exemple complet avec un Admission Controller : PodSecurityPolicy

##### Étapes pour configurer un PodSecurityPolicy

1. **Activer le contrôleur PodSecurityPolicy** : Vous devez activer le contrôleur dans la configuration du serveur API de Kubernetes (kube-apiserver).

2. **Créer une PodSecurityPolicy** :

```yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: example-psp
spec:
  privileged: false  # Ne pas permettre des pods privilégiés
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - 'emptyDir'
  hostNetwork: false
  hostPorts:
    - min: 0
      max: 65535
  seLinux:
    rule: RunAsAny
  runAsUser:
    rule: MustRunAsNonRoot
  supplementalGroups:
    rule: RunAsAny
  fsGroup:
    rule: RunAsAny
```

3. **Créer un rôle et une liaison de rôle pour utiliser cette PSP** :

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: psp-user
  namespace: default
rules:
- apiGroups:
  - policy
  resourceNames:
  - example-psp
  resources:
  - podsecuritypolicies
  verbs:
  - use
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: use-psp
  namespace: default
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: psp-user
subjects:
- kind: User
  name: your-username
  apiGroup: rbac.authorization.k8s.io
```

4. **Appliquer ces configurations** :

```bash
kubectl apply -f pod-security-policy.yaml
kubectl apply -f role.yaml
kubectl apply -f role-binding.yaml
```

5. **Tester la configuration** : Essayez de créer un pod qui viole les politiques définies dans la PodSecurityPolicy pour vérifier que l'Admission Controller bloque correctement les pods non conformes.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: privileged-pod
spec:
  containers:
  - name: nginx
    image: nginx
    securityContext:
      privileged: true
```

```bash
kubectl apply -f test-pod.yaml
```

Vous devriez recevoir une erreur indiquant que la création du pod est interdite car il ne respecte pas la PodSecurityPolicy.

Les Admission Controllers sont essentiels pour renforcer la sécurité et la conformité des déploiements dans Kubernetes. Ils offrent une couche supplémentaire de contrôle et permettent d'appliquer des politiques complexes qui répondent aux exigences spécifiques de sécurité et de gestion des ressources.