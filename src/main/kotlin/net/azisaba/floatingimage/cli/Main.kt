@file:JvmName("Main")

package net.azisaba.floatingimage.cli

import net.azisaba.floatingimage.ImageRegistry
import net.azisaba.floatingimage.PackGenerator
import java.io.File
import java.util.logging.Logger
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty() || args[0] != "generate") usage()

    val options = mutableMapOf<String, String>()
    var i = 1
    while (i < args.size) {
        val key = args[i]
        if (!key.startsWith("--") || i + 1 >= args.size) usage()
        options[key.removePrefix("--")] = args[i + 1]
        i += 2
    }

    val imagesDir = File(options["images"] ?: usage())
    val outDir = File(options["out"] ?: usage())
    val maxSize = options["max-size"]?.toIntOrNull() ?: 512
    val packFormat = options["pack-format"]?.toIntOrNull() ?: 75

    if (!imagesDir.isDirectory) {
        System.err.println("images ディレクトリがありません: ${imagesDir.path}")
        exitProcess(1)
    }

    val logger = Logger.getLogger("FloatingImage")
    val entries = ImageRegistry.scan(imagesDir, logger)
    val result = PackGenerator(logger).generate(entries.values, outDir, null, maxSize, packFormat)

    println("Generated ${result.count} image(s) into ${File(outDir, "assets/${PackGenerator.NAMESPACE}").path}")
    if (result.count < entries.size) exitProcess(1)
}

private fun usage(): Nothing {
    System.err.println(
        """
        Usage: java -jar FloatingImage.jar generate --images <dir> --out <pack root> [--max-size 512] [--pack-format 75]
        """.trimIndent()
    )
    exitProcess(2)
}
