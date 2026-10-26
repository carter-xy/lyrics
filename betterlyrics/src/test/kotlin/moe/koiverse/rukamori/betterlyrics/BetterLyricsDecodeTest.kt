package moe.koiverse.rukamori.betterlyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BetterLyricsDecodeTest {
    @Test fun acceptsDocumentedTtmlLrcAndQrcResponses() {
        val ttml = "<tt xmlns=\"http://www.w3.org/ns/ttml\"><body/></tt>"
        val lrc = "[00:01.00]First line\n[00:03.00]Second line"
        val elrc = "[00:01.00]<00:01.00>First <00:01.50>line"
        val qrc = "[1000,2000]First(1000,500) line(1500,500)"
        assertEquals(ttml, BetterLyrics.decodeLyricsContent("""{"ttml":"${ttml.replace("\"", "\\\"")}"}"""))
        assertEquals(lrc, BetterLyrics.decodeLyricsContent("""{"lyrics":"${lrc.replace("\n", "\\n")}","provider":"kugou"}"""))
        assertEquals(elrc, BetterLyrics.decodeLyricsContent("""{"data":{"result":{"lyrics":"$elrc"}}}"""))
        assertEquals(qrc, BetterLyrics.decodeLyricsContent("""{"lyrics":"$qrc","provider":"qq"}"""))
    }

    @Test fun rejectsApiErrorsAndMalformedPayloads() {
        assertNull(BetterLyrics.decodeLyricsContent("""{"error":"Rate limit exceeded","message":"try later"}"""))
        assertNull(BetterLyrics.decodeLyricsContent("""{"lyrics":"<QrcInfos>broken</QrcInfos>"}"""))
        assertNull(BetterLyrics.decodeLyricsContent("""{"lyrics":"hello world"}"""))
        assertNull(BetterLyrics.decodeLyricsContent("<html>Service unavailable</html>"))
    }
}
