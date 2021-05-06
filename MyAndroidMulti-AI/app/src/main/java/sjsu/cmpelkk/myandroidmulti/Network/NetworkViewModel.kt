package sjsu.cmpelkk.myandroidmulti.Network

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import sjsu.cmpelkk.myandroidmulti.Firebase.cancelNotification
import sjsu.cmpelkk.myandroidmulti.Firebase.sendNotification
import sjsu.cmpelkk.myandroidmulti.R

//AndroidViewModel is a subclass of ViewModel that is aware of Application context
class NetworkViewModel(private val app: Application): AndroidViewModel(app) {
    private val TOPIC = "MyFCMtoAndroidMulti"

    fun sendNotification() {
        val notificationManager = ContextCompat.getSystemService(
            app, NotificationManager::class.java
        ) as NotificationManager
        notificationManager.cancelNotification()
        notificationManager.sendNotification("Test the notification", app)
    }

    fun createChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                // change importance
                NotificationManager.IMPORTANCE_HIGH
            )// disable badges for this channel
                .apply {
                    setShowBadge(true)
                }

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = app.getString(R.string.notification_channel_description)

            val notificationManager = app.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(notificationChannel)

        }
    }

    fun fetchTokens() {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("TitleViewModel", "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token
                //sendRegistrationToServer(token)

                // Log and toast
                //val msg = getString(R.string.msg_token_fmt, token)
                Log.i("TitleViewModel", "token: "+ token)
            })
    }

    fun subscribeTopic() {
        // [START subscribe_topic]
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
            .addOnCompleteListener { task ->
                var message = app.getString(R.string.message_subscribed)
                if (!task.isSuccessful) {
                    message = app.getString(R.string.message_subscribe_failed)
                }
                Log.i("TitleViewModel", message)
                //Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        // [END subscribe_topics]
    }

}