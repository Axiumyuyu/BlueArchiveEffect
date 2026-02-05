package me.axiumyu.blueArchiveEffect.comand

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.plugin
import me.axiumyu.blueArchiveEffect.Util.chooseSelectedEntity
import me.axiumyu.blueArchiveEffect.Util.error
import me.axiumyu.blueArchiveEffect.Util.node
import me.axiumyu.blueArchiveEffect.Util.nullIf
import me.axiumyu.blueArchiveEffect.Util.suggestAll
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.Type
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.modifier
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import org.bukkit.Bukkit.getServer

/*
 * /bamodifier check/set <selector> <atk/def> <type> [value]
 */
object ModifierHelper {
    fun register() {
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            // 根节点
            val root = node("bamodifier")
                .requires { it.sender.hasPermission("bamodifier.use") }

            // === Check 分支 ===
            val checkNode = node("check")
                // check <selector> ...
                .then(
                    argument("target", ArgumentTypes.entity())
                        .then(
                            node("atk")
                                .then(
                                    argument("type", StringArgumentType.word())
                                        .suggests { _, b -> b.suggestAll(AttackType.entries.map { it.id.lowercase() }); b.buildFuture() }
                                        .executes { checkEntityModifier(it, true) }
                                )
                        )
                        .then(
                            node("def")
                                .then(
                                    argument("type", StringArgumentType.word())
                                        .suggests { _, b -> b.suggestAll(DefenseType.entries.map { it.id.lowercase() }); b.buildFuture() }
                                        .executes { checkEntityModifier(it, false) }
                                )
                        )
                )


            // === Set 分支 ===
            val setNode = node("set")
                .requires { it.sender.hasPermission("baeffect.admin") }
                // set entity <selector> <atk/def> <type>
                .then(
                    argument("target", ArgumentTypes.entity())
                        .then(
                            node("atk")
                                .then(
                                    argument("type", StringArgumentType.word())
                                        .suggests { _, b -> b.suggestAll(AttackType.entries.map { it.id.lowercase() }); b.buildFuture() }
                                        .then(
                                            argument("value", DoubleArgumentType.doubleArg())
                                                .executes { setEntityModifier(it, true) }
                                        )

                                )
                        )
                        .then(
                            node("def")
                                .then(
                                    argument("type", StringArgumentType.word())
                                        .suggests { _, b -> b.suggestAll(DefenseType.entries.map { it.id.lowercase() }); b.buildFuture() }
                                        .then(
                                            argument("value", DoubleArgumentType.doubleArg())
                                                .executes { setEntityModifier(it, false) }
                                        )

                                )
                        )
                )


            // 注册
            root.then(checkNode)
            root.then(setNode)
            commands.register(root.build(), "BlueArchive RPG 属性管理")
        }
    }

    // ================= 实体逻辑 (Entity) =================

    private fun checkEntityModifier(ctx: CommandContext<CommandSourceStack>, isAtk: Boolean): Int {
        val target = chooseSelectedEntity(ctx, "target") ?: return error(ctx, "未选择实体")
        val typeId = StringArgumentType.getString(ctx, "type")

        if (isAtk) {
            // 使用扩展属性 .atkType
            val type =
                AttackType.fromId(typeId).nullIf(AttackType.NORMAL_A) ?: return error(ctx, "未知或不允许的攻击类型: $typeId")
            val modifier = target.modifier(type)
            ctx.sendMsgToSender(target.name, type, modifier)
        } else {
            // 使用扩展属性 .defType
            val type =
                DefenseType.fromId(typeId).nullIf(DefenseType.NORMAL_D) ?: return error(ctx, "未知或不允许的防御类型: $typeId")
            val modifier = target.modifier(type)
            ctx.sendMsgToSender(target.name, type, modifier)
        }
        return Command.SINGLE_SUCCESS
    }

    private fun setEntityModifier(ctx: CommandContext<CommandSourceStack>, isAtk: Boolean): Int {
        val target = chooseSelectedEntity(ctx, "target") ?: return error(ctx, "未选择实体")
        val typeId = StringArgumentType.getString(ctx, "type")
        val value = DoubleArgumentType.getDouble(ctx, "value")

        if (isAtk) {
            val type =
                AttackType.fromId(typeId).nullIf(AttackType.NORMAL_A) ?: return error(ctx, "未知或不允许的攻击类型: $typeId")
            // 直接赋值给扩展属性
            target.modifier(type, value)
            ctx.sendMsgToSender(target.name, type, value)
        } else {
            val type =
                DefenseType.fromId(typeId).nullIf(DefenseType.NORMAL_D) ?: return error(ctx, "未知或不允许的防御类型: $typeId")
            // 直接赋值给扩展属性
            target.modifier(type, value)
            ctx.sendMsgToSender(target.name, type, value)
        }
        return Command.SINGLE_SUCCESS
    }

    // --- 辅助方法 ---

    private fun CommandContext<CommandSourceStack>.sendMsgToSender(
        target: String,
        type: Type,
        value: Double,
    ) {
        source.sender.sendMessage(
            mm.deserialize("<gray>目标 <white>$target <gray>的 <${type.color.asHexString()}>${type.displayName}属性加成：$value")
        )
    }
}