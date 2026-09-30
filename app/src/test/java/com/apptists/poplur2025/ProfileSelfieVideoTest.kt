package com.apptists.poplur2025

import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileSelfieVideoTest {
    private fun dataUrl(type: String, bytes: ByteArray) = "data:$type;base64," + Base64.getEncoder().encodeToString(bytes)

    @Test
    fun readsAWebmClipWhoseTypeCarriesACodecList() {
        val parsed = ProfileSelfieVideo.parse(dataUrl("video/webm;codecs=vp8,opus", byteArrayOf(1, 2, 3)))
        assertNotNull(parsed)
        assertEquals("video/webm", parsed!!.mimeType)
        assertEquals("webm", parsed.extension)
        assertArrayEquals(byteArrayOf(1, 2, 3), parsed.bytes)
    }

    @Test
    fun readsAnMp4Clip() {
        val parsed = ProfileSelfieVideo.parse(dataUrl("video/mp4", byteArrayOf(9)))
        assertEquals("mp4", parsed?.extension)
        assertEquals("video/mp4", parsed?.mimeType)
    }

    @Test
    fun refusesAnythingThatIsNotABase64Video() {
        assertNull(ProfileSelfieVideo.parse(dataUrl("image/jpeg", byteArrayOf(1))))
        assertNull(ProfileSelfieVideo.parse("data:video/mp4,plain-text"))
        assertNull(ProfileSelfieVideo.parse("not a data url"))
        assertNull(ProfileSelfieVideo.parse("data:video/mp4;base64,***"))
    }

    @Test
    fun refusesAnEmptyOrOversizedClip() {
        assertNull(ProfileSelfieVideo.parse("data:video/mp4;base64,"))
        assertNull(ProfileSelfieVideo.parse(dataUrl("video/mp4", ByteArray(20)), maxBytes = 10))
    }
}
