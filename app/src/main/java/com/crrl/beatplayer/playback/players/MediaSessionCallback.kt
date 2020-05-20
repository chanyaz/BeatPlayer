/*
 * Copyright (c) 2020. Carlos René Ramos López. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.crrl.beatplayer.playback.players

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_NONE
import com.crrl.beatplayer.extensions.toIdList
import com.crrl.beatplayer.extensions.toMediaId
import com.crrl.beatplayer.repository.SongsRepository
import com.crrl.beatplayer.utils.BeatConstants
import com.crrl.beatplayer.utils.BeatConstants.QUEUE_INFO_KEY
import com.crrl.beatplayer.utils.BeatConstants.QUEUE_LIST_KEY
import com.crrl.beatplayer.utils.BeatConstants.QUEUE_LIST_TYPE_KEY
import com.crrl.beatplayer.utils.BeatConstants.REMOVE_SONG
import com.crrl.beatplayer.utils.BeatConstants.REPEAT_ALL
import com.crrl.beatplayer.utils.BeatConstants.REPEAT_MODE
import com.crrl.beatplayer.utils.BeatConstants.REPEAT_ONE
import com.crrl.beatplayer.utils.BeatConstants.RESTORE_MEDIA_SESSION
import com.crrl.beatplayer.utils.BeatConstants.SEEK_TO
import com.crrl.beatplayer.utils.BeatConstants.SET_MEDIA_STATE
import com.crrl.beatplayer.utils.BeatConstants.SHUFFLE_MODE
import com.crrl.beatplayer.utils.BeatConstants.SONG_KEY
import com.crrl.beatplayer.utils.BeatConstants.UPDATE_QUEUE

class MediaSessionCallback(
    private val mediaSession: MediaSessionCompat,
    private val musicPlayer: BeatPlayer,
    private val songsRepository: SongsRepository
) : MediaSessionCompat.Callback() {
    override fun onPause() = musicPlayer.pause()

    override fun onPlay() = musicPlayer.playSong()

    override fun onPlayFromSearch(query: String?, extras: Bundle?) {
        query?.let {
            val song = songsRepository.search(query, 1)
            if (song.isNotEmpty()) {
                musicPlayer.playSong(song.first())
            }
        } ?: onPlay()
    }

    override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
        extras ?: return
        val songId = mediaId.toMediaId().mediaId!!.toLong()
        val queue = extras.getLongArray(QUEUE_INFO_KEY)
        val queueTitle = extras.getString(BeatConstants.SONG_LIST_NAME)
        val seekTo = extras.getInt(SEEK_TO)

        if (queue != null) {
            musicPlayer.setData(queue, queueTitle!!)
        }

        if (seekTo > 0) {
            musicPlayer.seekTo(seekTo)
        }

        musicPlayer.playSong(songId)
    }

    override fun onSeekTo(pos: Long) = musicPlayer.seekTo(pos.toInt())

    override fun onSkipToNext() {
        musicPlayer.nextSong()
    }

    override fun onSkipToPrevious() {
        musicPlayer.previousSong()
    }

    override fun onStop() = musicPlayer.stop()

    override fun onSetRepeatMode(repeatMode: Int) {
        super.onSetRepeatMode(repeatMode)
        val bundle = mediaSession.controller.playbackState.extras ?: Bundle()
        musicPlayer.setPlaybackState(
            PlaybackStateCompat.Builder(mediaSession.controller.playbackState)
                .setExtras(bundle.apply {
                    putInt(REPEAT_MODE, repeatMode)
                }
                ).build()
        )
    }

    override fun onSetShuffleMode(shuffleMode: Int) {
        super.onSetShuffleMode(shuffleMode)
        val bundle = mediaSession.controller.playbackState.extras ?: Bundle()
        musicPlayer.setPlaybackState(
            PlaybackStateCompat.Builder(mediaSession.controller.playbackState)
                .setExtras(bundle.apply {
                    putInt(SHUFFLE_MODE, shuffleMode)
                }).build()
        )
    }

    override fun onCustomAction(action: String?, extras: Bundle?) {
        when (action) {
            SET_MEDIA_STATE -> setSavedMediaSessionState()
            REPEAT_ONE -> musicPlayer.repeatSong()
            REPEAT_ALL -> musicPlayer.repeatQueue()
            RESTORE_MEDIA_SESSION -> restoreMediaSession()
            REMOVE_SONG -> musicPlayer.removeFromQueue(extras?.getLong(SONG_KEY)!!)

            UPDATE_QUEUE -> extras?.let {
                musicPlayer.updateData(
                    it.getLongArray(QUEUE_LIST_KEY)!!,
                    it.getString(QUEUE_LIST_TYPE_KEY)!!
                )
            }
        }
    }

    private fun setSavedMediaSessionState() {
        val controller = mediaSession.controller ?: return
        if (controller.playbackState == null || controller.playbackState.state == STATE_NONE) {
            musicPlayer.restoreQueueData()
        } else {
            restoreMediaSession()
        }
    }

    private fun restoreMediaSession() {
        mediaSession.setMetadata(mediaSession.controller.metadata)
        musicPlayer.setPlaybackState(mediaSession.controller.playbackState)
        musicPlayer.setData(
            mediaSession.controller.queue.toIdList(),
            mediaSession.controller.queueTitle.toString()
        )
    }
}