package dev.cluvex.strata.core.util

import androidx.annotation.StringRes
import dev.cluvex.strata.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Difference
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import dev.cluvex.strata.core.model.FileItem

enum class FileKind {
    FOLDER, IMAGE, VIDEO, AUDIO, ARCHIVE, APK, CODE, TEXT, PDF, FONT, DOCUMENT, ENCRYPTED, BINARY;

    val icon: ImageVector
        get() = when (this) {
            FOLDER -> Icons.Rounded.Folder
            IMAGE -> Icons.Rounded.Image
            VIDEO -> Icons.Rounded.Movie
            AUDIO -> Icons.Rounded.Audiotrack
            ARCHIVE -> Icons.Rounded.FolderZip
            APK -> Icons.Rounded.Android
            CODE -> Icons.Rounded.Code
            TEXT -> Icons.AutoMirrored.Rounded.TextSnippet
            PDF -> Icons.Rounded.PictureAsPdf
            FONT -> Icons.Rounded.FontDownload
            DOCUMENT -> Icons.Rounded.Description
            ENCRYPTED -> Icons.Rounded.Lock
            BINARY -> Icons.AutoMirrored.Rounded.InsertDriveFile
        }

    @get:StringRes
    val label: Int
        get() = when (this) {
            FOLDER -> R.string.common_folder
            IMAGE -> R.string.model_open_as_image
            VIDEO -> R.string.model_open_as_video
            AUDIO -> R.string.model_open_as_audio
            ARCHIVE -> R.string.files_kind_archive
            APK -> R.string.files_kind_apk
            CODE -> R.string.files_kind_code
            TEXT -> R.string.model_open_as_text
            PDF -> R.string.files_kind_pdf
            FONT -> R.string.files_kind_font
            DOCUMENT -> R.string.files_kind_document
            ENCRYPTED -> R.string.files_kind_encrypted
            BINARY -> R.string.common_file
        }
}

/**
 * Finer-grained flavours of [FileKind.TEXT]: the kind decides how the file is opened, while the
 * variant only changes how it looks in a listing. Everything in the text family used to share one
 * glyph and one hue, which made `notes.txt`, `README.md` and `config.json` indistinguishable.
 */
enum class FileVariant(val icon: ImageVector) {
    MARKDOWN(Icons.Rounded.Article),
    JSON(Icons.Rounded.DataObject),
    XML(Icons.Rounded.Code),
    YAML(Icons.Rounded.Settings),
    CSV(Icons.Rounded.TableChart),
    LOG(Icons.Rounded.Notes),
    CONFIG(Icons.Rounded.Tune),
    DIFF(Icons.Rounded.Difference),
}

object FileKinds {

    private val image = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "heif", "ico", "svg")
    private val video = setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "flv", "ts", "m4v")
    private val audio = setOf("mp3", "flac", "wav", "ogg", "m4a", "aac", "opus", "mid", "amr")
    private val archive = setOf(
        "zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "zst", "zstd", "lz4",
        "jar", "apks", "xapk", "apkm", "war",
    )
    private val code = setOf(
        "kt", "kts", "java", "c", "cc", "cpp", "h", "hpp", "cs", "go", "rs", "swift",
        "py", "rb", "js", "ts", "tsx", "jsx", "php", "lua", "dart", "sh", "bat",
        "gradle", "smali", "asm", "sql", "html", "css", "scss", "vue", "zig",
    )
    private val text = setOf(
        "txt", "md", "json", "xml", "yml", "yaml", "toml", "ini", "cfg", "conf",
        "properties", "prop", "log", "csv", "tsv", "mtl", "mtsx", "diff", "patch",
        "markdown", "mdown", "mkd", "mdx", "json5", "jsonc", "ndjson", "plist",
    )
    private val font = setOf("ttf", "otf", "woff", "woff2", "ttc")
    private val document = setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "rtf", "epub")

    private val variants: Map<String, FileVariant> = buildMap {
        fun putAll(variant: FileVariant, vararg extensions: String) = extensions.forEach { put(it, variant) }
        putAll(FileVariant.MARKDOWN, "md", "markdown", "mdown", "mkd", "mdx")
        putAll(FileVariant.JSON, "json", "json5", "jsonc", "ndjson")
        putAll(FileVariant.XML, "xml", "xsd", "xsl", "xslt", "plist")
        putAll(FileVariant.YAML, "yml", "yaml")
        putAll(FileVariant.CSV, "csv", "tsv")
        putAll(FileVariant.LOG, "log")
        putAll(FileVariant.CONFIG, "ini", "cfg", "conf", "properties", "prop", "toml")
        putAll(FileVariant.DIFF, "diff", "patch")
    }

    /** The look of a file inside [FileKind.TEXT], or null when the generic text glyph is right. */
    fun variantOf(item: FileItem): FileVariant? {
        if (item.isDirectory) return null
        if (of(item) != FileKind.TEXT) return null
        return variants[item.extension]
    }

    fun of(item: FileItem): FileKind {
        if (item.isDirectory) return FileKind.FOLDER
        val ext = item.extension
        return when {
            ext == "apk" -> FileKind.APK
            ext == "pdf" -> FileKind.PDF
            ext == "senc" -> FileKind.ENCRYPTED
            ext in image -> FileKind.IMAGE
            ext in video -> FileKind.VIDEO
            ext in audio -> FileKind.AUDIO
            ext in archive -> FileKind.ARCHIVE
            ext in code -> FileKind.CODE
            ext in text -> FileKind.TEXT
            ext in font -> FileKind.FONT
            ext in document -> FileKind.DOCUMENT
            else -> FileKind.BINARY
        }
    }

    fun isImage(item: FileItem) = of(item) == FileKind.IMAGE
    fun isVideo(item: FileItem) = of(item) == FileKind.VIDEO
    fun isThumbnailable(item: FileItem) = isImage(item) || isVideo(item) || item.extension == "apk"
    fun isTextEditable(item: FileItem): Boolean {
        val k = of(item)
        return k == FileKind.TEXT || k == FileKind.CODE
    }
    fun isArchive(item: FileItem) = of(item) == FileKind.ARCHIVE
}
