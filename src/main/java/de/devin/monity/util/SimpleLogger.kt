package filemanagment.util

import name
import java.text.SimpleDateFormat
import java.util.*

fun getCurrentTime(): String {
    val currentTime = System.currentTimeMillis()
    val date = Date(currentTime)
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
    return formatter.format(date)
}

private fun printBase(): String {
    return "[${getCurrentTime()}] ${ConsoleColors.BLACK_BRIGHT}[${ConsoleColors.PURPLE}$name-Server${ConsoleColors.BLACK_BRIGHT}]${ConsoleColors.RESET}"
}

fun logInfo(message: Any) = printlnAndReset(printBase() + "${ConsoleColors.BLACK_BRIGHT}[${ConsoleColors.GREEN}INFO${ConsoleColors.BLACK_BRIGHT}]${ConsoleColors.RESET} $message")
fun logError(message: Any) = printlnAndReset(printBase() + "${ConsoleColors.BLACK_BRIGHT}[${ConsoleColors.RED}ERROR${ConsoleColors.BLACK_BRIGHT}]${ConsoleColors.RESET} $message")
fun logWarning(message: Any) = printlnAndReset(printBase() + "${ConsoleColors.BLACK_BRIGHT}[${ConsoleColors.YELLOW}WARNING${ConsoleColors.BLACK_BRIGHT}]${ConsoleColors.RESET} $message")
fun printlnAndReset(message: Any) = println("$message${ConsoleColors.RESET}")
