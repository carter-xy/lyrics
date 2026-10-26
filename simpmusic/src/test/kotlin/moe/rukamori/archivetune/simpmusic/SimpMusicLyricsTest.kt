package moe.rukamori.archivetune.simpmusic

import moe.rukamori.archivetune.simpmusic.models.LyricsData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SimpMusicLyricsTest {
    private val rich = "[00:01.000]<00:01.000>Hello <00:01.500>world"
    private val synced = "[00:01.00]Hello world"

    @Test fun validRichTimingWinsWithinMatchingTrack() {
        val track = LyricsData(duration = 180, richSyncLyrics = rich, syncedLyrics = synced, plainLyrics = "Hello world")
        assertEquals(rich, SimpMusicLyrics.selectLyrics(listOf(track), 180))
        assertEquals(listOf(rich, synced, "Hello world"), SimpMusicLyrics.selectAllLyrics(listOf(track), 180))
    }

    @Test fun unknownDurationAndModerateDriftRemainEligible() {
        val unknown = LyricsData(duration = null, richSyncLyrics = rich)
        val close = LyricsData(duration = 188, syncedLyrics = synced)
        assertEquals(rich, SimpMusicLyrics.selectLyrics(listOf(unknown, close), 180))
        assertEquals(listOf(rich, synced), SimpMusicLyrics.selectAllLyrics(listOf(unknown, close), 180))
        assertEquals(rich, SimpMusicLyrics.selectLyrics(listOf(unknown), 180))
        assertEquals(rich, SimpMusicLyrics.selectLyrics(listOf(unknown), 0))
    }

    @Test fun richTimingWinsAcrossTracksEvenWhenPlainTrackIsCloser() {
        val plain = LyricsData(duration = 180, plainLyrics = "Hello world")
        val wordTimed = LyricsData(duration = 184, richSyncLyrics = rich)
        assertEquals(rich, SimpMusicLyrics.selectLyrics(listOf(plain, wordTimed), 180))
    }

    @Test fun malformedRichAndWrongDurationAreNotSelected() {
        val invalid = LyricsData(duration = 180, richSyncLyrics = "word only", syncedLyrics = synced)
        val wrongRecording = LyricsData(duration = 240, richSyncLyrics = rich)
        assertEquals(synced, SimpMusicLyrics.selectLyrics(listOf(invalid, wrongRecording), 180))
        assertNull(SimpMusicLyrics.selectLyrics(listOf(wrongRecording), 180))
    }
}
