package eu.ttbox.gabuzomeu.widget

/**
 * Les façons d'écrire la date sous l'heure.
 *
 * Remplace les deux interrupteurs précédents (« afficher la date » et « date en Shadok »), qui
 * avaient un défaut : le jour de la semaine était une **ligne à part**, que « afficher la date »
 * ne gouvernait pas. Masquer la date laissait donc « VENDREDI » en place. Un format unique et
 * exhaustif rend cet état incohérent inexprimable.
 */
internal enum class ClockDateFormat {

    /** Rien sous l'heure. Le seul choix qui tienne sur un widget d'une cellule de haut. */
    NONE,

    /** `VENDREDI` */
    WEEKDAY,

    /** `31 juillet 2026` */
    LONG,

    /** `VENDREDI` puis `31 juillet 2026`, sur deux lignes. Le comportement historique. */
    WEEKDAY_AND_LONG,

    /** `31/07/2026` — compact, et lisible même étroit. */
    NUMERIC,

    /**
     * `133/13/133222` — jour, mois et année en base 4.
     *
     * Suit l'écriture choisie pour l'heure : en glyphes si l'heure est en glyphes, en noms ou en
     * chiffres bruts sinon.
     */
    SHADOK,
    ;

    /** Le nombre de lignes que ce format occupe. */
    val lineCount: Int
        get() = when (this) {
            NONE -> 0
            WEEKDAY_AND_LONG -> 2
            WEEKDAY, LONG, NUMERIC, SHADOK -> 1
        }
}
