package me.axiumyu.blueArchiveEffect

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText
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
    inline fun <T> MutableList<T>.modifyOrInsert(
        predicate: (T) -> Boolean,
        newItemProvider: () -> T,
        modifier: (T) -> T
    ): T {
        // 查找满足条件的第一个索引
        val index = indexOfFirst(predicate)

        return if (index != -1) {
            // 如果找到了，在原地修改并写回
            val modifiedItem = modifier(this[index])
            this[index] = modifiedItem
            modifiedItem
        } else {
            // 如果没找到，创建新项并添加到列表末尾
            val newItem = modifier(newItemProvider())
            add(newItem)
            newItem
        }
    }

    @JvmStatic
    inline fun <T> MutableList<T>.modifyOrInsert(
        contains : String,
        newItemProvider: () -> T,
        modifier: (T) -> T
    ): T {
        // 查找满足条件的第一个索引
        val index = indexOfFirst{
            it.toString().contains(contains)
        }

        return if (index != -1) {
            // 如果找到了，在原地修改并写回
            val modifiedItem = modifier(this[index])
            this[index] = modifiedItem
            modifiedItem
        } else {
            // 如果没找到，创建新项并添加到列表末尾
            val newItem = modifier(newItemProvider())
            add(newItem)
            newItem
        }
    }

    @JvmStatic
    fun Component.toPlainText() = plainText().serialize(this)
}