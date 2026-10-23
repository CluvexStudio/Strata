package dev.cluvex.strata.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.palettes.TonalPalette
import dev.cluvex.strata.core.util.FileKind
import dev.cluvex.strata.core.util.FileVariant

data class KindColors(val container: Color, val content: Color)

object KindPalette {

    fun hue(kind: FileKind): Double? = when (kind) {
        FileKind.FOLDER, FileKind.BINARY -> null
        FileKind.IMAGE -> 340.0
        FileKind.VIDEO -> 27.0
        FileKind.AUDIO -> 60.0
        FileKind.ARCHIVE -> 85.0
        FileKind.APK -> 140.0
        FileKind.CODE -> 265.0
        FileKind.TEXT -> 200.0
        FileKind.PDF -> 12.0
        FileKind.FONT -> 300.0
        FileKind.DOCUMENT -> 240.0
        FileKind.ENCRYPTED -> 320.0
    }

    /** Hues for the text flavours, spread out so neighbouring rows never share a colour. */
    fun hue(variant: FileVariant): Double = when (variant) {
        FileVariant.MARKDOWN -> 210.0
        FileVariant.JSON -> 45.0
        FileVariant.XML -> 250.0
        FileVariant.YAML -> 172.0
        FileVariant.CSV -> 120.0
        FileVariant.LOG -> 30.0
        FileVariant.CONFIG -> 280.0
        FileVariant.DIFF -> 350.0
    }

    private val cache = HashMap<Triple<FileKind, Double, Boolean>, KindColors>()

    fun colors(kind: FileKind, colorful: Boolean, scheme: ColorScheme, dark: Boolean, hueOverride: Double? = hue(kind)): KindColors {
        if (kind == FileKind.FOLDER) return KindColors(scheme.primaryContainer, scheme.onPrimaryContainer)
        if (!colorful || hueOverride == null) return KindColors(scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
        return synchronized(cache) {
            cache.getOrPut(Triple(kind, hueOverride, dark)) {
                val palette = TonalPalette.fromHueAndChroma(hueOverride, 40.0)
                if (dark) KindColors(Color(palette.tone(30)), Color(palette.tone(90)))
                else KindColors(Color(palette.tone(90)), Color(palette.tone(30)))
            }
        }
    }
}
