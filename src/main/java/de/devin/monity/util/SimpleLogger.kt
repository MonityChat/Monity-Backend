package de.devin.monity.util

import de.devin.monity.name
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gets the current time and formats it into this format dd.MM.yyyy HH:mm:ss
 * @see SimpleDateFormat
 */
fun getCurrentTime(): String {
    val currentTime = System.currentTimeMillis()
    val date = Date(currentTime)
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
    return formatter.format(date)
}

private fun printBase(): String {
    return "[${getCurrentTime()}] ${ConsoleColors.BLACK_BRIGHT}[${ConsoleColors.PURPLE}$name-Server${ConsoleColors.BLACK_BRIGHT}]${ConsoleColors.RESET}"
}

/**
 * Will log an info message
 */
fun logInfo(message: Any) = printlnAndReset(printBase() + "${ConsoleColors.BLACK_BRIGHT}[${ConsoleColors.GREEN}INFO${ConsoleColors.BLACK_BRIGHT}]${ConsoleColors.RESET} $message")


/**
 * Will log an error message
 */
fun logError(message: Any) = printlnAndReset(printBase() + "${ConsoleColors.BLACK_BRIGHT}[${ConsoleColors.RED}ERROR${ConsoleColors.BLACK_BRIGHT}]${ConsoleColors.RESET} $message")


/**
 * Will log a warn message
 */
fun logWarning(message: Any) = printlnAndReset(printBase() + "${ConsoleColors.BLACK_BRIGHT}[${ConsoleColors.YELLOW}WARNING${ConsoleColors.BLACK_BRIGHT}]${ConsoleColors.RESET} $message")


/**
 * Will log a debug message
 */
fun logDebug(message: Any) = printlnAndReset(printBase() + "${ConsoleColors.BLACK_BRIGHT}[${ConsoleColors.YELLOW}DEBUG${ConsoleColors.BLACK_BRIGHT}]${ConsoleColors.RESET} $message")

/**
 * prints the given message and resets the consolecolors afterwards
 * @see ConsoleColors
 * @param message message to print
 */
fun printlnAndReset(message: Any) = println("$message${ConsoleColors.RESET}")
