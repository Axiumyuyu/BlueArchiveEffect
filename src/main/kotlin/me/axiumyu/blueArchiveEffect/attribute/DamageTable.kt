package me.axiumyu.blueArchiveEffect.attribute

import me.axiumyu.blueArchiveEffect.attribute.AttackType.*
import me.axiumyu.blueArchiveEffect.attribute.DefenseType.*
import me.axiumyu.blueArchiveEffect.attribute.AttackModifier.*
import kotlin.to

object DamageTable {

    @JvmField
    val damageTable = mapOf(
        BURST hit LIGHT to WEAK,
        BURST hit SPECIAL to RESIST,
        BURST hit ELASTIC to RESIST,

        PIERCING hit HEAVY to WEAK,
        PIERCING hit LIGHT to RESIST,

        MYSTIC hit SPECIAL to WEAK,
        MYSTIC hit HEAVY to RESIST,
        MYSTIC hit COMPOSITE to RESIST,

        VIBER hit ELASTIC to WEAK,
        VIBER hit SPECIAL to EFFECTIVE,
        VIBER hit HEAVY to RESIST,
        VIBER hit COMPOSITE to RESIST,

        DECOMPOSE hit COMPOSITE to WEAK,
        DECOMPOSE hit HEAVY to EFFECTIVE,
        DECOMPOSE hit LIGHT to RESIST
    )

    infix fun AttackType.hit(armor: DefenseType) = Attack(this, armor)

    fun calculateBaseDamage(a: AttackType, d: DefenseType): AttackModifier {
        return damageTable[a hit d] ?: NC
    }

}