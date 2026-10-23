package dev.cluvex.strata.feature.explorer.components

import dev.cluvex.strata.ui.isolated
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.decode.BitmapFactoryDecoder
import coil3.request.ImageRequest
import dev.cluvex.strata.R
import dev.cluvex.strata.core.media.ApkIcon
import dev.cluvex.strata.core.model.FileItem
import dev.cluvex.strata.core.model.ListPrefs
import dev.cluvex.strata.core.util.FileKind
import dev.cluvex.strata.core.util.FileKinds
import dev.cluvex.strata.core.util.Formatters
import dev.cluvex.strata.feature.explorer.dnd.DropHalo
import dev.cluvex.strata.feature.explorer.dnd.DropLook
import dev.cluvex.strata.feature.explorer.dnd.dropHaloBehind
import dev.cluvex.strata.feature.explorer.dnd.fileGestures
import dev.cluvex.strata.feature.explorer.dnd.rememberDropAnimation
import dev.cluvex.strata.ui.LocalSettings
import dev.cluvex.strata.ui.LocalUiDensity
import dev.cluvex.strata.ui.theme.KindPalette
import dev.cluvex.strata.ui.theme.toShape
import okio.Path.Companion.toPath

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FileRow(
    item: FileItem,
    selected: Boolean,
    flashKey: Int,
    showThumbnail: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    flashPulses: Int = FLASH_PULSES,
    drop: DropLook = DropLook.NONE,
    onDragStart: ((Offset, PointerId) -> Boolean)? = null,
) {
    val motion = MaterialTheme.motionScheme
    val colors = MaterialTheme.colorScheme
    val density = LocalUiDensity.current
    val settings = LocalSettings.current
    val list = settings.list
    val iconSize = density.iconDp.dp
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, motion.fastSpatialSpec(), label = "press")
    val look = rememberDropAnimation(drop)
    val background by animateColorAsState(
        when {
            drop.hovered -> colors.primaryContainer.copy(alpha = 0.6f)
            selected -> colors.secondaryContainer
            else -> Color.Transparent
        },
        motion.defaultEffectsSpec(),
        label = "rowBg",
    )
    val ghostAlpha by animateFloatAsState(if (drop.ghosted) 0.36f else 1f, motion.defaultEffectsSpec(), label = "rowGhost")
    val flash = rememberFlash(flashKey, flashPulses)
    val flashColor = colors.primary.copy(alpha = 0.32f)

    Row(
        modifier
            .then(drop.spot)
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = ghostAlpha
            }
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .drawBehind {
                val a = flash.value
                if (a > 0f) drawRect(flashColor, alpha = a)
            }
            .fileGestures(interaction, onClick, onLongClick, onDragStart)
            .padding(horizontal = 6.dp, vertical = density.rowPadDp.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DropHalo(look, drop.anchor) {
            Leading(item, selected, showThumbnail, iconSize, if (settings.behavior.iconTapSelects) onIconClick else null)
        }
        Spacer(Modifier.width(if (density.iconDp <= 30) 8.dp else 10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                isolated(Formatters.displayName(item.name, item.isDirectory, list.showExtensions)),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = density.nameSp.sp,
                maxLines = if (list.wrapNames) 3 else density.nameMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            val date = if (list.showDate) Formatters.listDate(item.lastModified, list.dateStyle, list.use24Hour) else null
            val info = trailingInfo(item, list)
            if (date != null || info != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        date.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = density.metaSp.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (info != null) {
                        Text(
                            info,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = density.metaSp.sp,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FileTile(
    item: FileItem,
    selected: Boolean,
    selecting: Boolean,
    flashKey: Int,
    showThumbnail: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    flashPulses: Int = FLASH_PULSES,
    drop: DropLook = DropLook.NONE,
    onDragStart: ((Offset, PointerId) -> Boolean)? = null,
) {
    val motion = MaterialTheme.motionScheme
    val colors = MaterialTheme.colorScheme
    val settings = LocalSettings.current
    val list = settings.list
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, motion.fastSpatialSpec(), label = "press")
    val look = rememberDropAnimation(drop)
    val background by animateColorAsState(
        when {
            drop.hovered -> colors.primaryContainer
            selected -> colors.secondaryContainer
            else -> colors.surfaceContainerLow
        },
        motion.defaultEffectsSpec(),
        label = "tileBg",
    )
    val ghostAlpha by animateFloatAsState(if (drop.ghosted) 0.36f else 1f, motion.defaultEffectsSpec(), label = "tileGhost")
    val flash = rememberFlash(flashKey, flashPulses)
    val flashColor = colors.primary.copy(alpha = 0.32f)
    val shape = settings.appearance.iconShape.toShape()
    val kind = FileKinds.of(item)
    Column(
        modifier
            .then(drop.spot)
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = ghostAlpha
            }
            .clip(MaterialTheme.shapes.large)
            .background(background)
            .drawBehind {
                val a = flash.value
                if (a > 0f) drawRect(flashColor, alpha = a)
            }
            .fileGestures(interaction, onClick, onLongClick, onDragStart)
            .padding(6.dp)
            .testTag("file-tile"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).then(drop.anchor).dropHaloBehind(look, reach = 0.72f), contentAlignment = Alignment.Center) {
            val picture = showThumbnail && list.thumbnails && (kind == FileKind.IMAGE || kind == FileKind.VIDEO)
            val apk = showThumbnail && list.apkIcons && kind == FileKind.APK
            when {
                picture -> {
                    KindIcon(item, 48.dp, shape)
                    AsyncImage(
                        model = thumbnailModel(item),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium),
                    )
                }
                apk -> ApkIconOrKind(item, 56.dp, shape)
                else -> KindIcon(item, 56.dp, shape)
            }
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClickLabel = stringResource(R.string.common_select)) { onIconClick() }
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.explorer_selected), tint = colors.onPrimary, modifier = Modifier.size(14.dp))
                    }
                } else if (selecting) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).border(2.dp, colors.outline, CircleShape).background(colors.surface.copy(alpha = 0.6f)))
                }
            }
        }
        Text(
            isolated(Formatters.displayName(item.name, item.isDirectory, list.showExtensions)),
            style = MaterialTheme.typography.labelMedium,
            maxLines = if (list.wrapNames) 3 else 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        val info = trailingInfo(item, list) ?: if (list.showDate) Formatters.listDate(item.lastModified, list.dateStyle, list.use24Hour) else null
        if (info != null) {
            Text(info, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun rememberFlash(flashKey: Int, pulses: Int): Animatable<Float, *> {
    val flash = remember { Animatable(0f) }
    LaunchedEffect(flashKey) {
        if (flashKey == 0) {
            flash.snapTo(0f)
            return@LaunchedEffect
        }
        repeat(pulses) {
            flash.animateTo(1f, tween(FLASH_IN_MS, easing = FastOutSlowInEasing))
            flash.animateTo(0.25f, tween(FLASH_OUT_MS, easing = FastOutSlowInEasing))
        }
        flash.animateTo(0f, tween(FLASH_FADE_MS))
    }
    return flash
}

const val FLASH_PULSES = 2
private const val FLASH_IN_MS = 200
private const val FLASH_OUT_MS = 280
private const val FLASH_FADE_MS = 520

fun flashDurationMs(pulses: Int): Long = (pulses * (FLASH_IN_MS + FLASH_OUT_MS) + FLASH_FADE_MS).toLong()

@Composable
private fun trailingInfo(item: FileItem, list: ListPrefs): String? = when {
    item.isSymlink -> stringResource(R.string.explorer_link)
    item.isDirectory -> null
    !list.showSize -> null
    else -> Formatters.size(item.size, list.sizeUnits)
}

@Composable
private fun thumbnailModel(item: FileItem): Any {
    val context = LocalContext.current
    return remember(item.path) {
        if (item.extension == "gif" || item.extension == "webp") {
            ImageRequest.Builder(context).data(item.path.toPath()).decoderFactory(BitmapFactoryDecoder.Factory()).build()
        } else {
            item.path.toPath()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Leading(item: FileItem, selected: Boolean, showThumbnail: Boolean, iconSize: Dp, onIconClick: (() -> Unit)?) {
    val motion = MaterialTheme.motionScheme
    val colors = MaterialTheme.colorScheme
    val settings = LocalSettings.current
    val shape = settings.appearance.iconShape.toShape()
    val kind = FileKinds.of(item)
    Box(
        Modifier
            .size(iconSize)
            .clip(shape)
            .then(if (onIconClick != null) Modifier.clickable(onClickLabel = stringResource(R.string.common_select)) { onIconClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                (scaleIn(motion.fastSpatialSpec(), initialScale = 0.6f) + fadeIn(motion.fastEffectsSpec()))
                    .togetherWith(scaleOut(motion.fastSpatialSpec(), targetScale = 0.6f) + fadeOut(motion.fastEffectsSpec()))
            },
            label = "selectCheck",
        ) { isSelected ->
            when {
                isSelected -> Box(
                    Modifier.size(iconSize).clip(shape).background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.explorer_selected), tint = colors.onPrimary)
                }
                showThumbnail && settings.list.thumbnails && (kind == FileKind.IMAGE || kind == FileKind.VIDEO) -> Box(
                    Modifier.size(iconSize),
                    contentAlignment = Alignment.Center,
                ) {
                    KindIcon(item, iconSize, shape)
                    AsyncImage(
                        model = thumbnailModel(item),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(iconSize).clip(shape),
                    )
                }
                showThumbnail && settings.list.apkIcons && kind == FileKind.APK -> ApkIconOrKind(item, iconSize, shape)
                else -> KindIcon(item, iconSize, shape)
            }
        }
    }
}

@Composable
private fun ApkIconOrKind(item: FileItem, size: Dp, shape: Shape) {
    var loaded by remember(item.path) { mutableStateOf(false) }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (!loaded) KindIcon(item, size, shape)
        AsyncImage(
            model = remember(item.path, item.lastModified) { ApkIcon(item.path, item.lastModified) },
            contentDescription = stringResource(R.string.explorer_app_icon),
            onSuccess = { loaded = true },
            modifier = Modifier.size(size).testTag("apk-icon"),
        )
    }
}

@Composable
private fun KindIcon(item: FileItem, iconSize: Dp, shape: Shape) {
    val colors = MaterialTheme.colorScheme
    val settings = LocalSettings.current
    val kind = FileKinds.of(item)
    val variant = FileKinds.variantOf(item)
    val tile = KindPalette.colors(
        kind,
        settings.appearance.colorfulIcons,
        colors,
        colors.background.luminance() < 0.5f,
        variant?.let { KindPalette.hue(it) },
    )
    Box(
        Modifier
            .size(iconSize)
            .clip(shape)
            .background(tile.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            variant?.icon ?: kind.icon,
            contentDescription = stringResource(kind.label),
            tint = tile.content,
            modifier = Modifier.padding(iconSize * 0.22f),
        )
    }
}
