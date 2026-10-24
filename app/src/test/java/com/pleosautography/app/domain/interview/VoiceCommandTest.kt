package com.pleosautography.app.domain.interview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandTest {

    @Test
    fun `명령을 알아듣는다`() {
        assertEquals(VoiceCommand.STOP, VoiceCommand.parse("오토야, 잠깐만"))
        assertEquals(VoiceCommand.STOP, VoiceCommand.parse("여기까지"))
        assertEquals(VoiceCommand.WHATS_UP, VoiceCommand.parse("오토야 뭐해?"))
        assertEquals(VoiceCommand.ACCEPT, VoiceCommand.parse("응"))
        assertEquals(VoiceCommand.ACCEPT, VoiceCommand.parse("그래, 이어서 하자"))
        assertEquals(VoiceCommand.LISTEN, VoiceCommand.parse("오토야, 이 이야기 들려줘"))
        assertEquals(VoiceCommand.FIX, VoiceCommand.parse("방금 거기 고치자"))
    }

    @Test
    fun `답변 속 ‘응’은 명령이 아니다`() {
        assertNull(VoiceCommand.parse("응, 그날은 눈이 많이 왔어요"))
        assertNull(VoiceCommand.parse("스물아홉 살 겨울이었어요."))
    }
}
