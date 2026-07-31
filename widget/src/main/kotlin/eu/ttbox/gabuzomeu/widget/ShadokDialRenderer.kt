package eu.ttbox.gabuzomeu.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap

/** Les couleurs du cadran, résolues depuis le thème avant le dessin. */
internal data class DialColors(
    val face: Int,
    val ring: Int,
    val marker: Int,
    val hourHand: Int,
    val minuteHand: Int,
)

/**
 * Ce qui distingue un cadran d'un autre : ses repères, leur taille, ses couleurs.
 *
 * Deux cadrans coexistent — quatre quartiers Ga/Bu/Zo/Meu, ou les douze heures en base 4 — et
 * ils ne diffèrent que par ces trois données. Les regrouper permet à un seul renderer de
 * dessiner les deux, au lieu de dupliquer tout le tracé pour changer quatre glyphes.
 *
 * @property markerBoxRatio la taille d'un glyphe, en fraction du rayon. Plus petite sur le
 *   cadran des douze heures : douze repères, dont certains à deux chiffres, s'y bousculent.
 */
internal data class DialStyle(
    val colors: DialColors,
    val markers: List<DialMarker>,
    val markerBoxRatio: Float,
)

/**
 * Dessine le cadran dans un [Bitmap].
 *
 * Glance rend du `RemoteViews` : il n'existe pas de `Canvas` composable, et la seule façon
 * d'afficher un tracé libre est de le peindre hors composition puis de le fournir en image.
 * D'où ce rendu impératif, volontairement contenu dans un seul fichier.
 *
 * La taille est **bornée** : un widget redimensionné en grand sur un écran dense demanderait
 * sinon un bitmap de plusieurs mégaoctets, or `RemoteViews` plafonne la mémoire graphique
 * qu'une mise à jour peut transporter. Au-delà de [MAX_SIZE_PX], l'image est simplement
 * étirée par la vue — invisible à l'œil sur un cadran, et sans risque de dépassement.
 *
 * La géométrie, elle, vit dans [ShadokDialGeometry] et se teste sur la JVM : ici il ne reste
 * que des appels de peinture.
 */
internal object ShadokDialRenderer {

    /** En dessous, le cadran serait illisible ; au-dessus, la vue étire l'image. */
    private const val MIN_SIZE_PX = 96
    private const val MAX_SIZE_PX = 720

    // Toutes les dimensions sont relatives au rayon : le cadran reste juste à toute taille.
    private const val FACE_RATIO = 0.98f
    private const val RING_WIDTH_RATIO = 0.020f
    private const val MARKER_RADIUS_RATIO = 0.78f

    /** Taille d'un glyphe sur le cadran des quarts : quatre repères, donc de la place. */
    const val QUARTER_MARKER_BOX_RATIO = 0.30f

    /**
     * Sur le cadran des douze heures : douze repères, dont la moitié à deux chiffres. Il faut
     * les resserrer, sans quoi `MeuGa` et `BuBu` se toucheraient.
     */
    const val HOUR_MARKER_BOX_RATIO = 0.15f
    private const val MINUTE_HAND_RATIO = 0.70f
    private const val HOUR_HAND_RATIO = 0.46f
    private const val MINUTE_HAND_WIDTH_RATIO = 0.035f
    private const val HOUR_HAND_WIDTH_RATIO = 0.055f
    private const val PIVOT_RATIO = 0.045f

    fun render(
        context: Context,
        requestedSizePx: Int,
        hour: Int,
        minute: Int,
        style: DialStyle,
    ): Bitmap {
        val size = requestedSizePx.coerceIn(MIN_SIZE_PX, MAX_SIZE_PX)
        val bitmap = createBitmap(size, size)
        val painter = DialPainter(
            canvas = Canvas(bitmap),
            center = size / 2f,
            radius = size / 2f * FACE_RATIO,
            style = style,
        )

        painter.drawFace()
        painter.drawMarkers(context)
        painter.drawHands(hour, minute)
        return bitmap
    }

