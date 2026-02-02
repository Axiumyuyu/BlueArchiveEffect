package me.axiumyu.blueArchiveEffect.comand

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.plugin
import me.axiumyu.blueArchiveEffect.Util.chooseSelectedEntity
import me.axiumyu.blueArchiveEffect.Util.node
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.config.Config
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import kotlin.jvm.java

/*
 * /batype check <item/player> <selector> <atk/def> (需要baeffect.use权限)
 * /batype set <item/player> <selector> <atk/def> <type>(需要baeffect.admin权限,若有modifier,则必须指明数值)
 * /batype reload(需要baeffect.admin权限)
 * e.g.
 * /batype check @n[type=creeper] atk
 * /batype set @s atk BURST
 * /batype set axiumyu atk burst
 */
@Deprecated("Use TypeHelperNew instead")
object TypeHelper {



    fun register() {
        val manager = plugin.lifecycleManager
        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            // 根命令 /batype
            val root = node("batype")
                .requires { it.sender.hasPermission("baeffect.use") }

            // 1. Reload (Admin)
            val reloadNode = node("reload")
                .requires { it.sender.hasPermission("baeffect.admin") }
                .executes { ctx ->
                    Config.loadConfig()
                    ctx.source.sender.sendMessage(
                        mm.deserialize("<green>配置已重载！")
                    )
                    Command.SINGLE_SUCCESS
                }

            // 2. Check (Use)
            // /batype check <selector> <atk/def>
            val checkNode = node("check")
                .then(
                    Commands.argument("target", ArgumentTypes.entity()) // 选择单个实体
                        .then(
                            node("atk")
                                .executes { ctx -> checkAttribute(ctx, true) }
                        )
                        .then(
                            node("def")
                                .executes { ctx -> checkAttribute(ctx, false) }
                        )
                )

            // 3. Set (Admin)
            // /batype set <selector> atk <type>
            // /batype set <selector> def <type>
            val setNode = node("set")
                .requires { it.sender.hasPermission("baeffect.admin") }
                .then(
                    argument("target", ArgumentTypes.entity())
                        // 分支：设置攻击属性
                        .then(
                            node("atk")
                            .then(
                                argument("type", StringArgumentType.word())
                                    .suggests { _, builder ->
                                        // 自动补全所有攻击类型ID
                                        AttackType.entries.forEach { builder.suggest(it.id) }
                                        builder.buildFuture()
                                    }
                                    .executes { ctx -> setAttribute(ctx, true) }
                            )
                        )
                        // 分支：设置防御属性
                        .then(
                            node("def")
                            .then(
                                argument("type", StringArgumentType.word())
                                    .suggests { _, builder ->
                                        // 自动补全所有防御类型ID
                                        DefenseType.entries.forEach { builder.suggest(it.id) }
                                        builder.buildFuture()
                                    }
                                    .executes { ctx -> setAttribute(ctx, false) }
                            )
                        )

                )

            // 组装命令树
            root.then(reloadNode)
            root.then(checkNode)
            root.then(setNode)

            // 注册
            commands.register(root.build(), "BlueArchive RPG 属性管理指令")
        }
    }

    // --- 执行逻辑：检查属性 ---
    private fun checkAttribute(ctx: CommandContext<CommandSourceStack>, isAttack: Boolean): Int {
        val sender = ctx.source.sender
        val targetEntity = chooseSelectedEntity(ctx,"target")

        if (targetEntity !is LivingEntity) {
            sender.sendMessage(mm.deserialize("<red>错误: 目标必须是生物！"))
            return 0
        }

        if (isAttack) {
            val type = targetEntity.atkType
            sender.sendMessage(formatMessage(targetEntity.name, "攻击属性", type.displayName, type.color))
        } else {
            val type = targetEntity.defType
            sender.sendMessage(formatMessage(targetEntity.name, "防御属性", type.displayName, type.color))
        }
        return Command.SINGLE_SUCCESS
    }

    // --- 执行逻辑：设置属性 ---
    private fun setAttribute(ctx: CommandContext<CommandSourceStack>, isAttack: Boolean): Int {
        val sender = ctx.source.sender
        val targetEntity = chooseSelectedEntity(ctx, "target")
        val typeId = StringArgumentType.getString(ctx, "type")

        if (targetEntity !is LivingEntity) {
            sender.sendMessage(mm.deserialize("<red>错误: 目标必须是生物！"))
            return 0
        }

        if (isAttack) {
            val type = AttackType.fromId(typeId)
            if (type == null) {
                sender.sendMessage(mm.deserialize("<red>未知攻击属性: $typeId"))
                return 0
            }
            targetEntity.atkType = type
            sender.sendMessage(mm.deserialize("<green>已将目标 <white>${targetEntity.name} <green>的攻击属性设置为 <${type.color.asHexString()}>${type.displayName}"))
        } else {
            val type = DefenseType.fromId(typeId)
            if (type == null) {
                sender.sendMessage(mm.deserialize("<red>未知防御属性: $typeId"))
                return 0
            }
            targetEntity.defType = type
            sender.sendMessage(mm.deserialize("<green>已将目标 <white>${targetEntity.name} <green>的防御属性设置为 <${type.color.asHexString()}>${type.displayName}"))
        }

        return Command.SINGLE_SUCCESS
    }



    private fun formatMessage(targetName: String, category: String, value: String, color: NamedTextColor): Component {
        return mm.deserialize(
            "<gray>目标 <white>$targetName <gray>的 $category: <${color.asHexString()}>$value"
        )
    }
}