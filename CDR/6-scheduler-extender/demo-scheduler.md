Pour configurer un scheduler extender dans Kubernetes, vous devez suivre plusieurs étapes. Voici un guide complet, incluant la création d'un serveur d'API personnalisé pour le scheduler extender, la configuration du scheduler, et le déploiement de l'extender dans Kubernetes.

### Étapes Complètes pour Configurer un Scheduler Extender

1. **Écrire le Serveur d'API Personnalisé**
2. **Construire et Pousser l'Image Docker**
3. **Déployer le Serveur d'API Personnalisé dans Kubernetes**
4. **Configurer le Scheduler pour Utiliser l'Extender**
5. **Redémarrer le Scheduler avec la Nouvelle Configuration**

### 1. Écrire le Serveur d'API Personnalisé

Créez un serveur d'API personnalisé qui interagira avec le scheduler Kubernetes.

**Fichier `scheduler-extender.py`:**

```python
from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/filter', methods=['POST'])
def filter_nodes():
    data = request.get_json()
    nodes = data['nodes']['items']
    # Implémentez ici votre logique de filtrage
    filtered_nodes = [node for node in nodes if node_meets_criteria(node)]
    return jsonify({'nodes': {'items': filtered_nodes}})

@app.route('/prioritize', methods=['POST'])
def prioritize_nodes():
    data = request.get_json()
    nodes = data['nodes']['items']
    # Implémentez ici votre logique de priorisation
    scores = [{'name': node['metadata']['name'], 'score': calculate_score(node)} for node in nodes]
    return jsonify({'scores': scores})

def node_meets_criteria(node):
    # Exemple de critère de filtrage
    return 'example-label' in node['metadata']['labels']

def calculate_score(node):
    # Exemple de calcul de score
    return int(node['metadata']['name'][-1])  # Score basé sur le dernier caractère du nom du nœud

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=12345)
```

### 2. Construire et Pousser l'Image Docker

Créez un Dockerfile pour votre serveur d'API.

**Fichier `Dockerfile`:**

```Dockerfile
FROM python:3.8-slim
COPY scheduler-extender.py /scheduler-extender.py
RUN pip install flask
CMD ["python", "/scheduler-extender.py"]
```

Construisez et poussez l'image Docker.

```sh
docker build -t your-repo/scheduler-extender .
docker push your-repo/scheduler-extender
```

### 3. Déployer le Serveur d'API Personnalisé dans Kubernetes

Créez les fichiers de déploiement Kubernetes pour votre serveur d'API.

**Fichier `scheduler-extender-deployment.yaml`:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: scheduler-extender
  namespace: default
spec:
  replicas: 1
  selector:
    matchLabels:
      app: scheduler-extender
  template:
    metadata:
      labels:
        app: scheduler-extender
    spec:
      containers:
        - name: scheduler-extender
          image: your-repo/scheduler-extender
          ports:
            - containerPort: 12345
---
apiVersion: v1
kind: Service
metadata:
  name: scheduler-extender
  namespace: default
spec:
  ports:
    - port: 12345
      targetPort: 12345
  selector:
    app: scheduler-extender
```

Déployez le serveur d'API personnalisé.

```sh
kubectl apply -f scheduler-extender-deployment.yaml
```

### 4. Configurer le Scheduler pour Utiliser l'Extender

Créez un fichier de configuration pour le scheduler incluant l'extender.

**Fichier `scheduler-config.yaml`:**

```yaml
apiVersion: kubescheduler.config.k8s.io/v1
kind: KubeSchedulerConfiguration
clientConnection:
  kubeconfig: "/etc/kubernetes/scheduler.conf"
leaderElection:
  leaderElect: true
extenders:
  - urlPrefix: "http://scheduler-extender.default.svc.cluster.local:12345"
    filterVerb: "filter"
    prioritizeVerb: "prioritize"
    weight: 1
    nodeCacheCapable: false
    managedResources:
      - name: "example.com/custom-resource"
        ignoredByScheduler: true
```

### 5. Redémarrer le Scheduler avec la Nouvelle Configuration

Déployez la nouvelle configuration du scheduler. Selon la méthode de déploiement de votre cluster Kubernetes (par exemple, kubeadm, kubelet, ou des opérateurs spécifiques au cloud), vous devrez redémarrer le scheduler avec cette nouvelle configuration.

Si vous utilisez kubeadm, vous devrez mettre à jour le fichier de déploiement du scheduler pour utiliser le nouveau fichier de configuration.

**Exemple de mise à jour du manifest kube-scheduler**:

1. **Éditer le manifest du kube-scheduler**:

```sh
kubectl -n kube-system edit deployment kube-scheduler
```

2. **Modifier la commande pour utiliser le fichier de configuration**:

Ajoutez ou modifiez les arguments de démarrage pour inclure le chemin vers votre fichier de configuration personnalisé.

```yaml
spec:
  containers:
    - command:
        - kube-scheduler
        - --config=/etc/kubernetes/scheduler-config.yaml
```

3. **Monter le fichier de configuration dans le pod**:

Assurez-vous que le fichier de configuration est monté dans le pod.

```yaml
    volumeMounts:
    - mountPath: /etc/kubernetes/scheduler-config.yaml
      name: scheduler-config
      subPath: scheduler-config.yaml
  volumes:
  - name: scheduler-config
    configMap:
      name: scheduler-config
```

4. **Créer le ConfigMap pour le fichier de configuration**:

Créez un ConfigMap pour le fichier de configuration.

```sh
kubectl create configmap scheduler-config --from-file=scheduler-config.yaml
```

### Conclusion

Vous avez maintenant configuré un scheduler extender dans Kubernetes. Le scheduler extender permet de personnaliser le comportement du scheduler de Kubernetes en ajoutant des fonctionnalités supplémentaires pour le filtrage et la priorisation des nœuds lors de la planification des pods. Cette méthode est puissante pour implémenter des politiques de planification spécifiques à votre environnement.