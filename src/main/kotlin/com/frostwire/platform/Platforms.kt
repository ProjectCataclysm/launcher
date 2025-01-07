/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2025, FrostWire(R). All rights reserved.
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
package com.frostwire.platform

import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * @author gubatron
 * @author aldenml
 */
object Platforms {
    private val platform = AtomicReference<Platform>()

    fun get(): Platform {
        val p = platform.get()
        checkNotNull(p) { "Platform can't be null" }
        return p
    }

    fun set(p: Platform) {
        platform.compareAndSet(null, p)
    }

    /**
     * Shortcut to current platform file system.
     *
     * @return
     */
    fun fileSystem(): FileSystem {
        return get().fileSystem()
    }

    /**
     * Shortcut to current platform application settings.
     *
     * @return
     */
    fun appSettings(): AppSettings {
        return get().appSettings()
    }

    /**
     * Shortcut to current platform file system data method.
     *
     * @return
     */
    fun data(): File {
        return get().systemPaths().data()
    }

    /**
     * Shortcut to current platform file system torrents method.
     *
     * @return
     */
    fun torrents(): File {
        return get().systemPaths().torrents()
    }

    /**
     * Shortcut to current platform file system temp method.
     *
     * @return
     */
    fun temp(): File {
        return get().systemPaths().temp()
    }
}
