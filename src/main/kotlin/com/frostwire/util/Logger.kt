/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2022, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.frostwire.util

import java.util.logging.Level

/**
 * @author gubatron
 * @author aldenml
 */
class Logger private constructor(private val jul: java.util.logging.Logger) {
    val name: String = jul.name

    @JvmOverloads
    fun info(msg: String?, showCallingMethodInfo: Boolean = false) {
        if (contextPrefix == null) {
            jul.logp(Level.INFO, name, "", (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg))
        } else {
            jul.logp(
                Level.INFO,
                name,
                "",
                contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg)
            )
        }
    }

    @JvmOverloads
    fun info(msg: String?, e: Throwable?, showCallingMethodInfo: Boolean = false) {
        jul.logp(Level.INFO, name, "", (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg), e)
    }

    @JvmOverloads
    fun warn(msg: String?, showCallingMethodInfo: Boolean = false) {
        if (contextPrefix == null) {
            jul.logp(Level.WARNING, name, "", (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg))
        } else {
            jul.logp(
                Level.WARNING,
                name,
                "",
                contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg)
            )
        }
    }

    @JvmOverloads
    fun warn(msg: String?, e: Throwable?, showCallingMethodInfo: Boolean = false) {
        if (contextPrefix == null) {
            jul.logp(Level.INFO, name, "", (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg), e)
        } else {
            jul.logp(
                Level.INFO,
                name,
                "",
                contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg),
                e
            )
        }
    }

    @JvmOverloads
    fun error(msg: String?, showCallingMethodInfo: Boolean = false) {
        if (contextPrefix == null) {
            jul.logp(Level.SEVERE, name, "", (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg))
        } else {
            jul.logp(
                Level.SEVERE,
                name,
                "",
                contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg)
            )
        }
    }

    @JvmOverloads
    fun error(msg: String?, e: Throwable?, showCallingMethodInfo: Boolean = false) {
        if (contextPrefix == null) {
            jul.logp(Level.INFO, name, "", (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg), e)
        } else {
            jul.logp(
                Level.INFO,
                name,
                "",
                contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg),
                e
            )
        }
    }

    @JvmOverloads
    fun debug(msg: String?, showCallingMethodInfo: Boolean = false) {
        if (contextPrefix == null) {
            jul.logp(Level.INFO, name, "", (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg))
        } else {
            jul.logp(
                Level.INFO,
                name,
                "",
                contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg)
            )
        }
    }

    @JvmOverloads
    fun debug(msg: String?, e: Throwable?, showCallingMethodInfo: Boolean = false) {
        if (contextPrefix == null) {
            jul.logp(Level.INFO, name, "", (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg), e)
        } else {
            jul.logp(
                Level.INFO,
                name,
                "",
                contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg),
                e
            )
        }
    }

    companion object {
        private var contextPrefix: String? = null

        fun setContextPrefix(prefix: String?) {
            contextPrefix = prefix
        }

        fun clearContextPrefix() {
            contextPrefix = null
        }

        fun getLogger(clazz: Class<*>): Logger {
            return Logger(java.util.logging.Logger.getLogger(clazz.simpleName))
        }

        private val callingMethodInfo: String
            /**
             * TODO: When Android's Java has Thread.threadId() we'll be able to fix this warning
             * @return
             */
            get() {
                val currentThread = Thread.currentThread()
                val stackTrace = currentThread.stackTrace
                var caller = " - <Thread not scheduled yet>"
                if (stackTrace.size >= 5) {
                    val stackElement = stackTrace[5]
                    caller =
                        " - Called from <" + stackElement.fileName + "::" + stackElement.methodName + ":" + stackElement.lineNumber + " on thread:" + currentThread.name + "(tid=" + currentThread.id + ")>"
                }
                if (stackTrace.size >= 6) {
                    val stackElement = stackTrace[6]
                    caller =
                        """
 - invoked by  <${stackElement.fileName}::${stackElement.methodName}:${stackElement.lineNumber} on thread:${currentThread.name}(tid=${currentThread.id})>"""
                }
                return caller
            }

        private fun appendCallingMethodInfo(msg: String): String {
            return msg + callingMethodInfo
        }
    }
}
