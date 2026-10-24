package com.pleosautography.app.domain.interview

import org.junit.Assert.assertEquals
import org.junit.Test

class ParticleTest {
    @Test
    fun `받침에 따라 을·를을 고른다`() {
        assertEquals("를", InterviewSession.objectParticle("첫 차"))
        assertEquals("를", InterviewSession.objectParticle("일과 생계"))
        assertEquals("을", InterviewSession.objectParticle("가장 길었던 밤"))
        assertEquals("을", InterviewSession.objectParticle("길 위에서 만난 사람들"))
    }
}
