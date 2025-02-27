package ru.cataclysm.services

import org.tinylog.Level
import org.tinylog.kotlin.Logger
import org.tinylog.kotlin.TaggedLogger
import java.io.PrintStream
import java.util.*
import java.util.function.Consumer
import kotlin.math.log


/**
 * Created 28 . 2018 . / 18:03:14
 * @author Knoblul
 */
object Log {
    private var logger: TaggedLogger? = null
        get() {
            if (field == null) {
                field = Logger.tag("ProjectCataclysm/Launcher")
            }

            return field
        }

    fun err(t: Throwable?, msg: String, vararg args: Any?) {
        if (t != null) {
            logger!!.error(t, msg, args)
        } else {
            logger!!.error(msg, args)
        }
    }

    fun err(msg: String, vararg args: Any?) {
        err(null, msg, *args)
    }

    fun msg(msg: String?, vararg args: Any?) {
        logger!!.info(msg.toString(), args)
    }

    fun configureLogging() {
        val out = System.out
        val err = System.err

        System.setOut(LoggingPrintStream(out, org.tinylog.Logger.tag("STDOUT")::info))
        System.setErr(LoggingPrintStream(err, org.tinylog.Logger.tag("STDERR")::error))
    }

    private class LoggingPrintStream(original: PrintStream?, private val logger: Consumer<Any?>) :
        PrintStream(original!!) {

        override fun println(o: Any?) {
            logger.accept(o)
        }

        override fun println(s: String?) {
            logger.accept(s as Any?)
        }
    }
}