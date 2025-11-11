package id.avium.aviumnotes.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageView
import id.avium.aviumnotes.MainActivity
import id.avium.aviumnotes.R

class ClipboardBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CLIPBOARD_LISTEN = "org.avium.CLIPBOARD_LISTEN"
        private const val TAG = "ClipboardReceiver"
    }

    @SuppressLint("InflateParams")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CLIPBOARD_LISTEN) {
            Log.d(TAG, "Received clipboard broadcast from Avium OS")

            if (Settings.canDrawOverlays(context)) {
                showTemporaryBubble(context)
            }
        }
    }

    private fun showTemporaryBubble(context: Context) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = context.resources.displayMetrics

        val bubbleView = LayoutInflater.from(context)
            .inflate(R.layout.layout_floating_bubble, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = displayMetrics.widthPixels - 200
            y = displayMetrics.heightPixels - 300
        }

        var isViewAttached = true

        windowManager.addView(bubbleView, params)

        bubbleView.findViewById<ImageView>(R.id.bubble_icon)?.setOnClickListener {
            if (isViewAttached) {
                try {
                    windowManager.removeView(bubbleView)
                    isViewAttached = false
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing bubble on click", e)
                }
            }

            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("from_clipboard_broadcast", true)
                putExtra("note_id", -2L)
            }
            context.startActivity(mainIntent)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (isViewAttached) {
                try {
                    windowManager.removeView(bubbleView)
                    isViewAttached = false
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing bubble on timeout", e)
                }
            }
        }, 5000)
    }
}
