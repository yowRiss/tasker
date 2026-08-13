package com.tasker.android.ui.notes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

object NoteMathParser {

    fun formatMathAndMarkdown(text: String): AnnotatedString {
        if (text.isBlank()) return AnnotatedString("(Empty note)")

        return buildAnnotatedString {
            val lines = text.split("\n")
            lines.forEachIndexed { index, rawLine ->
                val line = rawLine.trimEnd()
                when {
                    line.startsWith("# ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
                            append(parseInlineMathAndStyles(line.substring(2)))
                        }
                    }
                    line.startsWith("## ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp)) {
                            append(parseInlineMathAndStyles(line.substring(3)))
                        }
                    }
                    line.startsWith("### ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
                            append(parseInlineMathAndStyles(line.substring(4)))
                        }
                    }
                    line.trimStart().startsWith("- ") -> {
                        val indentLevel = (line.indexOf("-") / 2).coerceAtLeast(0)
                        val bullet = "  ".repeat(indentLevel) + "• "
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(bullet)
                        }
                        append(parseInlineMathAndStyles(line.substring(line.indexOf("-") + 1).trimStart()))
                    }
                    line.startsWith("$$") && line.endsWith("$$") && line.length > 4 -> {
                        val mathContent = line.substring(2, line.length - 2)
                        withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 17.sp)) {
                            append("  ∑  ${cleanTexSymbols(mathContent)}")
                        }
                    }
                    else -> {
                        append(parseInlineMathAndStyles(line))
                    }
                }
                if (index < lines.size - 1) append("\n")
            }
        }
    }

    private fun parseInlineMathAndStyles(input: String): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < input.length) {
                if (input.startsWith("$$", i)) {
                    val end = input.indexOf("$$", i + 2)
                    if (end != -1) {
                        val math = input.substring(i + 2, end)
                        withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)) {
                            append("[ ${cleanTexSymbols(math)} ]")
                        }
                        i = end + 2
                        continue
                    }
                } else if (input[i] == '$') {
                    val end = input.indexOf('$', i + 1)
                    if (end != -1) {
                        val math = input.substring(i + 1, end)
                        withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)) {
                            append(cleanTexSymbols(math))
                        }
                        i = end + 1
                        continue
                    }
                }
                append(input[i])
                i++
            }
        }
    }

    private fun cleanTexSymbols(tex: String): String {
        return tex
            .replace("\\sum", "∑")
            .replace("\\int", "∫")
            .replace("\\prod", "∏")
            .replace("\\infty", "∞")
            .replace("\\pm", "±")
            .replace("\\mp", "∓")
            .replace("\\times", "×")
            .replace("\\div", "÷")
            .replace("\\neq", "≠")
            .replace("\\le", "≤")
            .replace("\\ge", "≥")
            .replace("\\alpha", "α")
            .replace("\\beta", "β")
            .replace("\\gamma", "γ")
            .replace("\\delta", "δ")
            .replace("\\pi", "π")
            .replace("\\theta", "θ")
            .replace("\\sqrt", "√")
            .replace(Regex("\\\\frac\\{([^}]+)\\}\\s*\\{([^}]+)\\}"), "($1 / $2)")
            .replace(Regex("\\^{([^}]+)\\}"), "^$1")
            .replace(Regex("_\\{([^}]+)\\}"), "_$1")
    }
}
