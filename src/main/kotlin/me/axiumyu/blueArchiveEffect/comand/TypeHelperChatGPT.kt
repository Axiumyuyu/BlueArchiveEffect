package me.axiumyu.blueArchiveEffect.comand

import com.mojang.brigadier.Command
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.SuggestionProvider
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver
import me.axiumyu.blueArchiveEffect.Util.node
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkModifier
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defModifier
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import me.axiumyu.blueArchiveEffect.config.Config
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.LivingEntity

@Deprecated("use TypeHelperNew")
object TypeHelperChatGPT {

    private val ERR_NOT_LIVING = SimpleCommandExceptionType(LiteralMessage("目标必须是 LivingEntity"))
    private val ERR_NOT_SINGLE = SimpleCommandExceptionType(LiteralMessage("selector 必须且只能选择一个实体"))
    private val ERR_BAD_TYPE = SimpleCommandExceptionType(LiteralMessage("未知的属性类型 id"))

    private val SUGGEST_ATTACK: SuggestionProvider<CommandSourceStack> = SuggestionProvider { _, builder ->
        AttackType.entries.forEach { t ->
            builder.suggest(t.id, LiteralMessage(t.displayName))
        }
        builder.buildFuture()
    }

    private val SUGGEST_DEFENSE: SuggestionProvider<CommandSourceStack> = SuggestionProvider { _, builder ->
        DefenseType.entries.forEach { t ->
            builder.suggest(t.id, LiteralMessage(t.displayName))
        }
        builder.buildFuture()
    }

    fun register(commands: Commands) {
        val root = node("batype")
            .then(buildCheck())
            .then(buildSet())
            .then(buildReload())
            .build()

        // description + aliases 可选，这里给个简短描述
        commands.register(root, "BlueArchive 属性查看/设置命令", listOf())
    }

    private fun buildCheck() =
        node("check")
            .requires { it.sender.hasPermission("baeffect.use") }
            .then(
                argument("target", ArgumentTypes.entity())
                    .then(
                        node("atk")
                            .executes { ctx -> execCheckAtk(ctx, null) }
                            .then(
                                argument("type", StringArgumentType.word())
                                    .suggests(SUGGEST_ATTACK)
                                    .executes { ctx ->
                                        val id = StringArgumentType.getString(ctx, "type")
                                        val type = AttackType.fromId(id) ?: throw ERR_BAD_TYPE.create()
                                        execCheckAtk(ctx, type)
                                    }
                            )
                    )
                    .then(
                        node("def")
                            .executes { ctx -> execCheckDef(ctx, null) }
                            .then(
                                argument("type", StringArgumentType.word())
                                    .suggests(SUGGEST_DEFENSE)
                                    .executes { ctx ->
                                        val id = StringArgumentType.getString(ctx, "type")
                                        val type = DefenseType.fromId(id) ?: throw ERR_BAD_TYPE.create()
                                        execCheckDef(ctx, type)
                                    }
                            )
                    )
            )

    private fun buildSet() =
        node("set")
            .requires { it.sender.hasPermission("baeffect.admin") }
            .then(
                argument("target", ArgumentTypes.entity())
                    .then(
                        node("atk")
                            .then(
                                argument("type", StringArgumentType.word())
                                    .suggests(SUGGEST_ATTACK)
                                    .executes { ctx ->
                                        val target = resolveLiving(ctx)
                                        val type = AttackType.fromId(StringArgumentType.getString(ctx, "type"))
                                            ?: throw ERR_BAD_TYPE.create()
                                        target.atkType = type
                                        reply(ctx.source, okSetMsg(target, "atk", type.displayName, type.color))
                                        Command.SINGLE_SUCCESS
                                    }
                                    .then(
                                        node("modifier")
                                            .then(
                                                argument("value", DoubleArgumentType.doubleArg())
                                                    .executes { ctx ->
                                                        val target = resolveLiving(ctx)
                                                        val type = AttackType.fromId(StringArgumentType.getString(ctx, "type"))
                                                            ?: throw ERR_BAD_TYPE.create()
                                                        val value = DoubleArgumentType.getDouble(ctx, "value")
                                                        target.atkType = type
                                                        target.atkModifier(type, value)
                                                        reply(
                                                            ctx.source,
                                                            okSetMsg(target, "atk", "${type.displayName} + modifier=$value", type.color)
                                                        )
                                                        Command.SINGLE_SUCCESS
                                                    }
                                            )
                                    )
                            )
                    )
                    .then(
                        node("def")
                            .then(
                                argument("type", StringArgumentType.word())
                                    .suggests(SUGGEST_DEFENSE)
                                    .executes { ctx ->
                                        val target = resolveLiving(ctx)
                                        val type = DefenseType.fromId(StringArgumentType.getString(ctx, "type"))
                                            ?: throw ERR_BAD_TYPE.create()
                                        target.defType = type
                                        reply(ctx.source, okSetMsg(target, "def", type.displayName, type.color))
                                        Command.SINGLE_SUCCESS
                                    }
                                    .then(
                                        node("modifier")
                                            .then(
                                                argument("value", DoubleArgumentType.doubleArg())
                                                    .executes { ctx ->
                                                        val target = resolveLiving(ctx)
                                                        val type = DefenseType.fromId(StringArgumentType.getString(ctx, "type"))
                                                            ?: throw ERR_BAD_TYPE.create()
                                                        val value = DoubleArgumentType.getDouble(ctx, "value")
                                                        target.defType = type
                                                        target.defModifier(type, value)
                                                        reply(
                                                            ctx.source,
                                                            okSetMsg(target, "def", "${type.displayName} + modifier=$value", type.color)
                                                        )
                                                        Command.SINGLE_SUCCESS
                                                    }
                                            )
                                    )
                            )
                    )
            )

