package me.axiumyu.blueArchiveEffect

import org.bukkit.scheduler.BukkitRunnable

class UpdateTask : BukkitRunnable() {
    override fun run() {
        HologramService.updateHolograms()
    }
}