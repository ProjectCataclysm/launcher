package ru.cataclysm.services

import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object LauncherLock {
    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    fun unlock() {
        try {
            lock!!.release()
        } catch (e: IOException) {
            Log.err(e, "failed to release file lock")
        }

        try {
            channel!!.close()
        } catch (e: IOException) {
            Log.err(e, "failed to close lock file")
        }
    }

    fun lock() {
        try {
            val lockPath: Path = Settings.LAUNCHER_DIR_PATH.resolve("launcher.lock")
            channel = FileChannel.open(lockPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
            lock = channel?.tryLock()
            if (lock == null) {
                throw AlreadyLaunchedException()
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to create file lock", e)
        }
    }

    /**
     * <br></br><br></br>ProjectCataclysm
     * <br></br>Created: 07.08.2022 15:32
     *
     * @author Knoblul
     */
    class AlreadyLaunchedException : RuntimeException()
}