package su.afk.yummy.tv.android

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle

class DeepLinkRouterActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val target = if (isTelevision()) TvActivity::class.java else MobileActivity::class.java
        startActivity(
            Intent(this, target).apply {
                action = intent.action
                data = intent.data
                putExtras(intent)
            }
        )
        finish()
    }

    private fun isTelevision(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
}
