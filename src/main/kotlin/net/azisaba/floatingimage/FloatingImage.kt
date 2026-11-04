package net.azisaba.floatingimage

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class FloatingImage : JavaPlugin() {

    lateinit var settings: Settings
        private set
    lateinit var registry: ImageRegistry
        private set
    lateinit var displays: ImageDisplayManager
        private set

    private val packGenerator by lazy { PackGenerator(logger) }
    private val reloading = AtomicBoolean(false)

    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        settings = Settings.load(config, dataFolder)
        registry = ImageRegistry(logger)
        displays = ImageDisplayManager(this)

        val command = FloatingImageCommand(this)
        getCommand("fimg")!!.apply {
            setExecutor(command)
            tabCompleter = command
        }

        if (settings.generateOnEnable) {
            reloadAll(reloadConfig = false) { logger.info(it.replace(Regex("<[^>]+>"), "")) }
        }
    }

    override fun onDisable() {
        instance = null
    }

    fun reloadAll(reloadConfig: Boolean = true, report: (String) -> Unit) {
        if (!reloading.compareAndSet(false, true)) {
            report("<yellow>すでに読み込み中です")
            return
        }
        if (reloadConfig) {
            reloadConfig()
            settings = Settings.load(config, dataFolder)
        }
        val s = settings
        val remoteUrl = s.remotePackUrl.ifEmpty { server.resourcePack }

        server.scheduler.runTaskAsynchronously(this, Runnable {
            val message = try {
                when (s.source) {
                    Settings.Source.LOCAL -> loadLocal(s)
                    Settings.Source.REMOTE -> {
                        val pack = loadRemote(remoteUrl)
                        "<green>リソースパックから画像 ${pack.entries.size} 枚を読み込みました<newline><gray>${pack.url}"
                    }
                }
            } catch (e: Exception) {
                logger.severe("画像の読み込みに失敗しました: $e")
                "<red>画像の読み込みに失敗しました: ${e.message}"
            } finally {
                reloading.set(false)
            }
            server.scheduler.runTask(this, Runnable { report(message) })
        })
    }

    private fun loadLocal(s: Settings): String {
        val entries = registry.loadLocal(File(dataFolder, "images"))
        val result = packGenerator.generate(
            entries.values, s.packOutputDir, s.zipFile, s.maxTextureSize, s.packFormat,
        )
        return buildString {
            append("<green>画像 ${result.count} 枚を読み込み、リソースパックを生成しました")
            append("<newline><gray>出力先: ${result.outputDir.path}")
            if (result.zipFile != null) {
                append("<newline><gray>zip: ${result.zipFile.path}")
                append("<newline><gray>sha1: <white>${result.sha1}")
            }
            append("<newline><yellow>リソースパックを再配布するまで、新しい画像はプレイヤーに表示されません")
        }
    }

    private fun loadRemote(url: String): ImageRegistry.RemotePack {
        if (url.isBlank()) error("remote.pack-url が空で、server.properties の resource-pack も設定されていません")
        return registry.loadRemote(url)
    }

    companion object {
        @JvmStatic
        var instance: FloatingImage? = null
            private set
    }
}
