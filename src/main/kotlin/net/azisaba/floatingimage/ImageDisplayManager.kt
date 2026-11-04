package net.azisaba.floatingimage

import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.key.Key
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f

class ImageDisplayManager(private val plugin: FloatingImage) {

    val imageKey = NamespacedKey(plugin, "image")

    fun createItem(id: String): ItemStack = ItemStack.of(Material.PAPER).apply {
        setData(DataComponentTypes.ITEM_MODEL, Key.key(PackGenerator.NAMESPACE, id))
    }

    fun spawn(
        id: String,
        location: Location,
        size: Float = plugin.settings.defaultSize,
        billboard: Display.Billboard = plugin.settings.defaultBillboard,
    ): ItemDisplay {
        val world = requireNotNull(location.world) { "location.world is null" }
        return world.spawn(location, ItemDisplay::class.java) { display ->
            display.setItemStack(createItem(id))
            display.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.NONE
            display.billboard = billboard
            display.brightness = Display.Brightness(15, 15)
            display.transformation = Transformation(
                Vector3f(),
                Quaternionf(),
                Vector3f(size, size, size),
                Quaternionf(),
            )
            display.viewRange = plugin.settings.viewRange
            display.shadowRadius = 0f
            display.shadowStrength = 0f
            display.isPersistent = true
            display.persistentDataContainer.set(imageKey, PersistentDataType.STRING, id)
        }
    }

    fun isImageDisplay(display: ItemDisplay): Boolean =
        display.persistentDataContainer.has(imageKey, PersistentDataType.STRING)

    fun imageIdOf(display: ItemDisplay): String? =
        display.persistentDataContainer.get(imageKey, PersistentDataType.STRING)

    fun findNearest(location: Location, radius: Double): ItemDisplay? {
        val world = location.world ?: return null
        return world.getNearbyEntitiesByType(ItemDisplay::class.java, location, radius)
            .filter { isImageDisplay(it) }
            .minByOrNull { it.location.distanceSquared(location) }
    }
}
