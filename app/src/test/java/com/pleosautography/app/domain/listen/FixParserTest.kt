package com.pleosautography.app.domain.listen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FixParserTest {

    @Test
    fun `연도 정정을 꺼낸다`() {
        assertEquals(
            FixParser.Fix("1988년", "1987년"),
            FixParser.parse("오토야, 방금 거기 고치자. 1988년이 아니라 1987년이었어."),
        )
    }

    @Test
    fun `조사와 끝맺음을 뗀다`() {
        assertEquals(FixParser.Fix("포니", "프라이드"), FixParser.parse("포니가 아니라 프라이드였어요"))
        assertEquals(FixParser.Fix("부산", "대구"), FixParser.parse("부산이 아니라 대구야"))
    }

    @Test
    fun `정정이 아니면 null`() {
        assertNull(FixParser.parse("오토야, 방금 거기 고치자"))
        assertNull(FixParser.parse("1988년이 아니라 1988년이었어"))
    }

    @Test
    fun `고친 말 뒤 한 낱말과 조사`() {
        assertEquals("1987년 겨울", ListenSession.phraseAround("1987년 겨울, 차를 받으러 가던 날", "1987년"))
        assertEquals("로", ListenSession.directionParticle("1987년 겨울"))
        assertEquals("으로", ListenSession.directionParticle("대구역"))
        assertEquals("으로", ListenSession.directionParticle("부산"))
        assertEquals("로", ListenSession.directionParticle("서울"))
    }

    @Test
    fun `문장으로 나눈다`() {
        assertEquals(
            listOf("삼 년 동안 샀어요.", "1988년 겨울이었죠."),
            ListenSession.splitSentences("삼 년 동안 샀어요. 1988년 겨울이었죠."),
        )
    }
}
