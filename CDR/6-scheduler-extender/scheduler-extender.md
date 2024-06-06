Le `scheduler-extender` dans Kubernetes est un mécanisme permettant de personnaliser le comportement du planificateur (scheduler) Kubernetes en ajoutant des fonctionnalités supplémentaires ou en modifiant les décisions de planification prises par le planificateur par défaut. Cela permet aux utilisateurs de Kubernetes de créer des règles de planification plus complexes ou spécifiques à leurs besoins sans avoir à modifier le code source du planificateur principal de Kubernetes.

### Fonctionnement du Scheduler Extender

Un scheduler extender est essentiellement un service externe qui interagit avec le scheduler de Kubernetes via des appels HTTP. Lorsqu'une décision de planification est nécessaire, le scheduler principal peut déléguer certaines parties de la décision au scheduler extender. Voici les étapes générales de ce fonctionnement :

1. **Configuration** : Le scheduler est configuré pour utiliser un ou plusieurs extenders en spécifiant leur(s) URL(s) et les méthodes HTTP qu'ils supportent.
2. **Filtrage (Filter)** : Lorsque le scheduler principal détermine la liste des nœuds candidats pour un pod, il peut envoyer cette liste au scheduler extender pour un filtrage supplémentaire. Le scheduler extender peut alors affiner cette liste en fonction de critères supplémentaires.
3. **Priorisation (Prioritize)** : Le scheduler principal peut également demander au scheduler extender d'ordonner les nœuds candidats selon des critères de priorité supplémentaires.
4. **Décision Finale** : Le scheduler principal utilise les résultats fournis par le scheduler extender pour prendre la décision finale de placement du pod.

### Cas d'utilisation

Les scheduler extenders sont utilisés pour des scénarios où des règles de planification plus complexes sont nécessaires. Voici quelques exemples concrets de cas d'utilisation :

1. **Affinité et Anti-affinité Personnalisées** : Implémenter des règles d'affinité ou d'anti-affinité plus complexes que celles supportées nativement par Kubernetes.
2. **Restrictions de Ressources** : Appliquer des restrictions de ressources spécifiques, comme des limites personnalisées sur l'utilisation du CPU ou de la mémoire par des pods sur certains nœuds.
3. **Conscience du Coût** : Planifier les pods en tenant compte des coûts d'utilisation des nœuds, par exemple en évitant les nœuds plus coûteux sauf en cas de nécessité.
4. **Intégration avec des Services Externes** : Intégrer des politiques de planification avec des services externes, comme des systèmes de gestion de charge ou des plateformes cloud spécifiques.

### Exemple de Configuration d'un Scheduler Extender

Voici un exemple de configuration d'un scheduler extender dans Kubernetes :

1. **Définir l'extender dans la configuration du scheduler** :

**scheduler-config.yaml** :

```yaml
apiVersion: kubescheduler.config.k8s.io/v1
kind: KubeSchedulerConfiguration
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

2. **Déployer le Scheduler Extender** :

Le scheduler extender est un service web qui écoute les requêtes du scheduler. Voici un exemple simplifié en Python utilisant Flask :

**scheduler-extender.py** :

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

**Dockerfile** :

```Dockerfile
FROM python:3.8-slim
COPY scheduler-extender.py /scheduler-extender.py
RUN pip install flask
CMD ["python", "/scheduler-extender.py"]
```

**Déploiement du Scheduler Extender dans Kubernetes** :

Créez un déploiement pour le scheduler extender :

**scheduler-extender-deployment.yaml** :

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
```

Déployez-le :

```sh
kubectl apply -f scheduler-extender-deployment.yaml
```

### Conclusion

Le `scheduler-extender` dans Kubernetes permet de personnaliser la logique de planification en utilisant des services externes. Cela permet de répondre à des besoins spécifiques de planification qui ne sont pas couverts par les fonctionnalités natives de Kubernetes, offrant ainsi une flexibilité et une extensibilité accrues dans la gestion des workloads Kubernetes.