package net.azisaba.floatingimage

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Display
import org.bukkit.entity.Player

class FloatingImageCommand(private val plugin: FloatingImage) : TabExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (args.getOrNull(0)?.lowercase()) {
            "reload" -> reload(sender)
            "list" -> list(sender)
            "place" -> place(sender, label, args)
            "remove" -> remove(sender, label, args)
            else -> usage(sender, label)
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        val candidates = when (args.size) {
            1 -> SUBCOMMANDS
            2 -> if (args[0].equals("place", true)) plugin.registry.ids() else emptyList()
            4 -> if (args[0].equals("place", true)) Display.Billboard.entries.map { it.name.lowercase() } else emptyList()
            else -> emptyList()
        }
        val input = args.last().lowercase()
        return candidates.filter { it.startsWith(input) }
    }

    private fun usage(sender: CommandSender, label: String) {
        sender.sendRichMessage("<gold>/$label place <ID> [大きさ] [billboard]")
        sender.sendRichMessage("<gold>/$label remove [半径]")
        sender.sendRichMessage("<gold>/$label list")
        sender.sendRichMessage("<gold>/$label reload")
    }

    private fun reload(sender: CommandSender) {
        sender.sendRichMessage("<gray>画像を読み込み中...")
        plugin.reloadAll { message -> sender.sendRichMessage(message) }
    }

    private fun list(sender: CommandSender) {
        val entries = plugin.registry.all().sortedBy { it.id }
        if (entries.isEmpty()) {
            sender.sendRichMessage("<yellow>画像がありません。画像を追加してから /fimg reload してください")
            return
        }
        sender.sendRichMessage("<gold>画像一覧 (${entries.size})")
        entries.forEach {
            val source = it.file?.name?.let { name -> ", $name" } ?: ""
            sender.sendRichMessage("<gray>- <white>${it.id} <dark_gray>(${it.width}x${it.height}$source)")
        }
    }

    private fun place(sender: CommandSender, label: String, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendRichMessage("<red>プレイヤーのみ実行できます")
            return
        }
        val id = args.getOrNull(1)?.lowercase() ?: run {
            sender.sendRichMessage("<red>/$label place <ID> [大きさ] [billboard]")
            return
        }
        if (plugin.registry.get(id) == null) {
            sender.sendRichMessage("<red>画像 '$id' はありません。/$label list で確認してください")
            return
        }
        val size = args.getOrNull(2)?.let { it.toFloatOrNull()?.takeIf { v -> v > 0f && v <= 256f } }
            ?: if (args.size > 2) {
                sender.sendRichMessage("<red>大きさは 0 より大きく 256 以下の数値で指定してください")
                return
            } else plugin.settings.defaultSize
        val billboard = args.getOrNull(3)?.let { name ->
            runCatching { Display.Billboard.valueOf(name.uppercase()) }.getOrNull() ?: run {
                sender.sendRichMessage("<red>billboard は ${Display.Billboard.entries.joinToString { it.name.lowercase() }} のいずれかです")
                return
            }
        } ?: plugin.settings.defaultBillboard

        val eye = sender.eyeLocation
        val location = eye.clone().add(eye.direction.multiply(plugin.settings.placeDistance))
        location.yaw = eye.yaw + 180f
        location.pitch = 0f

        plugin.displays.spawn(id, location, size, billboard)
        sender.sendRichMessage("<green>画像 '$id' を設置しました")
    }

    private fun remove(sender: CommandSender, label: String, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendRichMessage("<red>プレイヤーのみ実行できます")
            return
        }
        val radius = args.getOrNull(1)?.let { it.toDoubleOrNull()?.takeIf { v -> v > 0 && v <= 64 } }
            ?: if (args.size > 1) {
                sender.sendRichMessage("<red>半径は 0 より大きく 64 以下の数値で指定してください")
                return
            } else 5.0
        val target = plugin.displays.findNearest(sender.location, radius)
        if (target == null) {
            sender.sendRichMessage("<yellow>半径 $radius ブロック以内に画像がありません")
            return
        }
        val id = plugin.displays.imageIdOf(target)
        target.remove()
        sender.sendRichMessage("<green>画像 '$id' を削除しました")
    }

    companion object {
        private val SUBCOMMANDS = listOf("place", "remove", "list", "reload")
    }
}
