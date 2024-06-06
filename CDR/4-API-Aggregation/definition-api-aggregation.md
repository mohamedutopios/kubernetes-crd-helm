Une API Aggregation dans Kubernetes est une fonctionnalité permettant d'étendre les API Kubernetes par défaut avec des API supplémentaires. Cela se fait en intégrant des API externes comme s'il s'agissait d'extensions natives du serveur API Kubernetes. Cette fonctionnalité est particulièrement utile pour ajouter de nouvelles capacités sans avoir à modifier le code source du serveur API Kubernetes lui-même.

### Fonctionnement de l'API Aggregation

L'API Aggregation fonctionne en ajoutant des services supplémentaires qui se comportent comme des API Kubernetes natives. Voici les étapes générales :

1. **Déployer l'API serveur additionnel** : Vous créez et déployez un serveur d'API additionnel en tant que service Kubernetes.
2. **Configurer l'APIService** : Une ressource `APIService` est utilisée pour enregistrer cette API supplémentaire auprès du serveur API Kubernetes.
3. **Trafic routé vers l'API serveur additionnel** : Les requêtes vers les groupes de l'API additionnelle sont redirigées vers le service correspondant, permettant à ces API de fonctionner comme si elles faisaient partie intégrante de l'API Kubernetes.

### Cas d'utilisation concret : Extensibilité avec Prometheus Adapter

Un cas d'utilisation concret de l'API Aggregation est l'extension du serveur API Kubernetes avec un adapter Prometheus pour permettre des métriques personnalisées.

#### Objectif
Permettre à Kubernetes de récupérer des métriques personnalisées depuis Prometheus et les utiliser pour le scaling automatique des pods (Horizontal Pod Autoscaler).

#### Étapes

1. **Déployer Prometheus et l'adapter Prometheus** :
   - Prometheus doit être configuré et déployé dans le cluster Kubernetes pour collecter les métriques.
   - Déployer un adapter Prometheus, qui expose les métriques Prometheus au serveur API Kubernetes via une API compatible avec le Custom Metrics API.

2. **Configurer l'APIService** :
   - Créer une ressource `APIService` pour enregistrer l'API de l'adapter Prometheus avec le serveur API Kubernetes.
   - Exemple de fichier YAML pour l'APIService :

     ```yaml
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

3. **Utiliser les métriques personnalisées pour le scaling** :
   - Configurer un Horizontal Pod Autoscaler (HPA) pour utiliser les métriques personnalisées exposées par l'adapter Prometheus.
   - Exemple de fichier YAML pour un HPA utilisant une métrique personnalisée :

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
             name: custom_metric
           target:
             type: AverageValue
             averageValue: 10
     ```

### Conclusion

L'API Aggregation de Kubernetes permet d'étendre la fonctionnalité du serveur API en intégrant des API externes de manière transparente. L'exemple de l'adapter Prometheus montre comment cette fonctionnalité peut être utilisée pour enrichir Kubernetes avec des métriques personnalisées, permettant des actions avancées comme le scaling automatique basé sur des métriques spécifiques à l'application.