    /**
     * Le contexte de dessin, porté par un objet plutôt que repassé de méthode en méthode.
     *
     * `canvas`, `center`, `radius` et `colors` sont invariants pendant tout le rendu : les
     * trimballer en paramètres allongeait chaque signature sans rien apprendre au lecteur.
     */
    private class DialPainter(
        private val canvas: Canvas,
        private val center: Float,
        private val radius: Float,
        private val style: DialStyle,
    ) {

        private val colors = style.colors

        fun drawFace() {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.face
                style = Paint.Style.FILL
            }
            canvas.drawCircle(center, center, radius, fill)

            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.ring
                style = Paint.Style.STROKE
                strokeWidth = radius * RING_WIDTH_RATIO * 2f
            }
            // Le trait est centré sur le chemin : on rentre d'une demi-épaisseur pour qu'il ne
            // soit pas rogné par le bord du bitmap.
            canvas.drawCircle(center, center, radius - ring.strokeWidth / 2f, ring)
        }

        /**
         * Les quatre chiffres Shadok, aux quarts du cadran.
         *
         * On réutilise les mêmes ressources vectorielles que le widget numérique — un seul jeu
         * de tracés pour les deux widgets — en les teintant à la couleur du thème.
         */
        fun drawMarkers(context: Context) {
            val box = radius * style.markerBoxRatio
            style.markers.forEach { marker ->
                val at = ShadokDialGeometry.pointAt(
                    degrees = marker.degrees,
                    radius = radius * MARKER_RADIUS_RATIO,
                    centerX = center,
                    centerY = center,
                )
                drawMarkerDigits(context, marker, at, box)
            }
        }

        /**
         * Les chiffres d'un repère, alignés horizontalement et **centrés** sur sa position.
         *
         * Le centrage sur la largeur totale compte : sans lui, `MeuGa` déborderait vers la
         * droite et midi paraîtrait décalé par rapport à 3 h, qui n'a qu'un chiffre.
         */
        private fun drawMarkerDigits(
            context: Context,
            marker: DialMarker,
            at: DialPoint,
            box: Float,
        ) {
            val totalWidth = box * marker.digits.size
            var left = at.x - totalWidth / 2f
            marker.digits.forEach { digit ->
                val drawable = ContextCompat.getDrawable(
                    context,
                    ShadokTimeGlyphs.drawableOf(digit),
                )
                if (drawable != null) {
                    drawable.setTint(colors.marker)
                    drawable.setBounds(
                        left.toInt(),
                        (at.y - box / 2f).toInt(),
                        (left + box).toInt(),
                        (at.y + box / 2f).toInt(),
                    )
                    drawable.draw(canvas)
                }
                left += box
            }
        }

        fun drawHands(hour: Int, minute: Int) {
            // L'aiguille des minutes est la plus longue et la plus fine, celle des heures la
            // plus courte et la plus épaisse : c'est ce qui les distingue d'un coup d'œil.
            drawHand(
                degrees = ShadokDialGeometry.minuteHandDegrees(minute),
                length = radius * MINUTE_HAND_RATIO,
                width = radius * MINUTE_HAND_WIDTH_RATIO * 2f,
                color = colors.minuteHand,
            )
            drawHand(
                degrees = ShadokDialGeometry.hourHandDegrees(hour, minute),
                length = radius * HOUR_HAND_RATIO,
                width = radius * HOUR_HAND_WIDTH_RATIO * 2f,
                color = colors.hourHand,
            )

            val pivot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.hourHand
                style = Paint.Style.FILL
            }
            canvas.drawCircle(center, center, radius * PIVOT_RATIO, pivot)
        }

        private fun drawHand(degrees: Float, length: Float, width: Float, color: Int) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = width
                strokeCap = Paint.Cap.ROUND
            }
            val tip = ShadokDialGeometry.pointAt(degrees, length, center, center)
            canvas.drawLine(center, center, tip.x, tip.y, paint)
        }
    }
}
