package me.axiumyu.blueArchiveEffect.attribute

import net.kyori.adventure.text.format.NamedTextColor

enum class DefenseType (
    val id: String,
    val displayName: String,
    val color: NamedTextColor
){
    NORMAL_D("normal", "常规装甲", NamedTextColor.GRAY),
    LIGHT("light", "轻装甲", NamedTextColor.RED),
    HEAVY("heavy", "重装甲", NamedTextColor.YELLOW),
    SPECIAL("mystic", "特殊装甲", NamedTextColor.BLUE),
    ELASTIC("elastic", "弹力装甲", NamedTextColor.LIGHT_PURPLE),
    COMPOSITE("composite", "复合装甲", NamedTextColor.GREEN);

    companion object {
        fun fromId(id: String?): DefenseType? = DefenseType.entries.find { it.id == id }
    }
}