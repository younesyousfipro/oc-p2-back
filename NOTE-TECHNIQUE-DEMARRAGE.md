# Note technique — démarrage du projet `etudiant-backend`

**Auteur :** Younes Yousfi
**Date :** 11 septembre 2026
**Objet :** Incidents rencontrés à la première exécution du projet fourni, causes identifiées et correctifs appliqués.

---

## Résumé

Le projet `etudiant-backend` tel que fourni ne démarrait pas et ses tests d'intégration ne passaient pas sur un poste conforme aux prérequis du README (JDK 21, Maven 3.9+, Docker Desktop).

Quatre incidents indépendants ont été identifiés et corrigés. Trois relèvent du projet lui-même, le quatrième de la configuration de l'éditeur.

| # | Incident | Origine | Statut |
|---|---|---|---|
| 1 | Image MySQL `latest` incompatible avec le volume de données | Projet (`compose.yaml`) | ✅ Corrigé |
| 2 | Testcontainers ne trouve pas le daemon Docker | Projet (`pom.xml`) | ✅ Corrigé |
| 3 | Image MySQL `latest` refusée par la configuration Testcontainers | Projet (test) | ✅ Corrigé |
| 4 | Classes compilées écrasées par l'éditeur | Poste de travail (VS Code) | ✅ Corrigé |

**État final :** `mvn clean test` → 6 tests, 0 échec. `mvn spring-boot:run` → démarrage en 2,7 s, `/actuator/health` à `UP`.

---

## Environnement de test

| Composant | Version |
|---|---|
| macOS | Darwin 25.6.0 (arm64) |
| JDK | Temurin 21.0.12.1+1 (LTS) |
| Maven | 3.9.9 |
| Docker Engine | 29.3.1 (`MinAPIVersion` 1.40) |
| Spring Boot | 3.5.5 |
| VS Code — extension Java | `redhat.java` 1.56.0 |

---

## Incident 1 — Le backend ne démarre pas

### Symptôme

```
[ERROR] Failed to execute goal org.springframework.boot:spring-boot-maven-plugin:3.5.5:run
        Process terminated with exit code: 1
[INFO] Total time:  02:04 min
```

### Cause

`compose.yaml` déclarait `image: 'mysql:latest'`. Ce tag flottant pointait au moment du test sur **MySQL 26.7.0**, alors que le volume Docker `oc-p2-back_db_data` avait été initialisé par une version **8.0.46**.

```
[ERROR] [MY-014060] Invalid MySQL server upgrade: Cannot upgrade from 80046 to 260700.
        The source and target must be in the same compatibility lineage.
[ERROR] [MY-010020] Data Dictionary initialization failed.
```

MySQL n'autorise une migration du dictionnaire de données que depuis la LTS immédiatement précédente. Le saut 8.0 → 26.7 est refusé et le serveur avorte.

### Enchaînement complet

Le message Maven ne mentionne pas MySQL. La chaîne réelle est la suivante :

```
tag latest → MySQL 26.7.0 sur un volume 8.0.46
  → refus de migration du dictionnaire → container en échec (exit 1)
    → aucun port en écoute
      → sonde de disponibilité Spring en échec pendant 120 s
        → sortie de la JVM en code 1
          → BUILD FAILURE
```

Les **2 min 04 s** affichées correspondent à la valeur par défaut de `spring.docker.compose.readiness.timeout` (120 s).

### Correctif

`compose.yaml` : `mysql:latest` → `mysql:8.0`, version qui correspond exactement à celle ayant initialisé le volume. Redémarrage sans migration ni perte de données.

---

## Incident 2 — Testcontainers ne trouve pas Docker

### Symptôme

```
java.lang.IllegalStateException: Could not find a valid Docker environment.
        Please see logs and check configuration
```

Erreur levée dans `@BeforeAll`, avant le moindre test, alors que Docker Desktop tournait normalement.

### Cause

Le message est trompeur : Testcontainers **atteint** le daemon, mais celui-ci répond `HTTP 400`.

Docker Engine 29.3.1 déclare une `MinAPIVersion` de 1.40 et rejette toute requête préfixée par une version antérieure. Vérification directe sur le socket :

```
GET /v1.32/info -> HTTP 400
GET /v1.39/info -> HTTP 400
GET /v1.40/info -> HTTP 200
GET /v1.44/info -> HTTP 200
```

Le corps de la réponse 400 est identique au caractère près à celui figurant dans la trace Testcontainers, ce qui confirme le mécanisme.

