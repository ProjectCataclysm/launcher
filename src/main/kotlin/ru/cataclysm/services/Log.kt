package ru.cataclysm.services

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import java.io.PrintStream
import java.util.*


/**
 * Created 28 . 2018 . / 18:03:14
 * @author Knoblul
 */
object Log {
    private var logger: Logger? = null
        get() {
            if (field == null) {
                field = LogManager.getLogger("ProjectCataclysm/Launcher")
            }

            return field
        }

    fun err(t: Throwable?, msg: String, vararg args: Any?) {
        logger!!.log(Level.ERROR, "! " + String.format(msg, *args), t)
    }

    fun err(msg: String, vararg args: Any?) {
        err(null, msg, *args)
    }

    fun msg(msg: String?, vararg args: Any?) {
        logger!!.log(Level.INFO, "* " + String.format(msg!!, *args))
    }

    fun logFutureErrors(ignored: Any?, cause: Throwable?) {
        if (cause != null) {
            err(cause, "future task error")
        }
    }

    fun configureLogging() {
        val out = System.out
        val err = System.err

        val configBuilder = ConfigurationBuilderFactory.newConfigurationBuilder()
        val pattern = configBuilder.newLayout("PatternLayout")
        pattern.addAttribute("pattern", "[%d{yyyy-MM-dd HH:mm:ss}] [%t/%level] [%c] %msg%n")

        val console = configBuilder.newAppender("stdout", "Console")
        console.add(pattern)
        configBuilder.add(console)

        val rollingFile = configBuilder.newAppender("file", "RollingFile")
        rollingFile.addAttribute("fileName", "logs/latest-launcher.log")
        rollingFile.addAttribute("filePattern", "logs/latest-launcher-%i.log")
        rollingFile.addComponent(
            configBuilder.newComponent("Policies")
                .addComponent(
                    configBuilder.newComponent("OnStartupTriggeringPolicy")
                        .addAttribute("minSize", 0)
                )
        )
        rollingFile.addComponent(
            configBuilder.newComponent("DefaultRolloverStrategy")
                .addAttribute("max", 6)
        )
        rollingFile.add(pattern)
        configBuilder.add(rollingFile)

        val rootLogger = configBuilder.newRootLogger(Level.INFO)
        rootLogger.add(configBuilder.newAppenderRef("stdout"))
        rootLogger.add(configBuilder.newAppenderRef("file"))
        configBuilder.add(rootLogger)

        Configurator.initialize(configBuilder.build())

        System.setOut(LoggingPrintStream(out, LogManager.getLogger("STDOUT"), Level.INFO))
        System.setErr(LoggingPrintStream(err, LogManager.getLogger("STDERR"), Level.ERROR))
    }

    private class LoggingPrintStream(original: PrintStream?, private val logger: Logger, private val level: Level) :
        PrintStream(original!!) {

        override fun println(o: Any?) {
            logger.log(level, Objects.toString(o))
        }

        override fun println(s: String?) {
            logger.log(level, s)
        }
    }
}