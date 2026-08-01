# Référence du domaine Shadok

Document de référence extrait du code d'origine (Eclipse ADT / Maven, tag `legacy-adt`)
avant sa suppression. Il fixe le contrat que la nouvelle implémentation doit respecter.

> Quand il n'y a pas de Shadoks, on dit **GA**
> Quand il y a un shadok de plus, on dit **BU**
> Quand il y a encore un shadok de plus, on dit **ZO**
> Et quand il y a encore un autre, on dit **MEU**.

Les Shadoks ne disposant que de quatre mots, ils comptent en **base 4**.

## La leçon d'origine — les poubelles du professeur Shadoko

Source : **« Comment compter comme les Shadoks ? »**, chaîne Archive INA
(<https://www.youtube.com/watch?v=lP9PaDs2xgQ>). C'est la vidéo qui a inspiré l'application, et
c'est son vocabulaire que reprend l'écran d'aide — parce qu'il rend la **retenue** évidente là où
« ajouter un rang » n'apprend rien à qui ne sait pas déjà ce qu'est un rang :

- au-delà de MEU il n'y a plus de mot. On jette les Shadoks dans une **poubelle** et on dit « BU
  poubelle ». Pour ne pas confondre avec le BU du début, on précise qu'il n'y a pas de Shadok à
  côté de la poubelle : `BuGa` = 4 ;
- quand les poubelles deviennent trop nombreuses pour être comptées, elles vont dans une **grande
  poubelle** : `BuGaGa` = 16 ;
- puis dans une **super poubelle** : `BuGaGaGa` = 64.

Une poubelle contient donc quatre unités du rang inférieur : c'est la base 4, énoncée sans le mot.

La leçon s'arrête aux entiers. Les parties fractionnaires, l'heure des widgets et le marqueur
d'approximation sont des extensions de cette application, signalées comme telles dans l'aide.

## Les quatre chiffres

| Valeur | Nom | Glyphe | Point de code | Nom Unicode |
|---|---|---|---|---|
| 0 | `Ga` | ◯ | `U+25EF` | LARGE CIRCLE |
| 1 | `Bu` | _ | `U+005F` | LOW LINE |
| 2 | `Zo` | ⅃ | `U+2143` | REVERSED SANS-SERIF CAPITAL L |
| 3 | `Meu` | ◿ | `U+25FF` | LOWER RIGHT TRIANGLE |

Source : `res/values/strings_shadok.xml:27-35` du projet d'origine.

Les quatre points de code sont **présents dans DejaVu Serif** (vérifié sur
`assets/dejavu_serif.ttf`, table `cmap` : gid 2333, 66, 1807, 2349). Un sous-ensemble de police
limité à ces quatre caractères est donc viable ; aucun repli en dessin vectoriel n'est nécessaire.

`Ga`, `Bu`, `Zo`, `Meu` sont des noms propres issus de la série : ils **ne se traduisent pas** et
sont donc des constantes de code, non des ressources de chaînes.

## Entiers — table de référence

| Décimal | Base 4 | Noms Shadok | Glyphes |
|---|---|---|---|
| 0 | `0` | `Ga` | `◯` |
| 1 | `1` | `Bu` | `_` |
| 2 | `2` | `Zo` | `⅃` |
| 3 | `3` | `Meu` | `◿` |
| 4 | `10` | `BuGa` | `_◯` |
| 5 | `11` | `BuBu` | `__` |
| 6 | `12` | `BuZo` | `_⅃` |
| 7 | `13` | `BuMeu` | `_◿` |
| 8 | `20` | `ZoGa` | `⅃◯` |
| 15 | `33` | `MeuMeu` | `◿◿` |
| 16 | `100` | `BuGaGa` | `_◯◯` |
| 42 | `222` | `ZoZoZo` | `⅃⅃⅃` |
| 63 | `333` | `MeuMeuMeu` | `◿◿◿` |
| 64 | `1000` | `BuGaGaGa` | `_◯◯◯` |
| 255 | `3333` | `MeuMeuMeuMeu` | `◿◿◿◿` |
| 1000 | `33220` | `MeuMeuZoZoGa` | `◿◿⅃⅃◯` |
| −6 | `−12` | `−BuZo` | `−_⅃` |

Le signe négatif est porté par un champ dédié du modèle, jamais par la table de chiffres.

## Parties fractionnaires

**C'est la fonctionnalité que le code d'origine n'a jamais implémentée** (branche `isAfterDot`
morte dans `GabuzomeuConverter.java:169-176,196-205`). La partie fractionnaire y était convertie
comme un entier : `0.5` produisait `Ga.BuBu`, car 5₁₀ = 11₄.

