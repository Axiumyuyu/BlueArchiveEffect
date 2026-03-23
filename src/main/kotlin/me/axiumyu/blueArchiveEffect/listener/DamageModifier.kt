package me.axiumyu.blueArchiveEffect.listener

import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import me.axiumyu.blueArchiveEffect.attribute.AttackModifier
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DamageTable.calculateBaseDamage
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.modifier
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent

data class Reeeeesponse(
    val sound: Sound,
    val vol: Pair<Float, Float> = (1f to 1f),
    val result: AttackModifier,
    val particle: Particle,
    val particleData: Any? = null,
    val isAttacker: Boolean
)

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
        val playerDefend = defender is Player

        val reeeeesponse = when (rate) {
            AttackModifier.WEAK -> {
                event.damage *= (rate.value + atkEffect - defEffect).coerceAtLeast(0.0)
                if (playerDefend) {
                    // 玩家没有防住攻击
                    Reeeeesponse(
                        sound = Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO,
                        vol = (1f to 1.1f),
                        result = AttackModifier.WEAK,
                        particle = Particle.RAID_OMEN,
                        isAttacker = false
                    )
                } else {
                    // 玩家打出了高伤害
                    Reeeeesponse(
                        sound = Sound.BLOCK_NOTE_BLOCK_BELL,
                        vol = (0.8f to 1.5f),
                        result = AttackModifier.WEAK,
                        particle = Particle.ELECTRIC_SPARK,
                        isAttacker = true
                    )
                }
            }

            AttackModifier.EFFECTIVE -> {
                event.damage *= (rate.value + (atkEffect - defEffect) / 2.0).coerceAtLeast(0.0)
                if (playerDefend) {
                    // 玩家没有防住部分攻击
                    Reeeeesponse(
                        sound = Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO,
                        vol = (0.9f to 1.3f),
                        result = AttackModifier.EFFECTIVE,
                        particle = Particle.RAID_OMEN,
                        isAttacker = false
                    )
                } else {
                    // 玩家打出了较高伤害
                    Reeeeesponse(
                        sound = Sound.BLOCK_NOTE_BLOCK_CHIME,
                        result = AttackModifier.EFFECTIVE,
                        particle = Particle.GLOW,
                        isAttacker = true
                    )
                }
            }

            AttackModifier.RESIST -> {
                event.damage *= (rate.value - atkEffect + defEffect).coerceAtLeast(0.0)
                if (playerDefend) {
                    // 玩家防住了攻击
                    Reeeeesponse(
                        sound = Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE,
                        vol = (0.8f to 0.8f),
                        particle = Particle.WHITE_SMOKE,
                        result = AttackModifier.RESIST,
                        isAttacker = false
                    )
                } else {
                    // 玩家打出了低伤害
                    Reeeeesponse(
                        sound = Sound.BLOCK_NOTE_BLOCK_SNARE,
                        vol = (1f to .5f),
                        result = AttackModifier.RESIST,
                        particle = Particle.WAX_OFF,
                        isAttacker = true
                    )
                }
            }

            AttackModifier.NC -> {
                return
            }
        }

        (attacker as? Player)?.inform(reeeeesponse)
        (defender as? Player)?.inform(reeeeesponse)
    }

    fun Player.inform(res: Reeeeesponse) {
        val particle = res.particle
        val sound = res.sound
        val (vol, pitch) = res.vol
        val isAttacker = res.isAttacker
        val rate = res.result
        val rateStr = if (isAttacker) {
            when (rate) {
                AttackModifier.WEAK -> "<red>高效"
                AttackModifier.EFFECTIVE -> "<orange>有效"
                AttackModifier.RESIST -> "<blue>无效"
                else -> ""
            }
        } else {
            when (rate) {
                AttackModifier.WEAK -> "<blue>穿透"
                AttackModifier.EFFECTIVE -> "<orange>弱化"
                AttackModifier.RESIST -> "<red>高效"
                else -> ""
            }
        }


        val prefix = if (isAttacker) "伤害" else "防御"
        val msg = "${rateStr}${prefix}"
        sendActionBar(mm.deserialize(msg))
        playSound(this, sound, vol, pitch)
        world.spawnParticle(particle, location.add(0.0, 1.5, 0.0), 35)
    }
}