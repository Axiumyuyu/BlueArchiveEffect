package me.axiumyu.blueArchiveEffect.listener

import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import me.axiumyu.blueArchiveEffect.attribute.AttackModifier
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DamageTable.calculateBaseDamage
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.modifier
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent

object DamageModifier : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onDamage(event: EntityDamageEvent) {
        val damageSource = event.damageSource
        val attacker = damageSource.causingEntity ?: return
        val defender = event.entity
        if (attacker !is LivingEntity || defender !is LivingEntity) return

        // 攻击属性
        var atkType = attacker.atkType
        if (damageSource.isIndirect) {
            when (damageSource.directEntity?.type) {
                EntityType.SPLASH_POTION -> {
                    atkType = AttackType.MYSTIC
                }

                else -> {}
            }
        }
//        val atkItemType = attacker.equipment?.itemInMainHand?.itemMeta?.atkType ?: return
//        val finalAtkType = if (atkItemType != AttackType.NORMAL_A) {
//            atkItemType
//        } else {
//            atkType
//        }

        // 防御属性
        val defType = defender.defType
//        val defItemType = defender.equipment?.chestplate?.itemMeta?.defType ?: return
//        val finalDefType = if (defItemType != DefenseType.NORMAL_D) {
//            defItemType
//        } else {
//            defType
//        }


        // 属性特效加成
        val atkEffect = attacker.modifier(true)
        val defEffect = defender.modifier(false)
        val rate = calculateBaseDamage(atkType, defType)

        val (sound, text) = when (rate) {
            AttackModifier.WEAK -> {
                event.damage *= (rate.value + atkEffect - defEffect).coerceAtLeast(0.0)
                Sound.ITEM_SHIELD_BREAK to "<red>WEAK!!"
            }

            AttackModifier.EFFECTIVE -> {
                event.damage *= (rate.value + (atkEffect - defEffect) / 2.0).coerceAtLeast(0.0)
                Sound.BLOCK_ENCHANTMENT_TABLE_USE to "<gold>EFFECTIVE!"
            }

            AttackModifier.RESIST -> {
                event.damage *= (rate.value - atkEffect + defEffect).coerceAtLeast(0.0)
                Sound.ITEM_SHIELD_BLOCK to "<blue>RESIST"
            }

            AttackModifier.NC -> {
                return
            }
        }

        (attacker as? Player)?.inform(sound, text, rate, true)
        (defender as? Player)?.inform(sound, text, rate, false)
    }

    fun Player.inform(sound: Sound, text: String, rate: AttackModifier, isAttacker: Boolean) {
        val prefix = if (isAttacker) "<green>你攻击了" else "<green>你被攻击了"
        this.playSound(this, sound, 1.0f, 1.0f)
        this.sendActionBar { mm.deserialize(text) }
    }
}