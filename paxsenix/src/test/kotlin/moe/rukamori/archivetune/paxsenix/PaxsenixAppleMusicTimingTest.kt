package moe.rukamori.archivetune.paxsenix

import moe.rukamori.archivetune.paxsenix.models.AppleMusicLine
import moe.rukamori.archivetune.paxsenix.models.AppleMusicLyricsResponse
import moe.rukamori.archivetune.paxsenix.models.AppleMusicWord
import org.junit.Assert.assertEquals
import org.junit.Test

class PaxsenixAppleMusicTimingTest {
    @Test fun preservesCompleteMillisecondWordTimestampsAsElrc() {
        val response = AppleMusicLyricsResponse(content = listOf(
            AppleMusicLine(1_000, listOf(AppleMusicWord("Hello", 1_000), AppleMusicWord("world", 1_537))),
        ))
        assertEquals(
            "[00:01.000]<00:01.000>Hello <00:01.537>world",
            PaxsenixLyrics.convertAppleMusicToLrc(response),
        )
    }

    @Test fun incompleteOrRegressingWordTimesStayLineTimed() {
        val response = AppleMusicLyricsResponse(content = listOf(
            AppleMusicLine(2_000, listOf(AppleMusicWord("Missing"), AppleMusicWord("time", 2_300))),
            AppleMusicLine(4_000, listOf(AppleMusicWord("Out", 4_500), AppleMusicWord("of order", 4_200))),
        ))
        assertEquals(
            "[00:02.000]Missing time\n[00:04.000]Out of order",
            PaxsenixLyrics.convertAppleMusicToLrc(response),
        )
    }
}
