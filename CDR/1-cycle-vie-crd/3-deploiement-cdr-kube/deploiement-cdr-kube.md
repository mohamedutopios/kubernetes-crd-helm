Le déploiement de Custom Resource Definitions (CRDs) dans un cluster Kubernetes est une étape clé pour étendre les capacités de Kubernetes avec des ressources personnalisées. Voici comment vous pouvez déployer des CRDs, que vous ayez utilisé Operator SDK ou Kubebuilder pour créer votre opérateur.

### Étapes Générales pour Déployer des CRDs

1. **Générer les Manifests CRD**
2. **Appliquer les Manifests CRD dans le Cluster**
3. **Vérifier l'Installation des CRDs**

### Étapes Détailées

#### 1. Générer les Manifests CRD

Si vous avez suivi les étapes de création de l'API avec Operator SDK ou Kubebuilder, vous avez déjà généré les manifests pour les CRDs. Ces manifests se trouvent généralement dans le répertoire `config/crd/bases`.

##### Avec Operator SDK
Les manifests CRD sont générés avec la commande suivante :

```sh
make manifests
```

##### Avec Kubebuilder
Les manifests CRD sont générés de manière similaire :

```sh
make manifests
```

Cela générera les fichiers YAML des CRDs dans le répertoire `config/crd/bases`.

#### 2. Appliquer les Manifests CRD dans le Cluster

Pour déployer les CRDs dans votre cluster Kubernetes, vous devez appliquer les fichiers YAML générés. Assurez-vous que `kubectl` est configuré pour pointer vers le bon cluster.

```sh
kubectl apply -f config/crd/bases
```

Vous pouvez aussi appliquer un fichier CRD spécifique si nécessaire :

```sh
kubectl apply -f config/crd/bases/<your_crd>.yaml
```

#### 3. Vérifier l'Installation des CRDs

Pour vous assurer que les CRDs ont été correctement installées, vous pouvez utiliser la commande `kubectl get crds`.

```sh
kubectl get crds
```

Cela affichera une liste de toutes les CRDs installées dans le cluster.

### Exemple Pratique

Imaginons que vous avez créé une CRD pour une ressource personnalisée `Memcached`.

1. **Générer les Manifests**

   ```sh
   make manifests
   ```

   Cela génère un fichier YAML pour `Memcached` dans `config/crd/bases/cache.example.com_memcacheds.yaml`.

2. **Appliquer les Manifests CRD**

   ```sh
   kubectl apply -f config/crd/bases/cache.example.com_memcacheds.yaml
   ```

3. **Vérifier l'Installation**

   ```sh
   kubectl get crds
   ```

   Vous devriez voir une entrée pour `memcacheds.cache.example.com`.

### Déploiement Automatique avec `make install`

Si vous avez un Makefile configuré (comme souvent généré par Operator SDK ou Kubebuilder), vous pouvez simplifier le processus en utilisant `make install`.

```sh
make install
```

Cette commande applique tous les manifests nécessaires, y compris les CRDs, à votre cluster.

### Conclusion

Déployer des CRDs dans un cluster Kubernetes est une étape essentielle pour utiliser des ressources personnalisées. En suivant les étapes ci-dessus, vous pouvez facilement générer, appliquer et vérifier les CRDs dans votre cluster. Que vous utilisiez Operator SDK ou Kubebuilder, le processus est similaire et bien pris en charge par ces outils.