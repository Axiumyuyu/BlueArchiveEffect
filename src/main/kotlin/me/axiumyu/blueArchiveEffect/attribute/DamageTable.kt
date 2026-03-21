package me.axiumyu.blueArchiveEffect.attribute

import me.axiumyu.blueArchiveEffect.attribute.AttackType.*
import me.axiumyu.blueArchiveEffect.attribute.DefenseType.*
import me.axiumyu.blueArchiveEffect.attribute.AttackModifier.*
import kotlin.to

object DamageTable {

    private val damageTable = mapOf(
        // 1. 爆发 (BURST) - 物理表现：横扫与大面积撕裂
        BURST hit LIGHT to WEAK,      // 轻易撕裂皮革/肉体
        BURST hit SPECIAL to EFFECTIVE, // 强动能扰乱魔法力场/高导能材质
        BURST hit HEAVY to RESIST,    // 砍在纯铁板上直接弹开卷刃

        // 2. 贯通 (PIERCING) - 物理表现：极点穿透
        PIERCING hit HEAVY to WEAK,      // 破甲箭精准穿透刚硬装甲的缝隙
        PIERCING hit LIGHT to EFFECTIVE, // 箭矢射中无甲肉体造成贯穿伤(次优解)
        PIERCING hit COMPOSITE to RESIST, // 极度致密、层层嵌套的合金完美卡死箭矢

        // 3. 分解 (DECOMPOSE) - 物理表现：钝器与强震荡波
        DECOMPOSE hit COMPOSITE to WEAK,  // 震荡波从内部破坏复合材料的层间粘合力
        DECOMPOSE hit HEAVY to EFFECTIVE, // 钝器砸重甲，虽然砸不穿但能震出内伤
        DECOMPOSE hit SPECIAL to RESIST, // 纯物理蛮力无法震碎虚无的魔法力场

        // 4. 神秘 (MYSTIC) - 物理表现：超自然法则与魔法
        MYSTIC hit SPECIAL to WEAK,     // 同源魔法法则碰撞，瞬间过载引爆
        MYSTIC hit COMPOSITE to EFFECTIVE,// 魔法能量无视物理厚度，直接熔毁合金内部
        MYSTIC hit LIGHT to RESIST    // 纯粹的自然生命血肉(Nature)天生排斥奥术(Arcane)
    )

    infix fun AttackType.hit(armor: DefenseType) = Attack(this, armor)

    fun calculateBaseDamage(a: AttackType, d: DefenseType): AttackModifier {
        return damageTable[a hit d] ?: NC
    }

}