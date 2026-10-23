package dev.cluvex.strata.feature.markdown

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.LazyMarkdownSuccess
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.elements.MarkdownCheckBox
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import dev.cluvex.strata.R
import dev.cluvex.strata.core.i18n.tr
import dev.cluvex.strata.core.markdown.MarkdownLink
import dev.cluvex.strata.core.markdown.MarkdownLinks
import dev.cluvex.strata.ui.isolated
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MarkdownScreen(
    path: String,
    rootEnabled: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenLocal: (String) -> Unit,
    vm: MarkdownViewModel = viewModel(key = "markdown"),
) {
    LaunchedEffect(path) { vm.load(path, rootEnabled) }
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val folder = remember(path) { File(path).parent ?: "/" }
    BackHandler(onBack = onBack)

    fun say(text: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }
    val links = remember(folder) {
        object : UriHandler {
            override fun openUri(uri: String) {
                when (val link = MarkdownLinks.resolve(folder, uri)) {
                    is MarkdownLink.Web -> runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.uri)).addCategory(Intent.CATEGORY_BROWSABLE))
                    }.onFailure { say(tr(R.string.markdown_no_app, uri)) }
                    is MarkdownLink.LocalFile -> if (File(link.path).exists()) onOpenLocal(link.path) else say(tr(R.string.markdown_not_found, link.path))
                    is MarkdownLink.Anchor -> say(tr(R.string.markdown_anchor_unsupported))
                    MarkdownLink.Unsupported -> say(tr(R.string.markdown_link_unsupported, uri))
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.testTag("markdown-screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(isolated(File(path).name), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.markdown_preview), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.common_back)) } },
                actions = { IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.common_edit)) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val document = s.document
        when {
            s.error != null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(s.error!!, color = MaterialTheme.colorScheme.error)
                Button(onClick = onEdit) { Text(stringResource(R.string.markdown_open_in_editor)) }
            }
            document == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            else -> CompositionLocalProvider(LocalUriHandler provides links) {
                val type = MaterialTheme.typography
                Markdown(
                    state = document,
                    imageTransformer = remember(folder) { LocalImages(folder) },
                    typography = markdownTypography(
                        h1 = type.headlineLarge, h2 = type.headlineMedium, h3 = type.headlineSmall,
                        h4 = type.titleLarge, h5 = type.titleMedium, h6 = type.titleSmall,
                    ),
                    components = markdownComponents(
                        codeBlock = highlightedCodeBlock,
                        codeFence = highlightedCodeFence,
                        checkbox = { MarkdownCheckBox(it.content, it.node, it.typography.text) },
                    ),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    success = { st, components, modifier ->
                        LazyMarkdownSuccess(st, components, modifier.testTag("markdown-content"), contentPadding = PaddingValues(16.dp))
                    },
                )
            }
        }
    }
}

private class LocalImages(private val folder: String) : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData {
        val path = MarkdownLinks.imagePath(folder, link)
            ?: return ImageData(rememberVectorPainter(Icons.Rounded.BrokenImage), contentDescription = stringResource(R.string.markdown_remote_image))
        val context = LocalPlatformContext.current
        // Built once per image: a fresh ImageRequest on every recomposition makes Coil restart the
        // load, which is what made images flash while the preview was being scrolled.
        val request = remember(path, context) { ImageRequest.Builder(context).data(File(path)).build() }
        val name = remember(path) { File(path).name }
        return ImageData(rememberAsyncImagePainter(request), contentDescription = name)
    }

    @Composable
    override fun intrinsicSize(painter: Painter): Size = Coil3ImageTransformerImpl.intrinsicSize(painter)
}