    private fun buildReload() =
        node("reload")
            .requires { it.sender.hasPermission("baeffect.admin") }
            .executes { ctx ->
                Config.loadConfig()
                reply(ctx.source, Component.text("已重载 BAType 配置文件", NamedTextColor.GREEN))
                Command.SINGLE_SUCCESS
            }

    private fun execCheckAtk(ctx: CommandContext<CommandSourceStack>, queryType: AttackType?): Int {
        val target = resolveLiving(ctx)
        val cur = target.atkType
        val showType = queryType ?: cur
        val mod = target.atkModifier(showType)

        reply(
            ctx.source,
            Component.text()
                .append(Component.text("目标: ", NamedTextColor.GRAY))
                .append(Component.text(target.name, NamedTextColor.WHITE))
                .append(Component.text(" | atk: ", NamedTextColor.GRAY))
                .append(Component.text(cur.displayName, cur.color))
                .append(Component.text(" (", NamedTextColor.DARK_GRAY))
                .append(Component.text(cur.id, NamedTextColor.DARK_GRAY))
                .append(Component.text(")", NamedTextColor.DARK_GRAY))
                .append(Component.text(" | modifier[", NamedTextColor.GRAY))
                .append(Component.text(showType.id, NamedTextColor.WHITE))
                .append(Component.text("]=", NamedTextColor.GRAY))
                .append(Component.text(mod.toString(), NamedTextColor.AQUA))
                .build()
        )
        return Command.SINGLE_SUCCESS
    }

    private fun execCheckDef(ctx: CommandContext<CommandSourceStack>, queryType: DefenseType?): Int {
        val target = resolveLiving(ctx)
        val cur = target.defType
        val showType = queryType ?: cur
        val mod = target.defModifier(showType)

        reply(
            ctx.source,
            Component.text()
                .append(Component.text("目标: ", NamedTextColor.GRAY))
                .append(Component.text(target.name, NamedTextColor.WHITE))
                .append(Component.text(" | def: ", NamedTextColor.GRAY))
                .append(Component.text(cur.displayName, cur.color))
                .append(Component.text(" (", NamedTextColor.DARK_GRAY))
                .append(Component.text(cur.id, NamedTextColor.DARK_GRAY))
                .append(Component.text(")", NamedTextColor.DARK_GRAY))
                .append(Component.text(" | modifier[", NamedTextColor.GRAY))
                .append(Component.text(showType.id, NamedTextColor.WHITE))
                .append(Component.text("]=", NamedTextColor.GRAY))
                .append(Component.text(mod.toString(), NamedTextColor.AQUA))
                .build()
        )
        return Command.SINGLE_SUCCESS
    }

    private fun resolveLiving(ctx: CommandContext<CommandSourceStack>): LivingEntity {
        val resolver = ctx.getArgument("target", EntitySelectorArgumentResolver::class.java)
        val list = resolver.resolve(ctx.source)
        val entity = list.singleOrNull() ?: throw ERR_NOT_SINGLE.create()
        return entity as? LivingEntity ?: throw ERR_NOT_LIVING.create()
    }

    private fun reply(source: CommandSourceStack, msg: Component) {
        source.sender.sendMessage(msg)
    }

    private fun okSetMsg(target: LivingEntity, which: String, value: String, color: NamedTextColor): Component =
        Component.text()
            .append(Component.text("已设置 ", NamedTextColor.GREEN))
            .append(Component.text(target.name, NamedTextColor.WHITE))
            .append(Component.text(" 的 $which 为 ", NamedTextColor.GREEN))
            .append(Component.text(value, color))
            .build()
}
