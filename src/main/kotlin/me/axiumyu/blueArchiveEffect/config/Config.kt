package me.axiumyu.blueArchiveEffect.config

import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.plugin
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.Attack
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason

object Config {
    val mobTypesList: MutableMap<EntityType, Attack> = mutableMapOf()

    val reasonBlackList: MutableList<SpawnReason> = mutableListOf()

    fun loadConfig() {
        val config = plugin.config
        val mobConfig = config.getConfigurationSection("mobs")

        val reasons = config.getStringList("reason-blacklist").mapNotNull { it ->
            runCatching {
                SpawnReason.valueOf(it)
            }.onFailure {
                plugin.logger.warning("Invalid spawn reason: $it, skipping")
            }.getOrNull()
        }

        reasonBlackList.clear()
        reasonBlackList.addAll(reasons)

        if (mobConfig == null) {
            plugin.logger.warning("Mob config is empty, skipping")
            return
        }

        mobTypesList.clear()

        mobConfig.getKeys(false).forEach { it ->

            //mob type
            val type = runCatching {
                EntityType.valueOf(it.uppercase())
            }.onFailure {
                plugin.logger.warning("Invalid mob type: $it, skipping")
            }.getOrNull() ?: return@forEach

            //atk type
            val atk = mobConfig.getString("${it}.atk")
            val atkType = AttackType.fromId(atk) ?: run {
                plugin.logger.warning("Invalid attack type: $atk, default to NORMAL_A")
                AttackType.NORMAL_A
            }

            //def type
            val def = mobConfig.getString("${it}.def")
            val defType = DefenseType.fromId(def) ?: run {
                plugin.logger.warning("Invalid defense type: $def, default to NORMAL_D")
                DefenseType.NORMAL_D
            }

            val types = Attack(atkType, defType)
            mobTypesList[type] = types
        }
    }
}
