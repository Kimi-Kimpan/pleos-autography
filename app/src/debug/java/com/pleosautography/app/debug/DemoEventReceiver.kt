package com.pleosautography.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.pleosautography.app.AppGraph
import com.pleosautography.app.domain.demo.DemoEvent

/**
 * 디버그 빌드 전용: adb로 데모 이벤트를 받는다. 시연 영상에 데모 패널이 보이지 않게 할 때 쓴다.
 * adb shell am broadcast -a com.pleosautography.app.DEMO_EVENT -p com.pleosautography.app --es event drive
 */
class DemoEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("event")
        val event = DemoEvent.fromId(id)
        if (event == null) {
            Log.w("DemoEvents", "알 수 없는 데모 이벤트: $id (가능: ${DemoEvent.entries.joinToString { it.id }})")
            return
        }
        AppGraph.get(context).demoEvents.send(event)
    }
}
