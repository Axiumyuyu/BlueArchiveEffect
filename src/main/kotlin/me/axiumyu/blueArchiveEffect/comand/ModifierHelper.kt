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
                                .executes { checkEntityModifier(it, true) }
                        )
                        .then(
                            node("def")
                                .executes { checkEntityModifier(it, false) }
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
                                    argument("value", DoubleArgumentType.doubleArg())
                                        .executes { setEntityModifier(it, true) }
                                )
                        )
                        .then(
                            node("def")
                                .then(
                                    argument("value", DoubleArgumentType.doubleArg())
                                        .executes { setEntityModifier(it, false) }
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

        val modifier = target.modifier(isAtk)
        ctx.sendMsgToSender(target.name, isAtk, modifier)
        return Command.SINGLE_SUCCESS
    }

    private fun setEntityModifier(ctx: CommandContext<CommandSourceStack>, isAtk: Boolean): Int {
        val target = chooseSelectedEntity(ctx, "target") ?: return error(ctx, "未选择实体")
        val value = DoubleArgumentType.getDouble(ctx, "value")

        target.modifier(isAtk, value)
        ctx.sendMsgToSender(target.name, isAtk, value)
        return Command.SINGLE_SUCCESS
    }

    // --- 辅助方法 ---

    private fun CommandContext<CommandSourceStack>.sendMsgToSender(
        target: String,
        type: Boolean,
        value: Double,
    ) {
        source.sender.sendMessage(
            mm.deserialize("<gray>目标 <white>$target <gray>的 ${if (type) "<red>攻击" else "<blue>防御"}加成：$value")
        )
    }
}