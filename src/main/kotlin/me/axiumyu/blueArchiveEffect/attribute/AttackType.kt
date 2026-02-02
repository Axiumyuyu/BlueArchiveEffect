package me.axiumyu.blueArchiveEffect.attribute

import net.kyori.adventure.text.format.NamedTextColor

enum class AttackType(
    val id: String,
    val displayName: String,
    val color: NamedTextColor
) {
    NORMAL_A("normal", "常规", NamedTextColor.GRAY),
    BURST("burst", "爆发", NamedTextColor.RED),
    PIERCING("piercing", "贯通", NamedTextColor.YELLOW),
    MYSTIC("mystic", "神秘", NamedTextColor.BLUE),
    VIBER("viber", "振动", NamedTextColor.LIGHT_PURPLE),
    DECOMPOSE("decompose", "分解", NamedTextColor.GREEN);

    companion object {
        fun fromId(id: String?): AttackType? = entries.find { it.id == id }
    }
}