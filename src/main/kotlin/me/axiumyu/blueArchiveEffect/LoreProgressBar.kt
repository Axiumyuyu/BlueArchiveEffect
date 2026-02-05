package me.axiumyu.blueArchiveEffect

import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import me.axiumyu.blueArchiveEffect.Util.toPlainText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.inventory.ItemStack
import java.awt.Color.HSBtoRGB
import kotlin.math.roundToInt

object SmartLoreBar {

    /**
     * 智能更新 Lore 中的数值和进度条
     *
     * @param prefix 数值行的识别前缀（纯文本，不带颜色），例如 "充能条："
     * @param current 新的当前数值
     * @param max 最大数值
     * @param totalBars 进度条长度（默认为20）
     * @param filledChar 填充字符（默认为 ■）
     * @param emptyChar 空字符（默认为 □）
     * @param filledColor 填充颜色 MiniMessage Tag（默认为 <green>）
     * @param emptyColor 空颜色 MiniMessage Tag（默认为 <gray>）
     */
    fun ItemStack.updateSmartLore(
        prefix: String,
        current: Int,
        max: Int,
        totalBars: Int = 20,
        filledChar: Char = '■',
        emptyChar: Char = '□',
        filledColor: String = "<green>",
        emptyColor: String = "<gray>"
    ): ItemStack {
        val meta = this.itemMeta ?: return this
        val originalLore = meta.lore() ?: return this // 如果没有 Lore，则无法更新，直接返回

        // 使用 map 生成一个新的 List，遍历每一行进行特征匹配和替换
        val newLore = originalLore.map { line ->
            // 1. 转为纯文本，去除颜色代码，用于逻辑判断
            val plainText = line.toPlainText()

            when {
                // 特征 A: 这一行以指定的 prefix 开头 (例如 "充能条：")
                plainText.startsWith(prefix) -> {
                    // 重新生成数值行
                    mm.deserialize("<!i><gray>$prefix<white>${current.coerceIn(0,max)}/$max")
                }

                // 特征 B: 这一行看起来像进度条 (只包含方块字符和空格)
                isBarLine(plainText, filledChar, emptyChar) -> {
                    // 重新生成进度条行
                    generateBarComponent(current, max, totalBars, filledChar, emptyChar, filledColor, emptyColor)
                }

                // 其他行: 保持原样
                else -> line
            }
        }

        meta.lore(newLore)
        this.itemMeta = meta
        return this
    }

    /**
     * 判断一行纯文本是否是进度条
     * 逻辑：去除空格后，字符串不为空，且只包含 filledChar 或 emptyChar
     */
    private fun isBarLine(plainText: String, filledChar: Char, emptyChar: Char): Boolean {
        val trimmed = plainText.replace(" ", "") // 忽略空格干扰
        if (trimmed.isEmpty()) return false

        // 检查是否所有字符都在允许的字符集中
        return trimmed.all { it == filledChar || it == emptyChar }
    }

    /**
     * 生成进度条 Component (复用之前的逻辑)
     */
    private fun generateBarComponent(
        current: Int,
        max: Int,
        totalBars: Int,
        filledChar: Char,
        emptyChar: Char,
        filledColor: String,
        emptyColor: String
    ): Component {
        val safeMax = if (max <= 0) 1 else max
        val percentage = (current.toDouble() / safeMax).coerceIn(0.0, 1.0)

        // 满值彩虹特效
        if (percentage >= 1.0) {
            val fullBar = filledChar.toString().repeat(totalBars)
            return mm.deserialize("<!i><rainbow>$fullBar</rainbow>")
        }

        val filledCount = (percentage * totalBars).roundToInt()
        val emptyCount = totalBars - filledCount

        val sb = StringBuilder()
        sb.append("<!i>")
        sb.append(filledColor).append(filledChar.toString().repeat(filledCount))
        sb.append(emptyColor).append(emptyChar.toString().repeat(emptyCount))

        return mm.deserialize(sb.toString())
    }
}