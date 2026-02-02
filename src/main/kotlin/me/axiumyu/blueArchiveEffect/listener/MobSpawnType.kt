package me.axiumyu.blueArchiveEffect.listener

import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import me.axiumyu.blueArchiveEffect.config.Config.mobTypesList
import me.axiumyu.blueArchiveEffect.config.Config.reasonBlackList
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent

object MobSpawnType : Listener {

    @EventHandler
    fun onMobSpawn(event: CreatureSpawnEvent) {
        val entity = event.entity
        if (entity.type == EntityType.ENDER_DRAGON){
            entity.atkType = AttackType.entries.random()
            entity.defType = DefenseType.entries.random()
        }
        if (reasonBlackList.contains(event.spawnReason)) return
        if (!mobTypesList.keys.contains(event.entityType)) return
        entity.atkType = mobTypesList[event.entityType]!!.atk
        entity.defType = mobTypesList[event.entityType]!!.def
    }
}