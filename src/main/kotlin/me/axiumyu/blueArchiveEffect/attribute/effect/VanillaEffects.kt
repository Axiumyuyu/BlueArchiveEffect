package me.axiumyu.blueArchiveEffect.attribute.effect

import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.modifier
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.potion.PotionEffectType

object VanillaEffects : Listener {

    @EventHandler
    fun onEffectApply(event: EntityPotionEffectEvent) {
        val entity = event.entity as? LivingEntity ?: return
        val newType = event.newEffect?.type
        val oldType = event.oldEffect?.type
        when (event.action) {
            EntityPotionEffectEvent.Action.ADDED -> addModifier(newType, entity, 0.15)
            EntityPotionEffectEvent.Action.CHANGED -> addModifier(newType, entity, 0.15)
            EntityPotionEffectEvent.Action.REMOVED -> addModifier(oldType, entity, 0.0)
            EntityPotionEffectEvent.Action.CLEARED -> addModifier(oldType, entity, 0.0)
        }
    }

    private fun addModifier(type: PotionEffectType?, entity: Entity, value: Double) {
        when (type) {
            PotionEffectType.OOZING -> entity.modifier(AttackType.MYSTIC, value)
            PotionEffectType.WIND_CHARGED -> entity.modifier(AttackType.VIBER, value)
            PotionEffectType.WEAVING -> entity.modifier(AttackType.BURST, value)
            PotionEffectType.INVISIBILITY -> entity.modifier(AttackType.DECOMPOSE, value)
            PotionEffectType.INFESTED -> entity.modifier(AttackType.PIERCING, value)
        }
    }
}