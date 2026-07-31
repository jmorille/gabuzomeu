# GaBuZoMeu — la calculatrice en base 4 des Shadoks

> Quand il n'y a pas de Shadoks, on dit **GA**
> Quand il y a un shadok de plus, on dit **BU**
> Quand il y a encore un shadok de plus, on dit **ZO**
> Et quand il y a encore un autre, on dit **MEU**.

Ne disposant que de quatre mots, les Shadoks comptent en base 4. Cette application
convertit dans les deux sens, en temps réel, entre l'écriture décimale et l'écriture
Shadok — glyphes et noms prononcés.

| Valeur | 0 | 1 | 2 | 3 |
|---|---|---|---|---|
| Nom | `Ga` | `Bu` | `Zo` | `Meu` |
| Glyphe | ◯ | _ | ⅃ | ◿ |

Ainsi 6 s'écrit `12` en base 4, donc **`BuZo`** — ou `_⅃`.

Voir [`docs/shadok-reference.md`](docs/shadok-reference.md) pour la table de référence
complète, les règles de saisie et la convention du widget.

## Prérequis

- **JDK 17**
- **Android SDK** avec la plateforme `android-37.1` (`compileSdk` 37.1)
- Rien d'autre : le wrapper Gradle est versionné.

## Compiler et tester

```bash
# Logique métier seule — Kotlin pur, aucune dépendance Android, quelques secondes
./gradlew :core:shadok:test :core:eval:test

# Build complet : compilation, lint (en warningsAsErrors), tests unitaires
./gradlew build

# Formatage
./gradlew spotlessApply     # corrige
./gradlew spotlessCheck     # vérifie

# Sur un appareil ou un émulateur (API 31 minimum)
./gradlew :app:installDebug
./gradlew :app:connectedDebugAndroidTest
```

## Architecture

```
:core:shadok   Kotlin pur — chiffres Shadok, Rational, conversion base 4, horloge
:core:eval     Kotlin pur — lexique, analyse, évaluation exacte d'expressions
:app           Compose Material 3, ViewModel, DataStore
:widget        Glance AppWidget — l'horloge Shadok
```

```
:core:shadok  ←  :core:eval  ←  :app  →  :widget  →  :core:shadok
```

Les deux modules `:core` **n'ont aucune dépendance Android**. C'est délibéré : toute la
logique de conversion et de calcul se teste sur la JVM, sans émulateur ni Robolectric.

Deux choix structurants méritent une note.

**L'arithmétique est exacte.** Le type numérique interne est une fraction (`Rational`),
pas un flottant ni un `BigDecimal`. Ce n'est pas de la coquetterie : comme 4 = 2², une
fraction irréductible `p/q` a un développement fini en base 4 **si et seulement si `q`
est une puissance de deux**. Un tiers reste donc exactement un tiers et se rend
`Ga.BuBuBu…` — son vrai développement périodique — au lieu de propager les chiffres de
queue d'un arrondi décimal. Quand un développement ne termine pas, l'affichage le dit
avec un `≈` plutôt que de présenter un arrondi comme une valeur exacte.

**Une seule source de vérité.** `ExpressionBuffer` contient l'expression ; les trois
lignes affichées en sont des projections pures. La persistance stocke la *frappe*, que
la restauration rejoue à travers les mêmes règles de saisie — un état stocké ne peut donc
pas reconstruire un tampon invalide.

## Signature de la version release

Ne jamais mettre d'identifiants dans le dépôt. En local, créer un `keystore.properties`
à la racine (il est dans `.gitignore`) :

```properties
storeFile=/chemin/vers/gabuzomeu.jks
storePassword=…
keyAlias=…
keyPassword=…
```

En CI, utiliser les variables d'environnement `GABUZOMEU_STORE_FILE`,
`GABUZOMEU_STORE_PASSWORD`, `GABUZOMEU_KEY_ALIAS`, `GABUZOMEU_KEY_PASSWORD`.

Sans l'un ni l'autre, `assembleRelease` produit un APK non signé — suffisant pour
vérifier que R8 n'a rien supprimé de nécessaire.

## Intégration et livraison continues

Deux workflows GitHub Actions, dans `.github/workflows/` :

| Workflow | Déclencheur | Rôle |
|---|---|---|
| `ci.yml` | push sur `master`, chaque pull request | Tests JVM de la logique métier, puis `spotlessCheck`, `detekt`, `build` (compilation + lint + tests) et APK release. Un troisième job lance les tests instrumentés sur un émulateur API 31. |
| `release.yml` | tag `v*.*.*`, ou manuellement | Rejoue toutes les vérifications, construit l'AAB et l'APK **signés**, et publie une release GitHub avec les artefacts. |

Publier une version :

```bash
git tag v1.0.0
git push origin v1.0.0
```

Le tag pilote le numéro de version : `v1.2.3` produit un artefact `1.2.3`. Le
`versionCode`, lui, est le nombre de commits — monotone par construction.

Secrets à définir dans les paramètres du dépôt pour que la release soit signée
(`Settings ▸ Secrets and variables ▸ Actions`) :

| Secret | Contenu |
|---|---|
| `KEYSTORE_BASE64` | le keystore encodé : `base64 -w0 release.jks` |
| `KEYSTORE_PASSWORD` | mot de passe du keystore |
| `KEY_ALIAS` | alias de la clé |
| `KEY_PASSWORD` | mot de passe de la clé |

En leur absence le workflow reste vert, mais les artefacts sortent **non signés**.

## Historique

Ce projet a été entièrement réécrit. La version d'origine (2012-2014) était un fork de
l'AOSP Calculator2 en projet Eclipse ADT construit par Maven : `minSdk 14`,
`targetSdk 16`, Java 1.6, thème Holo, aucun Gradle. Elle n'était plus compilable — dépôt
de plugins disparu, deux dépendances à installer à la main — et environ 60 % de son code
était mort ou cassé.

Cet état reste accessible sous le tag **`legacy-adt`** :

```bash
git show legacy-adt:src/eu/ttbox/gabuzomeu/service/GabuzomeuConverter.java
```

La réécriture a corrigé au passage plusieurs bugs de fond, chacun couvert par un test —
notamment la conversion des parties fractionnaires, qui n'avait jamais été implémentée
(le code produisait `Ga.BuBu` pour 0.5, et le test de l'époque en attendait `Ga.Bu`, ce
qui est faux aussi : la bonne réponse est `Ga.Zo`).

## Licence

Le code d'origine dérive de l'AOSP Calculator2, sous licence Apache 2.0.
