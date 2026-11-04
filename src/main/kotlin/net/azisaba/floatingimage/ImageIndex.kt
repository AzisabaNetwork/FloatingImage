package net.azisaba.floatingimage

object ImageIndex {

    fun toJson(entries: Collection<ImageEntry>): String = buildString {
        append("{\n  \"images\": [")
        entries.sortedBy { it.id }.forEachIndexed { i, e ->
            if (i > 0) append(",")
            append("\n    {\"id\": \"${e.id}\", \"width\": ${e.width}, \"height\": ${e.height}}")
        }
        if (entries.isNotEmpty()) append("\n  ")
        append("]\n}\n")
    }

    private val ENTRY = Regex(
        """\{\s*"id"\s*:\s*"([a-z0-9_.-]+)"\s*,\s*"width"\s*:\s*(\d+)\s*,\s*"height"\s*:\s*(\d+)\s*}"""
    )

    fun parse(json: String): List<ImageEntry> =
        ENTRY.findAll(json).map {
            ImageEntry(it.groupValues[1], null, it.groupValues[2].toInt(), it.groupValues[3].toInt())
        }.toList()
}
