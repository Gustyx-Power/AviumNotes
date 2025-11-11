package id.avium.aviumnotes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import id.avium.aviumnotes.data.preferences.PreferencesManager
import id.avium.aviumnotes.service.FloatingBubbleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "Boot completed, checking if should start floating bubble")

                val pendingResult = goAsync()

                scope.launch {
                    try {
                        val preferencesManager = PreferencesManager(context)
                        val floatingBubbleEnabled = preferencesManager.floatingBubbleEnabled.first()

                        Log.d(TAG, "Floating bubble enabled: $floatingBubbleEnabled")

                        if (floatingBubbleEnabled && Settings.canDrawOverlays(context)) {
                            val serviceIntent = Intent(context, FloatingBubbleService::class.java)
                            serviceIntent.action = FloatingBubbleService.ACTION_START
                            context.startForegroundService(serviceIntent)
                            Log.d(TAG, "Started FloatingBubbleService on boot")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting service on boot", e)
                    } finally {
                        pendingResult.finish()
                        scope.cancel()
                    }
                }
            }
        }
    }
}
