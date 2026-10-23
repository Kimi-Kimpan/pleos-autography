package com.pleosautography.app.domain.trigger

import com.pleosautography.app.data.navi.NaviSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerEngineTest {

    private val minute = TriggerConfig.MINUTE
    private val guiding = NaviSnapshot(guiding = true, minutesToArrival = 24)

    private fun input(
        atMinute: Long,
        speed: Float? = 40f,
        navi: NaviSnapshot = guiding,
        interview: Boolean = false,
        nudge: Boolean = false,
        driving: Boolean = true,
    ) = TriggerInput(atMinute * minute, driving, speed, navi, interview, nudge)

    @Test
    fun `정체 15분이 지나면 제안한다`() {
        val engine = TriggerEngine()
        assertEquals(emptyList<TriggerAction>(), engine.evaluate(input(0, speed = 8f)))
        assertEquals(emptyList<TriggerAction>(), engine.evaluate(input(14, speed = 8f)))
        assertEquals(listOf(TriggerAction.Nudge(NudgeReason.CONGESTION)), engine.evaluate(input(15, speed = 8f)))
    }

    @Test
    fun `속도가 오르면 정체 시간을 다시 센다`() {
        val engine = TriggerEngine()
        engine.evaluate(input(0, speed = 8f))
        engine.evaluate(input(10, speed = 50f))
        engine.evaluate(input(11, speed = 8f))
        assertEquals(emptyList<TriggerAction>(), engine.evaluate(input(20, speed = 8f)))
        assertEquals(listOf(TriggerAction.Nudge(NudgeReason.CONGESTION)), engine.evaluate(input(26, speed = 8f)))
    }

    @Test
    fun `긴 직진 15분이 지나면 제안한다`() {
        val engine = TriggerEngine()
        val straight = guiding.copy(straightSinceMs = 0)
        assertEquals(emptyList<TriggerAction>(), engine.evaluate(input(14, navi = straight)))
        assertEquals(listOf(TriggerAction.Nudge(NudgeReason.STRAIGHT)), engine.evaluate(input(15, navi = straight)))
    }

    @Test
    fun `제안 뒤 20분 동안은 다시 제안하지 않는다`() {
        val engine = TriggerEngine()
        engine.evaluate(input(0, speed = 8f))
        engine.evaluate(input(15, speed = 8f))
        assertEquals(emptyList<TriggerAction>(), engine.evaluate(input(31, speed = 8f)))
        assertEquals(listOf(TriggerAction.Nudge(NudgeReason.CONGESTION)), engine.evaluate(input(35, speed = 8f)))
    }

    @Test
    fun `인터뷰 중에는 제안하지 않는다`() {
        val engine = TriggerEngine()
        engine.evaluate(input(0, speed = 8f, interview = true))
        assertEquals(emptyList<TriggerAction>(), engine.evaluate(input(15, speed = 8f, interview = true)))
    }

    @Test
    fun `도착 10분 이하면 인터뷰를 마무리하고 새 제안을 하지 않는다`() {
        val engine = TriggerEngine()
        val near = guiding.copy(minutesToArrival = 9)
        assertEquals(listOf(TriggerAction.WrapUp(9)), engine.evaluate(input(0, navi = near, interview = true)))
        // 한 번만 마무리한다.
        assertEquals(emptyList<TriggerAction>(), engine.evaluate(input(1, navi = near, interview = true)))
        engine.evaluate(input(2, speed = 8f, navi = near))
        assertEquals(emptyList<TriggerAction>(), engine.evaluate(input(20, speed = 8f, navi = near)))
    }

    @Test
    fun `도착이 가까우면 떠 있는 제안을 닫는다`() {
        val engine = TriggerEngine()
        val near = guiding.copy(minutesToArrival = 10)
        assertEquals(listOf(TriggerAction.CloseNudge), engine.evaluate(input(0, navi = near, nudge = true)))
    }

    @Test
    fun `복잡한 구간이면 미루고 지나면 다시 제안한다`() {
        val engine = TriggerEngine()
        val complex = guiding.copy(complexSection = true)
        assertEquals(listOf(TriggerAction.Defer), engine.evaluate(input(0, navi = complex, interview = true)))
        assertEquals(emptyList<TriggerAction>(), engine.evaluate(input(1, navi = complex)))
        assertEquals(listOf(TriggerAction.Nudge(NudgeReason.RESUME)), engine.evaluate(input(2)))
        assertEquals(emptyList<TriggerAction>(), engine.evaluate(input(3)))
    }

    @Test
    fun `복잡한 구간에서는 정체 제안도 하지 않는다`() {
        val engine = TriggerEngine()
        val complex = guiding.copy(complexSection = true)
        engine.evaluate(input(0, speed = 8f, navi = complex))
        assertEquals(emptyList<TriggerAction>(), engine.evaluate(input(15, speed = 8f, navi = complex)))
    }

    @Test
    fun `연속 주행 2시간이면 휴식을 한 번 권한다`() {
        val engine = TriggerEngine()
        engine.evaluate(input(0))
        assertTrue(TriggerAction.SuggestRest in engine.evaluate(input(120)))
        assertTrue(TriggerAction.SuggestRest !in engine.evaluate(input(121)))
    }

    @Test
    fun `정차하면 모든 상태를 지운다`() {
        val engine = TriggerEngine()
        engine.evaluate(input(0, speed = 8f))
        engine.evaluate(input(10, driving = false))
        assertEquals(emptyList<TriggerAction>(), engine.evaluate(input(16, speed = 8f)))
    }
}
