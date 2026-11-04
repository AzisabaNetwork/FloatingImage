package net.azisaba.floatingimage

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.util.logging.Logger
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO

data class ImageEntry(
    val id: String,
    val file: File?,
    val width: Int,
    val height: Int,
)

class ImageRegistry(private val logger: Logger) {

    @Volatile
    private var entries: Map<String, ImageEntry> = emptyMap()

    fun get(id: String): ImageEntry? = entries[id]

    fun ids(): List<String> = entries.keys.sorted()

    fun all(): Collection<ImageEntry> = entries.values

    fun loadLocal(imagesDir: File): Map<String, ImageEntry> {
        val result = scan(imagesDir, logger)
        entries = result
        return result
    }

    data class RemotePack(
        val url: String,
        val sha1: String,
        val entries: Map<String, ImageEntry>,
    )

    fun loadRemote(packUrl: String): RemotePack {
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build()

        val originalHost = URI.create(packUrl).host
        var uri = URI.create(packUrl)
        var pinned = uri
        var response: HttpResponse<ByteArray>
        var hops = 0
        while (true) {
            val request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "FloatingImage")
                .GET()
                .build()
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
            val location = response.headers().firstValue("Location").orElse(null)
            if (response.statusCode() !in 300..399 || location == null) break
            if (++hops > 10) error("リダイレクトが多すぎます: $packUrl")
            uri = uri.resolve(location)
            if (uri.host == originalHost) pinned = uri
        }
        if (response.statusCode() !in 200..299) {
            error("リソースパックを取得できません (HTTP ${response.statusCode()}): $uri")
        }

        val bytes = response.body()
        val sha1 = MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }
        val json = ZipInputStream(bytes.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }
                .firstOrNull { it.name.trimStart('/') == PackGenerator.INDEX_PATH }
                ?.let { zip.readBytes().toString(Charsets.UTF_8) }
        } ?: error("リソースパック内に ${PackGenerator.INDEX_PATH} がありません: $packUrl")

        val result = ImageIndex.parse(json).associateBy { it.id }
        entries = result
        return RemotePack(pinned.toString(), sha1, result)
    }

    companion object {
        val EXTENSIONS = setOf("png", "jpg", "jpeg")

        fun toId(name: String): String =
            name.lowercase().replace(Regex("[^a-z0-9_.-]"), "_").trim('_')

        fun scan(imagesDir: File, logger: Logger): Map<String, ImageEntry> {
            imagesDir.mkdirs()
            val result = linkedMapOf<String, ImageEntry>()
            val files = imagesDir.listFiles { f -> f.isFile && f.extension.lowercase() in EXTENSIONS }
                ?.sortedBy { it.name }
                .orEmpty()

            for (file in files) {
                val id = toId(file.nameWithoutExtension)
                if (id.isEmpty()) {
                    logger.warning("画像名を ID に変換できません: ${file.name}")
                    continue
                }
                if (id in result) {
                    logger.warning("ID '$id' が重複しています (${result[id]!!.file?.name} と ${file.name})。${file.name} は無視します")
                    continue
                }
                val image = runCatching { ImageIO.read(file) }.getOrNull()
                if (image == null) {
                    logger.warning("画像を読み込めません: ${file.name}")
                    continue
                }
                result[id] = ImageEntry(id, file, image.width, image.height)
            }
            return result
        }
    }
}
