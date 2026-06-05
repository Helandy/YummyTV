package su.afk.yummy.tv.feature.player.pip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MobilePlayerPipActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MobilePlayerPipController.handleAction(intent.action)
    }
}
