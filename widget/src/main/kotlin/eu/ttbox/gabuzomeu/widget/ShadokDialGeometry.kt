package eu.ttbox.gabuzomeu.widget

import eu.ttbox.gabuzomeu.core.shadok.Rational
import eu.ttbox.gabuzomeu.core.shadok.ShadokConverter
import eu.ttbox.gabuzomeu.core.shadok.ShadokDigit
import kotlin.math.cos
import kotlin.math.sin

/**
 * La géométrie du cadran, séparée de son dessin.
 *
 * Tout ce qui suit est de l'arithmétique pure : aucun `Canvas`, aucun `Bitmap`, donc tout se
 * vérifie sur la JVM. C'est délibéré — les fautes d'un cadran sont presque toujours des
 * fautes d'angle (sens de rotation, origine en haut plutôt qu'à droite, aiguille des heures
 * qui saute au lieu de dériver), et ce sont exactement les erreurs qu'un test attrape alors
 * qu'un coup d'œil sur un petit widget les laisse passer.
 *
 * Convention retenue : **0° en haut, sens horaire**, comme une horloge se lit. Ce n'est pas
 * la convention des mathématiques (0 rad vers la droite, sens antihoraire), et la conversion
 * est faite une seule fois, dans [pointAt].
 */
internal object ShadokDialGeometry {

    /** Un tour complet de cadran. */
    const val FULL_TURN: Float = 360f

    /** Le cadran est sur douze heures : 14 h et 2 h y pointent au même endroit. */
    private const val HOURS_ON_DIAL = 12

    private const val MINUTES_PER_HOUR = 60

    /** Un tour complet pour douze heures. */
    const val DEGREES_PER_HOUR: Float = FULL_TURN / HOURS_ON_DIAL

    /** Un tour complet pour soixante minutes. */
    const val DEGREES_PER_MINUTE: Float = FULL_TURN / MINUTES_PER_HOUR

    /** L'aiguille des heures avance aussi avec les minutes : 30° par heure, donc 0,5°/min. */
    const val HOUR_DRIFT_PER_MINUTE: Float = DEGREES_PER_HOUR / MINUTES_PER_HOUR

    private const val LAST_HOUR = HOURS_ON_DIAL * 2 - 1
    private const val LAST_MINUTE = MINUTES_PER_HOUR - 1

    /** Un quartier de cadran : un tour divisé par la base des Shadoks, donc 90°. */
    private const val QUARTER_TURN: Float = FULL_TURN / ShadokDigit.RADIX

    /**
     * Les quatre repères du cadran : Ga en haut, puis Bu, Zo et Meu dans le sens horaire.
     *
     * Quatre et non douze : les Shadoks comptent en base 4, un cadran en quatre quartiers
     * leur va donc naturellement — et quatre glyphes restent lisibles là où douze nombres
     * de un à trois chiffres se chevaucheraient.
     *
     * L'angle **se déduit de la valeur du chiffre** plutôt que d'être écrit à la main : Ga vaut
     * 0 et marque le haut, Meu vaut 3 et marque le trois-quarts de tour. Il devient ainsi
     * impossible de désaligner un repère de son chiffre.
     */
    val quarterMarkers: List<DialMarker> = ShadokDigit.entries.map { digit ->
        DialMarker(digits = listOf(digit), degrees = digit.value * QUARTER_TURN)
    }

    /**
     * Les douze heures, écrites en base 4 et posées à leur place sur le cadran.
     *
     * Un vrai cadran d'horloge, mais compté comme les Shadoks : 1 s'écrit `Bu`, 4 devient
     * `BuGa` (10₄) et 12 se lit `MeuGa` (30₄). Jamais plus de deux chiffres — 12 est le plus
     * grand, et 30₄ en compte deux — ce qui reste dessinable autour d'un cadran là où trois
     * chiffres se chevaucheraient.
     *
     * Midi est en haut, à 0°, comme sur n'importe quelle horloge : c'est le `% HOURS_ON_DIAL`
     * qui ramène 12 au sommet.
     */
    val hourMarkers: List<DialMarker> = (1..HOURS_ON_DIAL).map { hour ->
        DialMarker(
            digits = digitsOf(hour),
            degrees = (hour % HOURS_ON_DIAL) * DEGREES_PER_HOUR,
        )
    }

    /**
     * Les chiffres Shadok d'un entier.
     *
     * On passe par [ShadokConverter] plutôt que de refaire la division par 4 : la conversion
     * en base 4 est définie une seule fois dans le projet, et elle est largement testée.
     */
    private fun digitsOf(value: Int): List<ShadokDigit> =
        ShadokConverter.toBase4(Rational.of(value)).integerDigits

    /**
     * L'angle de l'aiguille des heures, **dérive comprise**.
     *
     * À 6 h 30 elle pointe à mi-chemin entre 6 et 7, pas sur le 6 : sans cette dérive, une
     * horloge à aiguilles ment la moitié du temps.
     */
    fun hourHandDegrees(hour: Int, minute: Int): Float {
        require(hour in 0..LAST_HOUR) { "Heure hors plage : $hour" }
        require(minute in 0..LAST_MINUTE) { "Minute hors plage : $minute" }
        // Le modulo : le cadran est sur douze heures, 14 h et 2 h s'y confondent.
        return (hour % HOURS_ON_DIAL) * DEGREES_PER_HOUR + minute * HOUR_DRIFT_PER_MINUTE
    }

    fun minuteHandDegrees(minute: Int): Float {
        require(minute in 0..LAST_MINUTE) { "Minute hors plage : $minute" }
        return minute * DEGREES_PER_MINUTE
    }

    /**
     * Le point situé à [degrees] et [radius] du centre.
     *
     * Le seul endroit qui convertit « 0° en haut, sens horaire » vers les coordonnées de
     * l'écran, où l'axe des ordonnées descend : d'où le `sin` sur x et le `-cos` sur y, et
     * non l'inverse.
     */
    fun pointAt(degrees: Float, radius: Float, centerX: Float, centerY: Float): DialPoint {
        val radians = Math.toRadians(degrees.toDouble())
        return DialPoint(
            x = centerX + radius * sin(radians).toFloat(),
            y = centerY - radius * cos(radians).toFloat(),
        )
    }
}

/**
 * Un repère du cadran : les chiffres à dessiner et leur position angulaire.
 *
 * Une **liste** de chiffres et non un seul : le cadran des quarts n'en pose qu'un par repère,
 * celui des douze heures jusqu'à deux (`MeuGa` pour midi). Le même type sert les deux, donc
 * le même code de dessin aussi.
 */
internal data class DialMarker(val digits: List<ShadokDigit>, val degrees: Float)

/** Un point du cadran, en pixels. */
internal data class DialPoint(val x: Float, val y: Float)
