package net.azisaba.floatingimage

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Display
import java.io.File

data class Settings(
    val source: Source,
    val remotePackUrl: String,
    val packOutputDir: File,
    val zipFile: File?,
    val packFormat: Int,
    val maxTextureSize: Int,
    val generateOnEnable: Boolean,
    val defaultSize: Float,
    val defaultBillboard: Display.Billboard,
    val viewRange: Float,
    val placeDistance: Double,
) {
    enum class Source { LOCAL, REMOTE }

    companion object {
        fun load(config: FileConfiguration, dataFolder: File): Settings {
            fun resolve(path: String): File =
                File(path).let { if (it.isAbsolute) it else File(dataFolder, path) }

            val billboard = config.getString("display.default-billboard", "CENTER")!!.uppercase()
            return Settings(
                source = if (config.getString("source", "local").equals("remote", ignoreCase = true))
                    Source.REMOTE else Source.LOCAL,
                remotePackUrl = config.getString("remote.pack-url", "")!!.trim(),
                packOutputDir = resolve(config.getString("pack.output-dir", "pack")!!),
                zipFile = if (config.getBoolean("pack.create-zip", true))
                    resolve(config.getString("pack.zip-file", "pack.zip")!!) else null,
                packFormat = config.getInt("pack.pack-format", 75),
                maxTextureSize = config.getInt("pack.max-texture-size", 512).coerceIn(16, 4096),
                generateOnEnable = config.getBoolean("load-on-enable", true),
                defaultSize = config.getDouble("display.default-size", 2.0).toFloat(),
                defaultBillboard = runCatching { Display.Billboard.valueOf(billboard) }
                    .getOrDefault(Display.Billboard.CENTER),
                viewRange = config.getDouble("display.view-range", 1.0).toFloat(),
                placeDistance = config.getDouble("display.place-distance", 2.0),
            )
        }
    }
}
