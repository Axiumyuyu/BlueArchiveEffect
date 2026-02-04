package me.axiumyu.blueArchiveEffect.attribute

import net.kyori.adventure.text.format.NamedTextColor


sealed interface Type{
    val id: String
    val displayName: String
    val color: NamedTextColor
}
