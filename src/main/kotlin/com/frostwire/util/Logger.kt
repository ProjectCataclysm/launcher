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
class Logger private constructor(private val name: String) {
    private val logger: org.tinylog.kotlin.TaggedLogger = org.tinylog.kotlin.Logger.tag(name)

    @JvmOverloads
    fun info(msg: String?, showCallingMethodInfo: Boolean = false) {
        if (contextPrefix == null) {
            logger.info((if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg))
        } else {
            logger.info(contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg))
        }
    }

    @JvmOverloads
    fun warn(msg: String?, showCallingMethodInfo: Boolean = false) {
        if (contextPrefix == null) {
            logger.warn((if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg))
        } else {
            logger.warn(contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg))
        }
    }

    @JvmOverloads
    fun warn(msg: String?, e: Throwable?, showCallingMethodInfo: Boolean = false) {
        if (contextPrefix == null) {
            if (e != null) {
                logger.warn(e, (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg).toString())
            } else {
                logger.warn((if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg).toString())
            }
        } else {
            if (e != null) {
                logger.warn(e, contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg).toString())
            } else {
                logger.warn(contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg).toString())
            }
        }
    }

    @JvmOverloads
    fun error(msg: String?, showCallingMethodInfo: Boolean = false) {
        if (contextPrefix == null) {
            logger.error((if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg))
        } else {
            logger.error(contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg))
        }
    }

    @JvmOverloads
    fun error(msg: String?, e: Throwable?, showCallingMethodInfo: Boolean = false) {
        if (contextPrefix == null) {
            if (e != null) {
                logger.error(e, (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg).toString())
            } else {
                logger.error((if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg).toString())
            }
        } else {
            if (e != null) {
                logger.error(e, contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg).toString())
            } else {
                logger.error(contextPrefix + (if (showCallingMethodInfo) msg?.let { appendCallingMethodInfo(it) } else msg).toString())
            }
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
            return Logger(clazz.simpleName)
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
