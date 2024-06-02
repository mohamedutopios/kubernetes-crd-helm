Voici le détail du programme de formation Kubernetes :

### Les Extensions (CRDs, Aggregation Layer, Admission Controllers...)
**Durée : 1.5 jours**

1. **Cycle de vie d’une CRD (Custom Resource Definition)**
   - Introduction aux CRDs
   - Création et gestion des CRDs
   - Déploiement de CRDs dans un cluster Kubernetes
   - Meilleures pratiques et cas d'utilisation

2. **Implémentation d’un Opérateur Kubernetes**
   - Concepts et avantages des opérateurs Kubernetes
   - Étapes pour développer un opérateur
   - Utilisation des frameworks tels que Operator SDK ou Kubebuilder
   - Cas pratiques : développement d'un opérateur simple

3. **Implémentation d’un Admission Controller**
   - Comprendre les Admission Controllers dans Kubernetes
   - Types d'Admission Controllers : Mutating et Validating
   - Développement et déploiement d'un Admission Controller
   - Utilisation des Webhooks pour l'Admission Control

### Helm avancé
**Durée : 0.5 jour**

1. **App/lib charts, subcharts, dependencies**
   - Différence entre les app charts et les library charts
   - Utilisation des subcharts pour organiser et réutiliser les composants
   - Gestion des dépendances entre charts
   - Définition et utilisation des requirements.yaml

2. **Pre & post actions/hooks**
   - Introduction aux hooks dans Helm
   - Création et utilisation des hooks pour automatiser les actions avant et après les déploiements
   - Cas d'utilisation des hooks pour des tâches comme les migrations de base de données, les vérifications de système, etc.

3. **Tester une chart**
   - Outils et techniques pour tester les charts Helm
   - Utilisation de Helm test pour valider les déploiements
   - Bonnes pratiques pour écrire des tests Helm

4. **Troubleshooting Helm**
   - Identification et résolution des problèmes courants avec Helm
   - Utilisation des logs et des outils de debugging
   - Stratégies pour résoudre les conflits de version et les erreurs de déploiement

Cette formation permet de maîtriser les aspects avancés de Kubernetes, en particulier les extensions et l'utilisation avancée de Helm, afin d'améliorer la gestion et l'automatisation des déploiements dans un cluster Kubernetes.