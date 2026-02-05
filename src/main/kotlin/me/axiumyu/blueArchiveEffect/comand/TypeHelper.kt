package me.axiumyu.blueArchiveEffect.comand

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.plugin
import me.axiumyu.blueArchiveEffect.Util.chooseSelectedEntity
import me.axiumyu.blueArchiveEffect.Util.node
import me.axiumyu.blueArchiveEffect.Util.error
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import me.axiumyu.blueArchiveEffect.config.Config
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.LivingEntity

object TypeHelper {
    fun register() {
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            // 根节点
            val root = node("batype")
                .requires { it.sender.hasPermission("batype.use") }

            // 1. Reload (Admin)
            val reloadNode = node("reload")
                .requires { it.sender.hasPermission("batype.admin") }
                .executes { ctx ->
                    Config.loadConfig()
                    ctx.source.sender.sendMessage(
                        mm.deserialize("<green>配置已重载！")
                    )
                    Command.SINGLE_SUCCESS
                }

            // === Check 分支 ===
            val checkNode = node("check")
                // check entity <selector> ...
                .then(node("entity")
                    .then(argument("target", ArgumentTypes.entity())
                        .then(node("atk").executes { checkEntity(it, true) })
                        .then(node("def").executes { checkEntity(it, false) })
                    )
                )
                // check item <selector> ...
                .then(node("item")
                    .then(argument("target", ArgumentTypes.entity())
                        .then(node("atk").executes { checkItem(it, true) })
                        .then(node("def").executes { checkItem(it, false) })
                    )
                )

            // === Set 分支 ===
            val setNode = node("set")
                .requires { it.sender.hasPermission("baeffect.admin") }
                // set entity <selector> <atk/def> <type>
                .then(node("entity")
                    .then(argument("target", ArgumentTypes.entity())
                        .then(node("atk")
                            .then(argument("type", StringArgumentType.word())
                                .suggests { _, b -> AttackType.entries.forEach { b.suggest(it.id) }; b.buildFuture() }
                                .executes { setEntity(it, true) }
                            )
                        )
                        .then(node("def")
                            .then(argument("type", StringArgumentType.word())
                                .suggests { _, b -> DefenseType.entries.forEach { b.suggest(it.id) }; b.buildFuture() }
                                .executes { setEntity(it, false) }
                            )
                        )
                    )
                )
                // set item <selector> <atk/def> <type>
                .then(node("item")
                    .then(argument("target", ArgumentTypes.entity())
                        .then(node("atk")
                            .then(argument("type", StringArgumentType.word())
                                .suggests { _, b -> AttackType.entries.forEach { b.suggest(it.id) }; b.buildFuture() }
                                .executes { setItem(it, true) }
                            )
                        )
                        .then(node("def")
                            .then(argument("type", StringArgumentType.word())
                                .suggests { _, b -> DefenseType.entries.forEach { b.suggest(it.id) }; b.buildFuture() }
                                .executes { setItem(it, false) }
                            )
                        )
                    )
                )

            // 注册
            root.then(checkNode)
            root.then(setNode)
            root.then(reloadNode)
            commands.register(root.build(), "BlueArchive RPG 属性管理")
        }
    }

    // ================= 实体逻辑 (Entity) =================

    private fun checkEntity(ctx: CommandContext<CommandSourceStack>, isAtk: Boolean): Int {
        val target = chooseSelectedEntity(ctx, "target") ?: return error(ctx, "未选择实体")

        if (isAtk) {
            // 使用扩展属性 .atkType
            val type = target.atkType
            ctx.sendMsgToSender(target.name, "自身攻击", type.displayName, type.color)
        } else {
            // 使用扩展属性 .defType
            val type = target.defType
            ctx.sendMsgToSender(target.name, "自身防御", type.displayName, type.color)
        }
        return Command.SINGLE_SUCCESS
    }

    private fun setEntity(ctx: CommandContext<CommandSourceStack>, isAtk: Boolean): Int {
        val target = chooseSelectedEntity(ctx, "target") ?: return error(ctx, "未选择实体")
        val typeId = StringArgumentType.getString(ctx, "type")

        if (isAtk) {
            val type = AttackType.fromId(typeId) ?: return error(ctx, "未知攻击类型: $typeId")
            // 直接赋值给扩展属性
            target.atkType = type
            ctx.sendMsgToSender(target.name, "自身攻击", type.displayName, type.color)
        } else {
            val type = DefenseType.fromId(typeId) ?: return error(ctx, "未知防御类型: $typeId")
            // 直接赋值给扩展属性
            target.defType = type
            ctx.sendMsgToSender(target.name, "自身防御", type.displayName, type.color)
        }
        return Command.SINGLE_SUCCESS
    }

    // ================= 物品逻辑 (Item) =================

    private fun checkItem(ctx: CommandContext<CommandSourceStack>, isAtk: Boolean): Int {
        val target = chooseSelectedEntity(ctx, "target") ?: return error(ctx, "未选择实体")
        if (target !is LivingEntity) return error(ctx, "该目标无法持有物品")

        val item = target.equipment?.itemInMainHand
        if (item == null || item.type.isAir) return error(ctx, "目标手中没有物品")

        val meta = item.itemMeta
        // ItemMeta 实现了 PersistentDataHolder，所以可以直接用 .atkType/.defType
        if (isAtk) {
            val type = meta.atkType
            ctx.sendMsgToSender(target.name, "手持物品攻击", type.displayName, type.color)
        } else {
            val type = meta.defType
            ctx.sendMsgToSender(target.name, "手持物品防御", type.displayName, type.color)
        }
        return Command.SINGLE_SUCCESS
    }

    private fun setItem(ctx: CommandContext<CommandSourceStack>, isAtk: Boolean): Int {
        val target = chooseSelectedEntity(ctx, "target") ?: return error(ctx, "未选择实体")
        if (target !is LivingEntity) return error(ctx, "该目标无法持有物品")

        val item = target.equipment?.itemInMainHand
        if (item == null || item.type.isAir) return error(ctx, "目标手中没有物品")

        val typeId = StringArgumentType.getString(ctx, "type")
        val meta = item.itemMeta

        if (isAtk) {
            val type = AttackType.fromId(typeId) ?: return error(ctx, "未知攻击类型")

            // 1. 设置数据 (使用扩展属性)
            meta.atkType = type

            ctx.sendMsgToSender(target.name, "手持物品攻击", type.displayName, type.color)
        } else {
            val type = DefenseType.fromId(typeId) ?: return error(ctx, "未知防御类型")

            // 1. 设置数据
            meta.defType = type

            ctx.sendMsgToSender(target.name, "手持物品防御", type.displayName, type.color)
        }

        // 3. 应用 Meta
        item.itemMeta = meta
        return Command.SINGLE_SUCCESS
    }

    private fun CommandContext<CommandSourceStack>.sendMsgToSender(target: String, context: String, value: String, color: NamedTextColor) {
        return source.sender.sendMessage(mm.deserialize("<gray>目标 <white>$target <gray>的 $context: <${color.asHexString()}>$value"))
    }
}