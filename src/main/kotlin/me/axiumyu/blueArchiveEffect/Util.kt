package me.axiumyu.blueArchiveEffect

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity

object Util {

    @JvmStatic
    fun chooseSelectedEntity(ctx: CommandContext<CommandSourceStack>, argumentName: String): Entity? =
        ctx.getArgument(argumentName, EntitySelectorArgumentResolver::class.java)
            .resolve(ctx.source).first()

    @JvmStatic
    fun <T> SuggestionsBuilder.suggestAll(list : Collection<T>) {
        list.forEach {
            suggest(it.toString())
        }
    }

    @JvmStatic
    fun node(name: String) = Commands.literal(name)

    @JvmStatic
    fun error(ctx: CommandContext<CommandSourceStack>, msg: String): Int {
        ctx.source.sender.sendMessage(mm.deserialize("<red>$msg"))
        return 0
    }

    @JvmStatic
    fun Component.toPlainText() = plainText().serialize(this)

    @JvmStatic
    fun <T> T?.nullIf(except : T): T? {
        return if (this == except) null else this
    }
}