### Pistes écartées

- **Problème de socket Docker** — non : `/var/run/docker.sock` existe et pointe correctement vers `~/.docker/run/docker.sock`.
- **Version de Testcontainers trop ancienne** — non : la montée en 1.21.3 produit exactement la même erreur ; 1.20.0 et 1.21.3 embarquent la même librairie `docker-java` 3.4.x.

### Correctif

Configuration `maven-surefire-plugin` dans `pom.xml` :

```xml
<systemPropertyVariables>
    <api.version>1.44</api.version>
</systemPropertyVariables>
```

1.44 est la version la plus élevée connue de `docker-java` 3.4.x, et se situe dans la plage acceptée par le daemon (1.40 à 1.54).

Ce réglage est placé dans le `pom.xml` plutôt que dans le `~/.testcontainers.properties` du poste : versionné avec le projet, il garantit un comportement identique sur toute machine.

**Réserve :** l'endroit exact d'où provient cette version d'API trop basse n'a pas pu être localisé (ni variable d'environnement, ni fichier de propriétés, ni constante littérale dans le bytecode). Le correctif est vérifié, son origine précise reste indéterminée.

---

## Incident 3 — Le container MySQL de test refuse de démarrer

Visible seulement une fois l'incident 2 corrigé.

### Symptôme

```
org.testcontainers.containers.ContainerLaunchException:
        Container startup failed for image mysql:latest
Caused by: java.lang.IllegalStateException: Wait strategy failed. Container exited with code 1
```

Durée avant échec : **366 secondes** (trois tentatives de Testcontainers).

### Cause

```
[ERROR] [MY-000067] unknown variable 'innodb_log_file_size=5M'
[ERROR] [MY-013236] The designated data directory /var/lib/mysql/ is unusable
```

`UserControllerTest` démarrait `new MySQLContainer("mysql:latest")`, soit MySQL 26.7.0. Testcontainers injecte son `my.cnf` par défaut, qui positionne `innodb_log_file_size` — variable dépréciée depuis 8.0.30 et **supprimée** depuis. `mysqld` refuse de démarrer sur une variable inconnue.

Contrôle complémentaire : lancée seule, sans la configuration Testcontainers, l'image `mysql:latest` démarre sans erreur. C'est bien la combinaison qui échoue.

### Correctif

`UserControllerTest` : `new MySQLContainer("mysql:8.0")`, aligné sur le `compose.yaml`. Le container démarre désormais en **8 secondes**.

---

## Incident 4 — Classes compilées écrasées par l'éditeur

### Symptôme

```
APPLICATION FAILED TO START

Description:
Parameter 1 of constructor in com.openclassrooms.etudiant.controller.UserController
required a bean of type 'com.openclassrooms.etudiant.mapper.UserDtoMapper'
that could not be found.
```

Touchait **à la fois** `mvn spring-boot:run` et `mvn clean test`, de façon **intermittente** (3 à 4 échecs sur 10 compilations propres).

### Observation initiale

MapStruct génère un source correct :

```java
public class UserDtoMapperImpl implements UserDtoMapper {
```

Mais la classe compilée ne déclarait aucune interface :

```
$ javap target/classes/.../UserDtoMapperImpl.class
public class com.openclassrooms.etudiant.mapper.UserDtoMapperImpl {
```

Spring instanciait donc bien un bean `UserDtoMapperImpl` — le scan le trouvait, son `@Component` était intact — mais ce bean ne correspondait à **aucun type** `UserDtoMapper`, d'où l'échec d'injection.

### Cause réelle

Le fichier `.class` **change tout seul après la fin du build**, sans qu'aucune commande ne soit lancée :

```
T0 (juste apres mvn) : md5=8074e2e2... implements=1
T1 (+3s, rien lance) : md5=e701e0db... implements=0
T2 (+6s, rien lance) : md5=e701e0db... implements=0
```

Le responsable est le **serveur de langage Java de VS Code** (`redhat.java`). Il recompile le projet en arrière-plan avec ECJ, le compilateur d'Eclipse, et **écrase `target/classes`** avec sa propre sortie. ECJ n'y résout pas l'interface `UserDtoMapper` et produit une classe « au mieux » malgré l'erreur, sans `implements`.

Deux éléments confirment l'origine :

1. La classe réécrite porte un attribut `InconsistentHierarchy` et un pool de constantes bien plus court (#25 contre #55) — signature d'un autre compilateur. Cet attribut est spécifique à ECJ, posé lorsqu'une supertype ne peut être résolue.
2. Le même projet copié dans un répertoire non surveillé par l'éditeur passe **6 tests sur 6** du premier coup.

Le caractère intermittent s'explique : tout dépend de la course entre la fin du build Maven et le déclenchement de la recompilation d'arrière-plan de l'éditeur.

### Correctif

Ajout dans `.vscode/settings.json` :

```json
"java.autobuild.enabled": false
```

L'éditeur cesse de reconstruire le projet en tâche de fond et n'écrit plus dans `target/classes`. La compilation redevient l'affaire exclusive de Maven. Le fichier étant versionné, le réglage s'applique aussi à toute personne rouvrant le projet dans VS Code.

### Fausses pistes — et pourquoi elles trompaient

Ce défaut a d'abord été attribué à tort à une interaction entre les processeurs d'annotations Lombok et MapStruct. Les mesures effectuées à ce moment-là étaient réelles mais **toutes biaisées par la même cause cachée** : le délai variable avant que l'éditeur n'écrase la classe.

| Piste testée | Résultat observé | Interprétation correcte |
|---|---|---|
| Lombok 1.18.32 → 1.18.38 | 4/10 → 3/10 échecs | Bruit statistique, aucun effet |
| Ajout de `lombok-mapstruct-binding` | Aucun effet mesurable | Sans rapport |
| `<annotationProcessors>` explicite | Aggrave nettement | Liste incomplète, omettait le second processeur de Lombok |
| `<fork>true</fork>` | 10/12 échecs | Build plus lent, donc l'éditeur gagnait plus souvent la course |
| javac en ligne de commande | 0/18 échecs | Compilait dans un répertoire non surveillé par l'éditeur |
| JDK 21.0.3 au lieu de 21.0.12.1 | 0/20 échecs | Boucle serrée : vérification effectuée avant la réécriture |

Aucune de ces modifications n'a été conservée. **Le `pom.xml` final ne contient que le correctif de l'incident 2.**

L'enseignement est méthodologique : plusieurs de ces mesures semblaient concluantes prises isolément. C'est la question « qu'est-ce qui, dans mon protocole, diffère entre le cas qui marche et celui qui échoue ? » qui a mené à la vraie cause — ici, le fait d'observer un fichier écrit par un processus tiers.

### Point d'attention sur la procédure du README

Le README propose de valider le démarrage en constatant l'existence de la table `user` en base. **Ce contrôle est insuffisant.** Hibernate crée la table pendant l'initialisation du contexte Spring, donc *avant* l'instanciation des controllers : la table peut exister alors que l'application a échoué juste après.

Un contrôle fiable consiste à vérifier la ligne `Started EtudiantBackendApplication` dans les logs, ou à interroger `http://localhost:8080/actuator/health`.

---

## Enseignement transversal : le tag `latest`

Les incidents 1 et 3 partagent la même cause : `image: 'mysql:latest'`.

`latest` n'est pas une version, c'est un **alias mobile**. Le projet fonctionnait lors de sa rédaction et a cessé de fonctionner sans qu'une ligne ne change, simplement parce que l'éditeur de l'image a repointé l'alias. C'est précisément ce que la conteneurisation est censée éliminer : même fichier, même commande, comportement différent selon la date.

Les deux occurrences — `compose.yaml` (développement) et `UserControllerTest.java` (test) — sont désormais figées sur `mysql:8.0`.

---

## Récapitulatif des fichiers modifiés

| Fichier | Modification | Justification |
|---|---|---|
| `compose.yaml` | `mysql:latest` → `mysql:8.0` | Démarrage reproductible, compatible avec le volume existant |
| `src/test/.../UserControllerTest.java` | `mysql:latest` → `mysql:8.0` | Aligne l'image de test sur celle du développement |
| `pom.xml` | Configuration `maven-surefire-plugin` (`api.version=1.44`) | Permet à Testcontainers de dialoguer avec Docker Engine 29 |
| `.vscode/settings.json` | `java.autobuild.enabled: false` | Empêche l'éditeur d'écraser les classes compilées par Maven |

Aucune dépendance n'a été ajoutée, aucune version de librairie modifiée.

---

## Vérification finale

```
$ mvn clean test
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- UserControllerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- UserServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Résultat stable sur trois exécutions consécutives.

```
$ mvn spring-boot:run
Tomcat started on port 8080 (http) with context path '/'
Started EtudiantBackendApplication in 2.686 seconds

$ curl http://localhost:8080/actuator/health
{"status":"UP"}
```