Algorithme correct — multiplications successives par 4 :

| Décimal | Base 4 | Noms Shadok | Exact ? |
|---|---|---|---|
| 0.25 = 1/4 | `0.1` | `Ga.Bu` | oui |
| 0.5 = 2/4 | `0.2` | `Ga.Zo` | oui |
| 0.75 = 3/4 | `0.3` | `Ga.Meu` | oui |
| 0.0625 = 1/16 | `0.01` | `Ga.GaBu` | oui |
| 0.125 = 1/8 | `0.02` | `Ga.GaZo` | oui |
| 1.5 | `1.2` | `Bu.Zo` | oui |
| 2.75 | `2.3` | `Zo.Meu` | oui |
| 6.25 | `12.1` | `BuZo.Bu` | oui |
| 1/3 | `0.111…` | `Ga.BuBuBu…` | **non** (périodique) |
| 0.1 = 1/10 | `0.0121212…` | `Ga.GaBuZoBuZo…` | **non** (périodique) |
| 1/5 | `0.030303…` | `Ga.GaMeuGaMeu…` | **non** (périodique) |

### Attention : l'attendu du test d'origine était faux

`gabuzomeu-tests/.../GabuzomeuDecimal2ConverterTest.java:22` affirmait `0.5 → "Ga.Bu"`.
C'est incorrect : `Bu` = 1, donc `Ga.Bu` vaut 1×4⁻¹ = **0.25**. La bonne valeur pour 0.5 est
`Ga.Zo` (2×4⁻¹). La nouvelle table ci-dessus est la référence ; l'ancien test ne l'est pas.

### Propriété de terminaison

Comme 4 = 2², le développement en base 4 d'une fraction irréductible `p/q` **termine si et
seulement si `q` est une puissance de deux**. D'où le choix d'un type `Rational` exact comme
numérique interne : il permet de distinguer un résultat exact d'un résultat tronqué, et de
signaler ce dernier à l'utilisateur (marqueur « ≈ ») au lieu d'afficher des chiffres de queue faux.

## Règles de saisie

Reprises de `CalculatorEditable.internalReplace` (`CalculatorEditable.java:49-97`) et de
`Logic.isOperator` (`Logic.java:336-339`).

Substitutions à la saisie (`CalculatorEditable.java:23-24`) :

| Frappe | Affiché | Point de code |
|---|---|---|
| `-` | `−` | `U+2212` MINUS SIGN |
| `*` | `×` | `U+00D7` MULTIPLICATION SIGN |
| `/` | `÷` | `U+00F7` DIVISION SIGN |

Sont considérés comme opérateurs : `+ − × ÷ / *`.

Les cinq règles à préserver :

1. **Pas de second point décimal dans un même nombre** — en remontant depuis le curseur tant qu'on
   lit des chiffres, si l'on tombe sur un `.`, la saisie est rejetée.
2. **Pas deux `−` successifs** — rejeté.
3. **Opérateurs successifs écrasés** — un nouvel opérateur remplace le précédent, *sauf* qu'un `−`
   peut suivre un `+` (`5+−` est donc valide et signifie 5 + (−…)).
4. **Pas d'opérateur en tête**, sauf `−` (`−5` est valide, `×5` non).
5. **Insertion après un résultat** — saisir un opérateur prolonge le résultat ; saisir un chiffre
   repart de zéro (`Logic.acceptInsert`, `Logic.java:158-160`).

## Widget horloge

Format d'origine (`ClockWidgetProvider.updateRemoveViews`, `ClockWidgetProvider.java:78-99`) :
la chaîne `HH:MM` est encodée d'un bloc, donc **heures et minutes sont chacune converties comme un
nombre décimal**, et non chiffre à chiffre.

Exemple — 14:35 :
- `14` → 14₁₀ = `32`₄ → `MeuZo`
- `35` → 35₁₀ = `203`₄ → `ZoGaMeu`
- résultat : **`MeuZo:ZoGaMeu`**

Le widget affichait aussi : jour de la semaine en majuscules, numéro du jour, mois en majuscules,
année — tous en décimal.

**Incohérence corrigée** : les secondes étaient concaténées **en décimal**
(`ClockWidgetProvider.java:89-90`), au milieu d'un affichage Shadok. Les secondes sont supprimées —
d'autant qu'Android ne permet plus de rafraîchir un widget à la seconde (`setRepeating` à 1 s est
ramené à ≥ 60 s et rendu inexact depuis l'API 19). Résolution retenue : la minute.
