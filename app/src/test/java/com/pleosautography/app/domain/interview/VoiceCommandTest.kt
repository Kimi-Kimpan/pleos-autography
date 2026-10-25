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

    @Test
    fun `짧은 명령은 답변으로 저장하지 않는다`() {
        assertEquals(VoiceCommand.LISTEN, VoiceCommand.parseCommand("오토야, 이 이야기 들려줘"))
        assertEquals(VoiceCommand.FIX, VoiceCommand.parseCommand("오토야, 방금 거기 고치자. 1988년이 아니라 1987년이었어."))
        assertEquals(VoiceCommand.STOP, VoiceCommand.parseCommand("여기까지"))
        assertEquals(VoiceCommand.WHATS_UP, VoiceCommand.parseCommand("오토야, 뭐해?"))
    }

    @Test
    fun `긴 이야기 속 명령어는 이야기다`() {
        assertNull(VoiceCommand.parseCommand("그날 아버지가 오늘은 여기까지 하자고 하시고는 트럭을 세우셨는데, 그 뒤로 한참을 말없이 앉아 계셨어요."))
    }
}
