package me.axiumyu.blueArchiveEffect.listener

import me.axiumyu.blueArchiveEffect.Util.drop
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import me.axiumyu.blueArchiveEffect.config.Config.mobTypesList
import me.axiumyu.blueArchiveEffect.config.Config.reasonBlackList
import me.axiumyu.blueArchiveEffect.config.Config.variationRate
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import kotlin.random.Random

object MobSpawnType : Listener {

    private val atkType = AttackType.entries.drop(AttackType.NORMAL_A)
    private val defType = DefenseType.entries.drop(DefenseType.NORMAL_D)

    @EventHandler
    fun onMobSpawn(event: CreatureSpawnEvent) {
        val entity = event.entity

        // 特殊规则1：末影龙随机属性
        if (entity.type == EntityType.ENDER_DRAGON) {
            random(entity)
            return
        }

        // 特殊规则2：末地主岛末影人随机属性
        if (entity.type == EntityType.ENDERMAN && entity.world.name.contains("end")) {
            val loc = entity.location
            if (loc.x in -512.0..512.0 && loc.z in -512.0..512.0) {
                random(entity)
                return
            }
        }

        // 配置文件中的variation rate
        if (Random.nextFloat() <= variationRate) {
            random(entity)
            return
        }

        // 默认规则
        if (reasonBlackList.contains(event.spawnReason)) return
        if (!mobTypesList.keys.contains(event.entityType)) return
        entity.atkType = mobTypesList[event.entityType]!!.atk
        entity.defType = mobTypesList[event.entityType]!!.def
    }


    private fun random(entity: LivingEntity) {
        entity.atkType = atkType.random()
        entity.defType = defType.random()
    }
}