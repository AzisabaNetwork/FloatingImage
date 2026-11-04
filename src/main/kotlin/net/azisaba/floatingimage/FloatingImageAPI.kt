package net.azisaba.floatingimage

import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay

object FloatingImageAPI {

    private val plugin: FloatingImage
        get() = FloatingImage.instance ?: error("FloatingImage is not enabled")

    @JvmStatic
    fun ids(): List<String> = plugin.registry.ids()

    @JvmStatic
    fun exists(id: String): Boolean = plugin.registry.get(id) != null

    @JvmStatic
    @JvmOverloads
    fun spawn(
        id: String,
        location: Location,
        size: Float = plugin.settings.defaultSize,
        billboard: Display.Billboard = plugin.settings.defaultBillboard,
    ): ItemDisplay {
        require(exists(id)) { "Unknown image: $id" }
        return plugin.displays.spawn(id, location, size, billboard)
    }

    @JvmStatic
    fun isImageDisplay(display: ItemDisplay): Boolean = plugin.displays.isImageDisplay(display)
}
