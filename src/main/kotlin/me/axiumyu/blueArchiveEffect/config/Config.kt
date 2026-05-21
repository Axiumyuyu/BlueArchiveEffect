package me.axiumyu.blueArchiveEffect.config

import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.plugin
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.AtkDef
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DamageTable.hit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason

object Config {

    val mobTypesList: MutableMap<EntityType, AtkDef> = mutableMapOf()

    val variants: MutableMap<EntityType, HashMap<SpawnReason, AtkDef>> = mutableMapOf()
    val reasonBlackList: MutableList<SpawnReason> = mutableListOf()

    var variationRate = 0.2

    fun loadConfig() {
        val config = plugin.config
        variationRate = config.getDouble("variation-rate", 0.2)
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

        mobConfig.getKeys(false).forEach { mobStr ->

            //mob type
            val type = runCatching {
                EntityType.valueOf(mobStr.uppercase())
            }.onFailure {
                plugin.logger.warning("Invalid mob type: $it, skipping")
            }.getOrNull() ?: return@forEach

            val default = loadMob(mobConfig, mobStr)

            mobTypesList[type] = default

            // override config
            val overrideTypeConfig = mobConfig.getConfigurationSection("${mobStr}.override") ?: return@forEach
            val overrideList = hashMapOf<SpawnReason, AtkDef>()
            overrideTypeConfig.getKeys(false).forEach { key ->
                val reason = runCatching {
                    SpawnReason.valueOf(key.uppercase())
                }.onFailure {
                    plugin.logger.warning("Invalid spawn reason: $key, skipping")
                }.getOrNull() ?: return@forEach
                val attack = loadMob(overrideTypeConfig, key)
                if (attack.atk != AttackType.NORMAL_A || attack.def != DefenseType.NORMAL_D){
                    overrideList[reason] = attack
                }
            }
            if (overrideList.isNotEmpty()) {
                variants[type] = overrideList
            }
        }
    }

    private fun loadMob(mobConfig: ConfigurationSection, key: String): AtkDef {
        val atk = mobConfig.getString("${key}.atk")
        val atkType = AttackType.fromId(atk) ?: run {
            plugin.logger.warning("Invalid attack type: $atk, default to NORMAL_A")
            AttackType.NORMAL_A
        }

        //def type
        val def = mobConfig.getString("${key}.def")
        val defType = DefenseType.fromId(def) ?: run {
            plugin.logger.warning("Invalid defense type: $def, default to NORMAL_D")
            DefenseType.NORMAL_D
        }
        return atkType hit defType
    }
}