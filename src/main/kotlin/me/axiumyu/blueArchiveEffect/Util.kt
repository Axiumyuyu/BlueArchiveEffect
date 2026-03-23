package me.axiumyu.blueArchiveEffect

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText
import org.bukkit.entity.Entity

object Util {

    fun chooseSelectedEntity(ctx: CommandContext<CommandSourceStack>, argumentName: String): Entity? =
        ctx.getArgument(argumentName, EntitySelectorArgumentResolver::class.java)
            .resolve(ctx.source).first()

    fun <T> SuggestionsBuilder.suggestAll(list : Collection<T>) {
        list.forEach {
            suggest(it.toString())
        }
    }

    fun node(name: String) = Commands.literal(name)

    fun error(ctx: CommandContext<CommandSourceStack>, msg: String): Int {
        ctx.source.sender.sendMessage(mm.deserialize("<red>$msg"))
        return 0
    }

    fun Component.toPlainText() = plainText().serialize(this)

    fun <T> T?.nullIf(except : T): T? {
        return if (this == except) null else this
    }

    fun <T> List<T>.drop(item: T): List<T>{
        return this.filter { it != item }
    }
}
