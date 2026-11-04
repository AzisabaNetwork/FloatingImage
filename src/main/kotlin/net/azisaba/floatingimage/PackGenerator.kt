package net.azisaba.floatingimage

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt

class PackGenerator(private val logger: Logger) {

    data class Result(val count: Int, val outputDir: File, val zipFile: File?, val sha1: String?)

    fun generate(
        entries: Collection<ImageEntry>,
        outputDir: File,
        zipFile: File?,
        maxTextureSize: Int,
        packFormat: Int,
    ): Result {
        val namespaceDir = File(outputDir, "assets/$NAMESPACE")
        namespaceDir.deleteRecursively()

        val itemsDir = File(namespaceDir, "items").apply { mkdirs() }
        val modelsDir = File(namespaceDir, "models/item").apply { mkdirs() }
        val texturesDir = File(namespaceDir, "textures/item").apply { mkdirs() }

        val generated = mutableListOf<ImageEntry>()
        for (entry in entries) {
            val file = entry.file ?: continue
            try {
                val source = ImageIO.read(file) ?: error("読み込み失敗")
                val texture = resize(source, maxTextureSize)
                ImageIO.write(texture, "png", File(texturesDir, "${entry.id}.png"))
                File(modelsDir, "${entry.id}.json").writeText(modelJson(entry.id, texture.width, texture.height))
                File(itemsDir, "${entry.id}.json").writeText(itemJson(entry.id))
                generated += entry
            } catch (e: Exception) {
                logger.warning("${file.name} の変換に失敗しました: ${e.message}")
            }
        }
        val count = generated.size

        File(namespaceDir, INDEX_FILE).writeText(ImageIndex.toJson(generated))

        val mcmeta = File(outputDir, "pack.mcmeta")
        if (!mcmeta.exists()) {
            mcmeta.writeText(
                """
                {
                  "pack": {
                    "description": "FloatingImage",
                    "pack_format": $packFormat,
                    "min_format": $packFormat,
                    "max_format": $packFormat
                  }
                }
                """.trimIndent() + "\n"
            )
        }

        var sha1: String? = null
        if (zipFile != null) {
            zip(outputDir, zipFile)
            sha1 = sha1(zipFile)
        }
        return Result(count, outputDir, zipFile, sha1)
    }

    private fun resize(source: BufferedImage, maxSize: Int): BufferedImage {
        val longest = max(source.width, source.height)
        val scale = if (longest > maxSize) maxSize.toDouble() / longest else 1.0
        val w = max(1, (source.width * scale).roundToInt())
        val h = max(1, (source.height * scale).roundToInt())
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(source, 0, 0, w, h, null)
        g.dispose()
        return out
    }

    private fun modelJson(id: String, width: Int, height: Int): String {
        val (w, h) = if (width >= height) 16.0 to 16.0 * height / width else 16.0 * width / height to 16.0
        val x0 = fmt(8 - w / 2)
        val x1 = fmt(8 + w / 2)
        val y0 = fmt(8 - h / 2)
        val y1 = fmt(8 + h / 2)
        val texture = "$NAMESPACE:item/$id"
        return """
            {
              "textures": {
                "image": "$texture",
                "particle": "$texture"
              },
              "elements": [
                {
                  "from": [$x0, $y0, 8],
                  "to": [$x1, $y1, 8],
                  "shade": false,
                  "light_emission": 15,
                  "faces": {
                    "south": { "uv": [0, 0, 16, 16], "texture": "#image" },
                    "north": { "uv": [16, 0, 0, 16], "texture": "#image" }
                  }
                }
              ]
            }
        """.trimIndent() + "\n"
    }

    private fun itemJson(id: String): String = """
        {
          "model": {
            "type": "minecraft:model",
            "model": "$NAMESPACE:item/$id"
          }
        }
    """.trimIndent() + "\n"

    private fun fmt(v: Double): String = String.format(Locale.ROOT, "%.4f", v).trimEnd('0').trimEnd('.')

    private fun zip(dir: File, zipFile: File) {
        zipFile.parentFile?.mkdirs()
        ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
            dir.walkTopDown().filter { it.isFile }.sortedBy { it.path }.forEach { file ->
                val name = file.relativeTo(dir).invariantSeparatorsPath
                zos.putNextEntry(ZipEntry(name))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    private fun sha1(file: File): String {
        val md = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val NAMESPACE = "floatingimage"
        const val INDEX_FILE = "index.json"
        const val INDEX_PATH = "assets/$NAMESPACE/$INDEX_FILE"
    }
}
