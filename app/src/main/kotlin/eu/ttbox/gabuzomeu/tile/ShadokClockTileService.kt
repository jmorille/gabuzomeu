package eu.ttbox.gabuzomeu.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import eu.ttbox.gabuzomeu.MainActivity
import eu.ttbox.gabuzomeu.core.shadok.ShadokClock
import java.time.LocalTime

/**
 * L'heure Shadok dans le panneau des réglages rapides.
 *
 * Le pendant léger des widgets d'horloge : **aucun réveil**. `onStartListening` n'est appelé
 * que lorsque le panneau devient visible, si bien que la tuile n'a besoin ni d'alarme, ni de
 * cadence, ni de rien à annuler quand on la retire. C'est exactement ce que le widget d'origine
 * ne savait pas faire : il réarmait un réveil par minute qui survivait à sa suppression.
 */
class ShadokClockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val now = LocalTime.now()

        qsTile?.apply {
            // Les **noms** et non les glyphes : `⅃` (U+2143) n'est pas garanti dans les polices
            // système, et la tuile n'a pas de vecteur à sa disposition — elle afficherait un
            // tofu. C'est déjà l'argument documenté dans ShadokClock, dont LABELS est le défaut.
            label = ShadokClock.format(now.hour, now.minute)
            // Le décimal en sous-titre : la tuile reste lisible pour qui ne sait pas encore lire
            // le Shadok, et sert alors de table de correspondance.
            subtitle = "%02d:%02d".format(now.hour, now.minute)
            // Une horloge ne s'allume ni ne s'éteint : l'état actif ne voudrait rien dire.
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    /**
     * Ouvre la calculatrice.
     *
     * La branche de version est obligatoire, pas cosmétique : `startActivityAndCollapse(Intent)`
     * est déprécié depuis l'API 34 et y **lève** `UnsupportedOperationException` dès que
     * `targetSdk ≥ 34` — le nôtre est 36. La surcharge `PendingIntent`, elle, n'existe qu'à
     * partir de 34, alors que `minSdk` est 31.
     */
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        } else {
            openOnLegacyAndroid(intent)
        }
    }

    /**
     * Le seul chemin possible en dessous de l'API 34.
     *
     * La suppression est isolée ici, et porte les deux identifiants : celui du compilateur
     * Kotlin et celui du lint Android, qui a le sien. Elle est sans risque parce que la branche
     * de version garantit que cet appel ne s'exécute jamais là où il lèverait — et le
     * remplacement conseillé n'existe pas sur les versions qui empruntent ce chemin.
     */
    @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
    private fun openOnLegacyAndroid(intent: Intent) {
        startActivityAndCollapse(intent)
    }

    private companion object {
        /** Une seule intention, jamais distinguée d'une autre : le code n'a pas à varier. */
        const val REQUEST_CODE = 0
    }
}
