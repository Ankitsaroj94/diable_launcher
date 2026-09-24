package `in`.ankitsaroj.diable.util

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

private val MATH_CHARS = Regex("^[0-9a-zA-Z+\\-*/^%().,!\\s]+$")
private val WORDS = Regex("[a-zA-Z]+")
private val IMPLIED_PRODUCT = Regex("[0-9)][a-z]")

private val CONSTANTS = mapOf("pi" to Math.PI, "e" to Math.E, "true" to 1.0, "false" to 0.0)

private val FUNCTIONS = setOf(
    "abs", "log", "ln", "log10", "sqrt", "fact", "round", "floor", "ceil", "ceiling",
    "sin", "cos", "tan", "asin", "acos", "atan", "max", "min", "random",
)

/**
 * Whether [query] should be treated as a calculation rather than an app search. Words are
 * only allowed when they are known constants or functions, so "maps" never reads as math,
 * and a lone constant ("e", "pi") still searches apps.
 */
fun looksLikeMath(query: String): Boolean {
    val t = query.trim()
    if (t.isEmpty() || !MATH_CHARS.matches(t)) return false
    val words = WORDS.findAll(t.lowercase()).map { it.value }.toList()
    if (words.any { it !in CONSTANTS && it !in FUNCTIONS }) return false
    val hasOperand = t.any { it.isDigit() } || words.isNotEmpty()
    // "2pi" multiplies without an operator sign.
    val hasOperation = t.any { it in "+-*/^%!(" } || IMPLIED_PRODUCT.containsMatchIn(t.lowercase())
    return hasOperand && hasOperation
}

/** Evaluates [expression], or null when it doesn't parse or has no finite result. */
fun evaluateMath(expression: String): Double? {
    val expr = expression.replace(" ", "").lowercase()
    if (expr.isEmpty() || !MATH_CHARS.matches(expr)) return null
    return try {
        val parser = MathParser(expr)
        val value = parser.parse()
        value.takeIf { parser.atEnd() && it.isFinite() }
    } catch (_: Exception) {
        null
    }
}

/**
 * Recursive-descent evaluator covering what Diable's calculator documents: + - * / ^ %,
 * postfix ! and %, implied multiplication (2(3), 2pi), constants and common functions.
 */
private class MathParser(private val input: String) {
    private var pos = 0

    fun parse(): Double = parseExpression()

    fun atEnd() = pos >= input.length

    private fun peek(): Char? = input.getOrNull(pos)

    private fun parseExpression(): Double {
        var result = parseTerm()
        while (true) {
            when (peek()) {
                '+' -> { pos++; result += parseTerm() }
                '-' -> { pos++; result -= parseTerm() }
                else -> return result
            }
        }
    }

    private fun parseTerm(): Double {
        var result = parseUnary()
        while (true) {
            val c = peek() ?: return result
            when {
                c == '*' -> { pos++; result *= parseUnary() }
                c == '/' -> {
                    pos++
                    val div = parseUnary()
                    if (div == 0.0) throw ArithmeticException("div0")
                    result /= div
                }
                c == '%' -> { pos++; result %= parseUnary() }
                // Implied multiplication: 2(3), 2pi, (1+1)(2).
                c == '(' || c.isLetter() || c.isDigit() || c == '.' -> result *= parseUnary()
                else -> return result
            }
        }
    }

    private fun parseUnary(): Double = when (peek()) {
        '-' -> { pos++; -parseUnary() }
        '+' -> { pos++; parseUnary() }
        else -> parsePower()
    }

    private fun parsePower(): Double {
        val base = parsePostfix()
        if (peek() == '^') {
            pos++
            // Right-associative: 2^3^2 = 2^9.
            return base.pow(parseUnary())
        }
        return base
    }

    private fun parsePostfix(): Double {
        var value = parsePrimary()
        while (true) {
            when (peek()) {
                '!' -> { pos++; value = factorial(value) }
                // A trailing % is a percentage ("50%"); between operands it is modulo.
                '%' -> {
                    val next = input.getOrNull(pos + 1)
                    if (next == null || next in "+-*/^)," ) {
                        pos++
                        value /= 100.0
                    } else {
                        return value
                    }
                }
                else -> return value
            }
        }
    }

    private fun parsePrimary(): Double {
        val c = peek() ?: throw IllegalStateException("unexpected end")
        if (c == '(') {
            pos++
            val result = parseExpression()
            if (peek() == ')') pos++
            return result
        }
        if (c.isLetter()) return parseIdentifier()
        return parseNumber()
    }

    private fun parseIdentifier(): Double {
        val start = pos
        while (peek()?.isLetter() == true) pos++
        var name = input.substring(start, pos)
        if (name == "log" && input.startsWith("10(", pos)) {
            pos += 2
            name = "log10"
        }
        CONSTANTS[name]?.let { return it }
        if (name !in FUNCTIONS) throw IllegalArgumentException("unknown $name")
        val args = parseArguments()
        fun arg() = args.singleOrNull() ?: throw IllegalArgumentException("$name takes 1 argument")
        return when (name) {
            "abs" -> abs(arg())
            "log", "ln" -> ln(arg())
            "log10" -> log10(arg())
            "sqrt" -> sqrt(arg())
            "fact" -> factorial(arg())
            "round" -> round(arg())
            "floor" -> floor(arg())
            "ceil", "ceiling" -> ceil(arg())
            "sin" -> sin(arg())
            "cos" -> cos(arg())
            "tan" -> tan(arg())
            "asin" -> asin(arg())
            "acos" -> acos(arg())
            "atan" -> atan(arg())
            "max" -> args.maxOrNull() ?: throw IllegalArgumentException("max()")
            "min" -> args.minOrNull() ?: throw IllegalArgumentException("min()")
            "random" -> Math.random()
            else -> throw IllegalArgumentException(name)
        }
    }

    private fun parseArguments(): List<Double> {
        if (peek() != '(') throw IllegalArgumentException("expected (")
        pos++
        val args = mutableListOf<Double>()
        if (peek() == ')') {
            pos++
            return args
        }
        while (true) {
            args += parseExpression()
            when (peek()) {
                ',' -> pos++
                ')' -> { pos++; return args }
                // Tolerate a missing closing paren while the user is still typing.
                null -> return args
                else -> throw IllegalArgumentException("expected , or )")
            }
        }
    }

    private fun parseNumber(): Double {
        val start = pos
        while (peek()?.let { it.isDigit() || it == '.' } == true) pos++
        if (start == pos) throw NumberFormatException("expected number")
        return input.substring(start, pos).toDouble()
    }

    private fun factorial(n: Double): Double {
        if (n < 0 || n != floor(n) || n > 170) throw ArithmeticException("fact")
        var result = 1.0
        for (i in 2..n.toInt()) result *= i
        return result
    }
}
