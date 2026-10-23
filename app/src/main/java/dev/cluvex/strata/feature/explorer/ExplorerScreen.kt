package dev.cluvex.strata.feature.explorer

import dev.cluvex.strata.core.i18n.separator
import dev.cluvex.strata.feature.vault.VaultSetupDialog
import dev.cluvex.strata.feature.vault.VaultUnlockDialog
import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.cluvex.strata.R
import dev.cluvex.strata.core.i18n.tr
import dev.cluvex.strata.core.fs.Mounts
import dev.cluvex.strata.core.data.sortFor
import dev.cluvex.strata.core.model.FolderSorts
import dev.cluvex.strata.core.archive.ArchiveFormat
import dev.cluvex.strata.core.fs.Conflict
import dev.cluvex.strata.core.model.FileItem
import dev.cluvex.strata.core.open.Opener
import dev.cluvex.strata.core.remote.RemoteProtocol
import dev.cluvex.strata.core.remote.RemoteUri
import dev.cluvex.strata.core.termux.Project
import dev.cluvex.strata.core.termux.TermuxProjects
import dev.cluvex.strata.core.util.FileKind
import dev.cluvex.strata.core.util.FileKinds
import dev.cluvex.strata.core.util.IntentUtils
import dev.cluvex.strata.feature.explorer.dnd.DragLayer
import dev.cluvex.strata.feature.explorer.dnd.FileDragState
import dev.cluvex.strata.feature.explorer.dnd.LocalFileDrag
import dev.cluvex.strata.feature.explorer.dnd.dragHost
import dev.cluvex.strata.feature.apk.SignApkDialog
import dev.cluvex.strata.feature.explorer.components.BatchRenameDialog
import dev.cluvex.strata.feature.explorer.components.BookmarkActions
import dev.cluvex.strata.feature.explorer.components.BookmarksSheet
import dev.cluvex.strata.feature.explorer.components.BottomBarActions
import dev.cluvex.strata.feature.explorer.components.ChecksumDialog
import dev.cluvex.strata.feature.explorer.components.CreateArchiveDialog
import dev.cluvex.strata.feature.explorer.components.CreateLinkDialog
import dev.cluvex.strata.feature.explorer.components.DeleteDialog
import dev.cluvex.strata.feature.explorer.components.EncryptDialog
import dev.cluvex.strata.feature.explorer.components.ExplorerBottomBar
import dev.cluvex.strata.feature.explorer.components.InputDialog
import dev.cluvex.strata.feature.explorer.components.InputSelection
import dev.cluvex.strata.feature.explorer.components.ItemActionSheet
import dev.cluvex.strata.feature.explorer.components.ItemActions
import dev.cluvex.strata.feature.explorer.components.MergeDialog
import dev.cluvex.strata.feature.explorer.components.NewFileSuffixes
import dev.cluvex.strata.feature.explorer.components.PaneColumn
import dev.cluvex.strata.feature.explorer.components.PasswordDialog
import dev.cluvex.strata.feature.explorer.components.SortSheet
import dev.cluvex.strata.feature.explorer.components.SplitDialog
import dev.cluvex.strata.feature.explorer.components.TaskOverlay
import dev.cluvex.strata.feature.explorer.components.TransferDialog
import dev.cluvex.strata.feature.props.PropertiesSheet
import dev.cluvex.strata.feature.remote.HostKeyChangedDialog
import dev.cluvex.strata.feature.remote.StoragePasswordDialog
import dev.cluvex.strata.feature.settings.ToolbarTab
import dev.cluvex.strata.feature.termux.NewProjectSheet
import dev.cluvex.strata.feature.termux.ProjectRunSheet
import dev.cluvex.strata.feature.termux.TerminalGate
import dev.cluvex.strata.feature.termux.TermuxViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface Dialog {
    data object None : Dialog
    data class NewFolder(val pane: Pane) : Dialog
    data class NewFile(val pane: Pane) : Dialog
    data object Delete : Dialog
    data object Sort : Dialog
    data object Bookmarks : Dialog
    data class CreateArchive(val pane: Pane, val items: List<FileItem>) : Dialog
    data class BatchRename(val pane: Pane, val items: List<FileItem>) : Dialog
    data class CreateLink(val pane: Pane, val items: List<FileItem>) : Dialog
    data class Split(val pane: Pane, val item: FileItem) : Dialog
    data class Merge(val pane: Pane, val items: List<FileItem>) : Dialog
    data class Encrypt(val pane: Pane, val item: FileItem) : Dialog
    data class Decrypt(val pane: Pane, val item: FileItem) : Dialog
    data class SignApk(val item: FileItem) : Dialog
    data class Transfer(val move: Boolean) : Dialog
    data class JumpTo(val pane: Pane) : Dialog
    data class Rename(val pane: Pane, val item: FileItem) : Dialog
    data class ItemSheet(val pane: Pane, val item: FileItem) : Dialog
    data class Checksum(val item: FileItem) : Dialog
    data class Properties(val items: List<FileItem>) : Dialog
    data class NewProject(val dir: String) : Dialog
    data class RunProject(val project: Project) : Dialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    vm: ExplorerViewModel,
    onOpenFile: (FileItem) -> Unit,
    onOpenWithOpener: (FileItem, Opener) -> Unit,
    onOpenWith: (FileItem) -> Unit,
    onCompare: (FileItem, FileItem) -> Unit,
    onOpenSearch: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onCustomize: (ToolbarTab) -> Unit = {},
    drawerOpen: Boolean = false,
    appSnackbar: SnackbarHostState? = null,
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<Dialog>(Dialog.None) }
    var overflow by remember { mutableStateOf(false) }
    var lastBackPress by remember { mutableLongStateOf(0L) }
    val rootPath = vm.rootPath
    val dual = ui.settings.dualPanePreferred
    val termux = viewModel<TermuxViewModel>().session
    val sshHere = RemoteUri.parse(ui.activePane.path)?.let { (id, dir) ->
        ui.storages.firstOrNull { it.id == id && it.protocol == RemoteProtocol.SFTP }?.let { it to dir }
    }
    val activePath = ui.activePane.path
    val project by produceState<Project?>(null, activePath, ui.activePane.items) {
        value = if (RemoteUri.isRemote(activePath)) null else withContext(Dispatchers.IO) { TermuxProjects.detect(java.io.File(activePath)) }
    }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            vm.consumeMessage()
            (appSnackbar ?: snackbar).showSnackbar(it)
        }
    }

    BackHandler(enabled = !drawerOpen) {
        val pane = ui.activePane
        when {
            pane.hasSelection -> vm.clearSelection(ui.active)
            ui.searchActive -> vm.setSearchActive(false)
            pane.path != rootPath && pane.path != "/" -> vm.navigateUp(ui.active)
            !ui.settings.behavior.doubleBackToExit -> (context as? Activity)?.finish()
            else -> {
                val now = SystemClock.uptimeMillis()
                if (now - lastBackPress < 2000) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPress = now
                    scope.launch { snackbar.showSnackbar(tr(R.string.explorer_press_back_again)) }
                }
            }
        }
    }

    fun withLocal(item: FileItem, use: (FileItem) -> Unit) {
        if (RemoteUri.isRemote(item.path)) vm.openRemote(item, use) else use(item)
    }

    fun localOnly(item: FileItem, block: () -> Unit) {
        if (RemoteUri.isRemote(item.path)) {
            dialog = Dialog.None
            scope.launch { (appSnackbar ?: snackbar).showSnackbar(tr(R.string.explorer_not_on_storage)) }
        } else {
            block()
        }
    }

    fun handleOpen(pane: Pane, item: FileItem) {
        when {
            item.isDirectory -> vm.open(pane, item)
            FileKinds.of(item) == FileKind.ENCRYPTED -> localOnly(item) { dialog = Dialog.Decrypt(pane, item) }
            else -> withLocal(item) { onOpenFile(it) }
        }
    }

    fun visibleOfActive() = visibleItems(ui.activePane, ui.settings, ui.query).map { it.path }

    val selection = ui.activePane.items.filter { it.path in ui.activePane.selected }
    val single = selection.singleOrNull()
    val smaliFolder by produceState(false, single?.path, ui.active) {
        value = single != null && single.isDirectory && vm.hasSmali(ui.active, single)
    }
    val jarFile = single != null && !single.isDirectory && single.extension.equals("jar", ignoreCase = true)

    fun selectionInOrder() = visibleItems(ui.activePane, ui.settings, ui.query).filter { it.path in ui.activePane.selected }

    fun archiveName(items: List<FileItem>, folder: String): String =
        items.singleOrNull()?.let { ArchiveFormat.ZIP.defaultName(it).removeSuffix(ArchiveFormat.ZIP.extension) }
            ?: java.io.File(folder).name.ifEmpty { "archive" }

    fun linkParent(pane: Pane): String =
        if (dual && ui.pane(pane.other).path != ui.pane(pane).path) ui.pane(pane.other).path else ui.pane(pane).path

    val drag = remember { FileDragState() }
    drag.haptics = LocalHapticFeedback.current
    drag.onDrop = { session, spot -> vm.dropInto(session.pane, session.items, spot.dir) }
    drag.onSpring = { spot -> Pane.entries.getOrNull(spot.pane)?.let { p -> vm.setActive(p); vm.openPath(p, spot.dir) } }
    val host = remember { arrayOfNulls<LayoutCoordinates>(1) }

    LaunchedEffect(ui.undo?.seq) {
        val u = ui.undo ?: return@LaunchedEffect
        val result = (appSnackbar ?: snackbar).showSnackbar(u.message, actionLabel = tr(R.string.explorer_undo), duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) vm.undoMove(u) else vm.consumeUndo(u.seq)
    }

    LaunchedEffect(ui.incoming?.seq) {
        val c = ui.incoming ?: return@LaunchedEffect
        val result = (appSnackbar ?: snackbar).showSnackbar(
            tr(R.string.explorer_incoming_hidden, c.name),
            actionLabel = tr(R.string.explorer_incoming_save),
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) vm.saveIncomingCopy(c) else vm.consumeIncoming(c.seq)
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { host[0] = it }
            .dragHost(drag) { host[0] },
    ) {
    CompositionLocalProvider(LocalFileDrag provides drag) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (ui.searchActive) {
                FilterBar(query = ui.query, onQuery = vm::setQuery, onClose = { vm.setSearchActive(false) })
            } else {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.explorer_menu)) }
                    },
                    title = { ExplorerTitle(ui.activePane, visibleItems(ui.activePane, ui.settings, ui.query)) },
                    actions = {
                        IconButton(onClick = { vm.setSearchActive(true) }) { Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.explorer_filter)) }
                        IconButton(onClick = {
                            if (RemoteUri.isRemote(ui.activePane.path)) {
                                scope.launch { snackbar.showSnackbar(tr(R.string.explorer_search_local_only)) }
                            } else {
                                onOpenSearch(ui.activePane.path)
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ManageSearch, contentDescription = stringResource(R.string.explorer_advanced_search))
                        }
                        IconButton(onClick = { dialog = Dialog.Sort }) { Icon(Icons.Rounded.SortByAlpha, contentDescription = stringResource(R.string.common_sort)) }
                        Box {
                            IconButton(onClick = { overflow = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.explorer_options)) }
                            DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.explorer_refresh_both)) }, onClick = { overflow = false; vm.refreshAll() })
                                DropdownMenuItem(text = { Text(stringResource(R.string.explorer_jump_to_path_more)) }, onClick = { overflow = false; dialog = Dialog.JumpTo(ui.active) })
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (ui.settings.showHidden) R.string.common_hide_hidden else R.string.common_show_hidden)) },
                                    onClick = { overflow = false; vm.setShowHidden(!ui.settings.showHidden) },
                                )
                                val mount = ui.activePane.mount
                                if (ui.settings.rootAccess && mount != null && mount.point in Mounts.SYSTEM_POINTS) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(if (mount.readOnly) R.string.explorer_mount_rw else R.string.explorer_mount_ro, mount.point)) },
                                        onClick = { overflow = false; vm.remount(ui.active, writable = mount.readOnly) },
                                    )
                                }
                                sshHere?.let { (c, dir) ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.explorer_ssh_here)) },
                                        onClick = { overflow = false; termux.openSsh(c.host, c.port, c.username, dir) },
                                    )
                                }
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            ExplorerBottomBar(
                selectionCount = ui.activePane.selected.size,
                canBack = ui.activePane.history.canBack,
                canForward = ui.activePane.history.canForward,
                activeIsLeft = ui.active == Pane.LEFT,
                dual = dual,
                clipboardCount = ui.clipboard?.items?.size ?: 0,
                clipboardIsMove = ui.clipboard?.move == true,
                selectedFiles = selection.count { !it.isDirectory },
                remote = ui.isRemote(ui.active),
                projectLabel = project?.kind?.label,
                smaliFolder = smaliFolder,
                jarFile = jarFile,
                vault = ui.isVault(ui.active),
                browse = ui.settings.actions.browse,
                selection = ui.settings.actions.selection,
                showHidden = ui.settings.showHidden,
                actions = BottomBarActions(
                    onBack = { vm.goBack(ui.active) },
                    onForward = { vm.goForward(ui.active) },
                    onUp = { vm.navigateUp(ui.active) },
                    onHome = { vm.goHome(ui.active) },
                    onSwap = vm::swapPanes,
                    onNewFolder = { dialog = Dialog.NewFolder(ui.active) },
                    onNewFile = { dialog = Dialog.NewFile(ui.active) },
                    onRefresh = { vm.refresh(ui.active) },
                    onPaste = { vm.paste(ui.active) },
                    onCancelClipboard = vm::clearClipboard,
                    onSelectAll = { vm.selectAll(ui.active, visibleOfActive()) },
                    onClearSelection = { vm.clearSelection(ui.active) },
                    onCopyToOther = { vm.beginTransfer(move = false) },
                    onMoveToOther = { vm.beginTransfer(move = true) },
                    onCopyToClipboard = { vm.copySelectionToClipboard(ui.active, move = false) },
                    onCutToClipboard = { vm.copySelectionToClipboard(ui.active, move = true) },
                    onDelete = { dialog = Dialog.Delete },
                    onRename = { selection.firstOrNull()?.let { dialog = Dialog.Rename(ui.active, it) } },
                    onCompress = { dialog = Dialog.CreateArchive(ui.active, selectionInOrder()) },
                    onShare = { IntentUtils.share(context, selection) },
                    onProperties = { dialog = Dialog.Properties(selection) },
                    onInvert = { vm.invertSelection(ui.active, visibleOfActive()) },
                    onBookmarks = { dialog = Dialog.Bookmarks },
                    onBookmarkFolder = { vm.addBookmarks(listOf(ui.activePane.path)) },
                    onBookmarkSelection = { vm.addSelectionToBookmarks(ui.active) },
                    onBatchRename = { dialog = Dialog.BatchRename(ui.active, selectionInOrder()) },
                    onCreateLinks = { dialog = Dialog.CreateLink(ui.active, selectionInOrder()) },
                    onMerge = { dialog = Dialog.Merge(ui.active, selectionInOrder().filterNot { it.isDirectory }) },
                    onCompare = {
                        val two = selectionInOrder().filterNot { it.isDirectory }
                        if (two.size == 2) onCompare(two[0], two[1])
                    },
                    onNewProject = { dialog = Dialog.NewProject(ui.activePane.path) },
                    onOpenTerminal = { termux.openTerminal(ui.activePane.path) },
                    onRunProject = { project?.let { dialog = Dialog.RunProject(it) } },
                    onSmaliToDex = { single?.let { vm.smaliToDex(ui.active, it) } },
                    onJarToDex = { single?.let { vm.jarToDex(ui.active, it) } },
                    onFilter = { vm.setSearchActive(true) },
                    onSearch = {
                        if (RemoteUri.isRemote(ui.activePane.path)) {
                            scope.launch { snackbar.showSnackbar(tr(R.string.explorer_search_local_only)) }
                        } else {
                            onOpenSearch(ui.activePane.path)
                        }
                    },
                    onSort = { dialog = Dialog.Sort },
                    onJump = { dialog = Dialog.JumpTo(ui.active) },
                    onToggleHidden = { vm.setShowHidden(!ui.settings.showHidden) },
                    onCustomize = onCustomize,
                    onMoveToVault = { vm.moveSelectionToVault(ui.active) },
                    onRestore = { vm.restoreFromVault(ui.active) },
                ),
            )
        },
    ) { padding ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val panes = if (dual) listOf(Pane.LEFT, Pane.RIGHT) else listOf(ui.active)
            for (pane in panes) {
                val state = ui.pane(pane)
                val visible = remember(state.items, ui.settings, ui.query) { visibleItems(state, ui.settings, ui.query) }
                PaneColumn(
                    state = state,
                    visible = visible,
                    isActive = ui.active == pane,
                    showActive = dual,
                    rootPath = rootPath,
                    onActivate = { vm.setActive(pane) },
                    onUp = { vm.setActive(pane); vm.navigateUp(pane) },
                    onOpen = { handleOpen(pane, it) },
                    onToggleSelect = { vm.toggleSelect(pane, it) },
                    onItemMenu = { dialog = Dialog.ItemSheet(pane, it) },
                    onJumpTo = { dialog = Dialog.JumpTo(pane) },
                    onRefresh = { vm.setActive(pane); vm.refresh(pane) },
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    displayPath = if (RemoteUri.isRemote(state.path)) ui.displayPath(state.path) else null,
                    pane = pane,
                )
            }
        }
    }
    }
    DragLayer(drag) { host[0]?.positionInRoot() ?: Offset.Zero }
    }

    when (val d = dialog) {
        Dialog.None -> Unit
        is Dialog.NewFolder -> InputDialog(
            title = stringResource(R.string.common_new_folder), label = stringResource(R.string.explorer_folder_name),
            onConfirm = { vm.newFolder(d.pane, it); dialog = Dialog.None },
            onDismiss = { dialog = Dialog.None },
        )
        is Dialog.NewFile -> InputDialog(
            title = stringResource(R.string.common_new_file), label = stringResource(R.string.explorer_file_name),
            suffixes = NewFileSuffixes,
            onConfirm = { vm.newFile(d.pane, it); dialog = Dialog.None },
            onDismiss = { dialog = Dialog.None },
        )
        Dialog.Bookmarks -> BookmarksSheet(
            bookmarks = ui.bookmarks,
            actions = BookmarkActions(
                onOpen = { vm.openBookmark(ui.active, it); dialog = Dialog.None },
                onAddCurrent = { vm.addBookmarks(listOf(ui.activePane.path)) },
                onUpdate = { path, edited -> vm.updateBookmark(path, edited) },
                onMove = { path, offset -> vm.moveBookmark(path, offset) },
                onDelete = { vm.removeBookmark(it) },
                onRenameGroup = { from, to -> vm.renameBookmarkGroup(from, to) },
                onDeleteGroup = { vm.deleteBookmarkGroup(it) },
            ),
            onDismiss = { dialog = Dialog.None },
        )
        is Dialog.CreateArchive -> CreateArchiveDialog(
            items = d.items,
            defaultName = archiveName(d.items, ui.pane(d.pane).path),
            onConfirm = { name, options, independent ->
                vm.createArchive(d.pane, d.items, name, options, independent)
                dialog = Dialog.None
            },
            onDismiss = { dialog = Dialog.None },
        )
        is Dialog.BatchRename -> BatchRenameDialog(
            items = d.items,
            history = ui.renameHistory,
            preview = { rule, order, descending -> vm.previewRename(d.pane, d.items, rule, order, descending) },
            onApply = { plan, rule -> vm.applyRename(d.pane, plan, rule); dialog = Dialog.None },
            onDismiss = { dialog = Dialog.None },
        )
        is Dialog.CreateLink -> CreateLinkDialog(
            items = d.items,
            defaultParent = linkParent(d.pane),
            onConfirm = { parent, requests -> vm.createLinks(d.pane, parent, requests); dialog = Dialog.None },
            onDismiss = { dialog = Dialog.None },
        )
        is Dialog.Split -> SplitDialog(
            item = d.item,
            onConfirm = { method, pattern -> vm.splitFile(d.pane, d.item, method, pattern); dialog = Dialog.None },
            onDismiss = { dialog = Dialog.None },
        )
        is Dialog.Merge -> MergeDialog(
            parts = d.items,
            existingNames = ui.pane(d.pane).items.mapTo(HashSet()) { it.name },
            onConfirm = { ordered, output, pdf -> vm.mergeFiles(d.pane, ordered, output, pdf); dialog = Dialog.None },
            onDismiss = { dialog = Dialog.None },
        )
        is Dialog.Encrypt -> EncryptDialog(
            item = d.item,
            onConfirm = { pw -> vm.encryptFile(d.pane, d.item, pw); dialog = Dialog.None },
            onDismiss = { dialog = Dialog.None },
        )
        is Dialog.Decrypt -> PasswordDialog(
            title = stringResource(R.string.explorer_decrypt_file),
            fileName = d.item.name,
            wrong = false,
            onConfirm = { pw -> vm.decryptFile(d.pane, d.item, pw); dialog = Dialog.None },
            onDismiss = { dialog = Dialog.None },
        )
        is Dialog.SignApk -> SignApkDialog(
            item = d.item,
            onConfirm = { request -> vm.signApk(d.item, request); dialog = Dialog.None },
            onDismiss = { dialog = Dialog.None },
        )
        is Dialog.JumpTo -> InputDialog(
            title = stringResource(R.string.explorer_jump_to_path), label = stringResource(R.string.common_path), initial = ui.pane(d.pane).path,
            confirmText = stringResource(R.string.explorer_go), select = InputSelection.ALL, ltr = true,
            onConfirm = { vm.openPath(d.pane, it.trim()); dialog = Dialog.None },
            onDismiss = { dialog = Dialog.None },
        )
        is Dialog.Rename -> InputDialog(
            title = stringResource(R.string.common_rename), label = stringResource(R.string.explorer_new_name), initial = d.item.name,
            select = if (d.item.isDirectory) InputSelection.ALL else InputSelection.BASE_NAME,
            onConfirm = { vm.rename(d.pane, d.item, it); vm.clearSelection(d.pane); dialog = Dialog.None },
            onDismiss = { dialog = Dialog.None },
        )
        is Dialog.Transfer -> Unit
        Dialog.Delete -> DeleteDialog(
            count = ui.activePane.selected.size,
            recycleByDefault = ui.settings.trashByDefault,
            allowRecycle = remember(ui.active, ui.activePane.path, ui.activePane.viaRoot) { vm.canRecycle(ui.active) },
            onConfirm = { toBin -> vm.deleteSelection(ui.active, toBin); dialog = Dialog.None },
            onDismiss = { dialog = Dialog.None },
        )
        Dialog.Sort -> {
            val folder = ui.activePane.path
            var own by remember(folder) { mutableStateOf(ui.settings.folderSorts.containsKey(FolderSorts.key(folder))) }
            var order by remember(folder) { mutableStateOf(ui.settings.sortFor(folder)) }
            SortSheet(
                current = order,
                showHidden = ui.settings.showHidden,
                onChange = { o ->
                    order = o
                    if (own) vm.setFolderSort(folder, o) else vm.setSort(o)
                },
                onToggleHidden = vm::setShowHidden,
                onDismiss = { dialog = Dialog.None },
                thisFolderOnly = own,
                folderName = ui.displayPath(folder).substringAfterLast('/').ifBlank { ui.displayPath(folder) },
                onThisFolderOnly = { on ->
                    own = on
                    vm.setFolderSort(folder, if (on) order else null)
                    if (!on) order = ui.settings.sortOrder
                },
            )
        }
        is Dialog.ItemSheet -> ItemActionSheet(
            item = d.item,
            smaliFolder = produceState(false, d.item.path) { value = d.item.isDirectory && vm.hasSmali(d.pane, d.item) }.value,
            actions = ItemActions(
                onSelect = { vm.setActive(d.pane); vm.toggleSelect(d.pane, d.item); dialog = Dialog.None },
                onOpen = {
                    dialog = Dialog.None
                    handleOpen(d.pane, d.item)
                },
                onOpenWith = { dialog = Dialog.None; withLocal(d.item) { onOpenWith(it) } },
                onOpenAsText = { dialog = Dialog.None; withLocal(d.item) { onOpenWithOpener(it, Opener.TEXT_EDITOR) } },
                onHex = { dialog = Dialog.None; withLocal(d.item) { onOpenWithOpener(it, Opener.HEX_EDITOR) } },
                onRename = { dialog = Dialog.Rename(d.pane, d.item) },
                onShare = { dialog = Dialog.None; withLocal(d.item) { IntentUtils.share(context, listOf(it)) } },
                onChecksum = { dialog = Dialog.None; withLocal(d.item) { dialog = Dialog.Checksum(it) } },
                onExtract = { localOnly(d.item) { vm.extractArchive(d.pane, d.item); dialog = Dialog.None } },
                onApkInfo = { dialog = Dialog.None; withLocal(d.item) { onOpenWithOpener(it, Opener.APK_VIEWER) } },
                onView = { dialog = Dialog.None; withLocal(d.item) { onOpenWithOpener(it, Opener.ARCHIVE_VIEWER) } },
                onSignApk = { localOnly(d.item) { dialog = Dialog.SignApk(d.item) } },
                onCompress = { localOnly(d.item) { dialog = Dialog.CreateArchive(d.pane, listOf(d.item)) } },
                onAddBookmark = { vm.addBookmarks(listOf(d.item.path)); dialog = Dialog.None },
                onCreateLink = { localOnly(d.item) { dialog = Dialog.CreateLink(d.pane, listOf(d.item)) } },
                onSplit = { localOnly(d.item) { dialog = Dialog.Split(d.pane, d.item) } },
                onEncrypt = { localOnly(d.item) { dialog = Dialog.Encrypt(d.pane, d.item) } },
                onDecrypt = { localOnly(d.item) { dialog = Dialog.Decrypt(d.pane, d.item) } },
                onProperties = { localOnly(d.item) { dialog = Dialog.Properties(listOf(d.item)) } },
                onDelete = {
                    vm.setActive(d.pane)
                    vm.clearSelection(d.pane)
                    vm.toggleSelect(d.pane, d.item, sound = false)
                    dialog = Dialog.Delete
                },
                onSmaliToDex = { localOnly(d.item) { dialog = Dialog.None; vm.smaliToDex(d.pane, d.item) } },
                onJarToDex = { localOnly(d.item) { dialog = Dialog.None; vm.jarToDex(d.pane, d.item) } },
                onMoveToVault = { localOnly(d.item) { dialog = Dialog.None; vm.moveToVault(d.pane, listOf(d.item)) } },
                onRestore = { dialog = Dialog.None; vm.restoreFromVault(d.pane, listOf(d.item)) },
            ),
            onDismiss = { dialog = Dialog.None },
            layout = ui.settings.actions.sheet,
            onCustomize = { dialog = Dialog.None; onCustomize(ToolbarTab.SHEET) },
        )
        is Dialog.Checksum -> ChecksumDialog(item = d.item, onDismiss = { dialog = Dialog.None })
        is Dialog.Properties -> PropertiesSheet(
            items = d.items,
            rootEnabled = ui.settings.rootAccess,
            onDismiss = { dialog = Dialog.None },
            onChanged = { vm.refreshAll() },
        )
        is Dialog.NewProject -> NewProjectSheet(
            parentDir = d.dir,
            session = termux,
            onCreated = { path ->
                dialog = Dialog.None
                vm.revealCreated(d.dir, path)
            },
            onDismiss = { dialog = Dialog.None },
        )
        is Dialog.RunProject -> ProjectRunSheet(d.project, onDismiss = { dialog = Dialog.None }, session = termux)
    }

    if (ui.remoteEdits.isNotEmpty() && dialog == Dialog.None) {
        val edits = ui.remoteEdits
        AlertDialog(
            onDismissRequest = vm::keepRemoteEdits,
            title = { Text(stringResource(R.string.explorer_upload_title)) },
            text = {
                Text(
                    if (edits.size == 1) {
                        stringResource(
                            R.string.explorer_upload_one,
                            RemoteUri.name(edits[0].uri),
                            ui.displayPath(RemoteUri.parent(edits[0].uri) ?: edits[0].uri),
                        )
                    } else {
                        pluralStringResource(R.plurals.explorer_upload_many, edits.size, edits.size)
                    },
                )
            },
            confirmButton = { TextButton(onClick = vm::uploadRemoteEdits) { Text(stringResource(R.string.explorer_upload)) } },
            dismissButton = { TextButton(onClick = vm::keepRemoteEdits) { Text(stringResource(R.string.explorer_not_now)) } },
        )
    }
    TerminalGate(termux, appSnackbar ?: snackbar)
    when (val prompt = ui.storagePrompt) {
        null -> Unit
        is StoragePrompt.Password -> StoragePasswordDialog(prompt.connection, onConfirm = vm::unlockStorage, onDismiss = vm::dismissStoragePrompt)
        is StoragePrompt.HostKey -> HostKeyChangedDialog(
            prompt.connection, old = prompt.connection.hostKey, presented = prompt.presented,
            onTrust = vm::trustStorageKey, onDismiss = vm::dismissStoragePrompt,
        )
        is StoragePrompt.Vault -> if (prompt.setup) {
            VaultSetupDialog(onDismiss = vm::dismissStoragePrompt, onReady = vm::vaultReady)
        } else {
            VaultUnlockDialog(onDismiss = vm::dismissStoragePrompt, onUnlocked = vm::vaultReady)
        }
    }

    ui.transferPrompt?.let { prompt ->
        TransferDialog(
            move = prompt.move,
            count = prompt.count,
            destination = prompt.destination,
            conflicts = prompt.conflicts,
            onConfirm = { conflict -> vm.confirmTransfer(conflict) },
            onDismiss = { vm.cancelTransfer() },
        )
    }

    ui.passwordPrompt?.let { p ->
        PasswordDialog(
            title = stringResource(if (p.purpose == PasswordPurpose.EXTRACT) R.string.explorer_encrypted_archive else R.string.explorer_decrypt_file),
            fileName = p.item.name,
            wrong = p.wrong,
            onConfirm = vm::submitPassword,
            onDismiss = vm::dismissPasswordPrompt,
        )
    }

    ui.task?.let { TaskOverlay(it) }
    ui.build?.let { dev.cluvex.strata.feature.build.BuildDialog(it, onClose = vm::closeBuild) }
    if ((ui.task != null || ui.build?.finished == false) && ui.settings.behavior.keepScreenOnTasks) {
        val view = LocalView.current
        DisposableEffect(view) {
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
        }
    }
}

@Composable
private fun ExplorerTitle(pane: PaneState, visible: List<FileItem>) {
    Column {
        Text("Strata", maxLines = 1)
        Text(
            countsLine(visible),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun countsLine(items: List<FileItem>): String {
    val folders = items.count { it.isDirectory }
    val files = items.size - folders
    return pluralStringResource(R.plurals.common_folders, folders, folders) + separator() + pluralStringResource(R.plurals.common_files, files, files)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(query: String, onQuery: (String) -> Unit, onClose: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.common_close)) }
        },
        title = {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                placeholder = { Text(stringResource(R.string.explorer_filter_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}
