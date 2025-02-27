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
package com.frostwire.platform

import java.io.File

/**
 * @author gubatron
 * @author aldenml
 */
interface FileSystem {
    fun isDirectory(file: File?): Boolean

    fun isFile(file: File?): Boolean

    fun canRead(file: File?): Boolean

    fun canWrite(file: File?): Boolean

    fun length(file: File?): Long

    fun lastModified(file: File?): Long

    fun exists(file: File?): Boolean

    fun mkdirs(file: File?): Boolean

    fun delete(file: File?): Boolean

    fun listFiles(file: File?, filter: FileFilter?): Array<File?>?

    fun copy(src: File?, dest: File?): Boolean

    fun write(file: File?, data: ByteArray?): Boolean

    fun walk(file: File?, filter: FileFilter?)
}
