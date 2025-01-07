/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2020, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.frostwire.transfers

/**
 * @author gubatron
 * @author aldenml
 */
enum class TransferState {
    FINISHING,
    CHECKING,
    DOWNLOADING_METADATA,
    DOWNLOADING_TORRENT,
    DOWNLOADING,
    FINISHED,
    SEEDING,
    PAUSED,
    ERROR,
    ERROR_MOVING_INCOMPLETE,
    ERROR_HASH_MD5,
    ERROR_SIGNATURE,
    ERROR_NOT_ENOUGH_PEERS,
    ERROR_NO_INTERNET,
    ERROR_SAVE_DIR,
    ERROR_TEMP_DIR,
    STOPPED,
    PAUSING,
    CANCELING,
    CANCELED,
    WAITING,
    COMPLETE,
    UPLOADING,
    UNCOMPRESSING,
    DEMUXING,
    UNKNOWN,
    ERROR_DISK_FULL,
    REDIRECTING,
    STREAMING,
    SCANNING,
    ERROR_CONNECTION_TIMED_OUT;

    companion object {
        fun isErrored(state: TransferState): Boolean {
            return state == ERROR ||
                    state == ERROR_MOVING_INCOMPLETE ||
                    state == ERROR_HASH_MD5 ||
                    state == ERROR_SIGNATURE ||
                    state == ERROR_NOT_ENOUGH_PEERS ||
                    state == ERROR_NO_INTERNET ||
                    state == ERROR_SAVE_DIR ||
                    state == ERROR_TEMP_DIR ||
                    state == ERROR_DISK_FULL ||
                    state == ERROR_CONNECTION_TIMED_OUT
        }
    }
}
