package me.axiumyu.blueArchiveEffect

import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import me.axiumyu.blueArchiveEffect.Util.toPlainText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.inventory.ItemStack

/**
 * 一个用于在 Item Lore 中维护「数值 + 可视化条」的轻量工具类
 * - 使用 MiniMessage 进行格式化
 * - 只修改指定行，不破坏其他 Lore
 * - 满值时自动切换为彩虹条
 */
class LoreProgressBar(
    private val max: Int,
    private val barLength: Int = 20,
    private val filledColor: TextColor = NamedTextColor.GREEN,
    private val emptyColor: TextColor = NamedTextColor.GRAY,
    private val barChar: Char = '|',
    private val numberLinePrefix: String
) {

    /**
     * 更新物品 Lore 中的数值与进度条
     * @param item 原物品
     * @param value 当前数值
     * @return 一个新的 ItemStack（不原地修改）
     */
    fun updateLore(item: ItemStack, value: Int): ItemStack {
        val meta = item.itemMeta ?: return item
        val lore = meta.lore() ?: return item

        val clamped = value.coerceIn(0, max)

        val newLore = lore.map { line ->
            when {
                isNumberLine(line) -> buildNumberLine(clamped)
                isBarLine(line) -> buildBarLine(clamped)
                else -> line
            }
        }

        meta.lore(newLore)
        item.itemMeta = meta
        return item
    }

    /** 判断是否是“充能条：x/y”这一行 */
    private fun isNumberLine(component: Component): Boolean {
        return component.toPlainText().startsWith(numberLinePrefix)
    }

    /** 判断是否是由 barChar 组成的进度条 */
    private fun isBarLine(component: Component): Boolean {
        val text = component.toPlainText()
        return text.isNotEmpty() && text.all { it == barChar }
    }

    /** 构建数值行 */
    private fun buildNumberLine(value: Int): Component {
        return mm.deserialize("<!i>${numberLinePrefix}${value}/${max}")
    }

    /** 构建进度条 */
    private fun buildBarLine(value: Int): Component {
        if (value >= max) {
            // 满值：彩虹条
            return Component.text()
                .decoration(TextDecoration.ITALIC, false)
                .append(rainbowBar())
                .build()
        }

        val filled = (value.toDouble() / max * barLength).toInt()

        val builder = Component.text().decoration(TextDecoration.ITALIC, false)

        repeat(barLength) { index ->
            val color = if (index < filled) filledColor else emptyColor
            builder.append(
                Component.text(barChar.toString()).color(color)
            )
        }

        return builder.build()
    }

    /** 生成彩虹进度条 */
    private fun rainbowBar(): Component {
        val builder = Component.text()
        for (i in 0 until barLength) {
            val hue = i.toFloat() / barLength
            builder.append(
                Component.text(barChar.toString())
                    .color(TextColor.color(java.awt.Color.HSBtoRGB(hue, 1f, 1f)))
            )
        }
        return builder.build()
    }
}
