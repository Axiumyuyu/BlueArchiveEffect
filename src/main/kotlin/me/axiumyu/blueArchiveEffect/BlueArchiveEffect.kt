package me.axiumyu.blueArchiveEffect

import com.github.retrooper.packetevents.PacketEvents
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import me.axiumyu.blueArchiveEffect.comand.TypeHelper
import me.axiumyu.blueArchiveEffect.comand.TypeHelperChatGPT
import me.axiumyu.blueArchiveEffect.comand.TypeHelperNew
import me.axiumyu.blueArchiveEffect.config.Config
import me.axiumyu.blueArchiveEffect.listener.DamageModifier
import me.axiumyu.blueArchiveEffect.listener.MobSpawnType
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import kotlin.getValue

class BlueArchiveEffect : JavaPlugin() {

    companion object{
        @JvmField
        val mm = MiniMessage.miniMessage()

        val plugin by lazy(LazyThreadSafetyMode.NONE) { getPlugin(BlueArchiveEffect::class.java) }

        const val NAMESPACE_KEY = "ba_attr"

        val updateTask by lazy(LazyThreadSafetyMode.NONE) { UpdateTask() }

    }
    override fun onLoad() {
        super.onLoad()
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().load()

        TypeHelperNew.register()
//        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
//            TypeHelperChatGPT.register(event.registrar())
//        }
        updateTask.runTaskTimer(this,  1L, 1L)
    }

    override fun onEnable() {
        saveDefaultConfig()
        Config.loadConfig()

        PacketEvents.getAPI().init()

        listOf(
            DamageModifier,
            MobSpawnType
        ).forEach {
            server.pluginManager.registerEvents(it, this)
        }
    }

    override fun onDisable() {
        PacketEvents.getAPI().terminate()
        updateTask.cancel()
    }
}
