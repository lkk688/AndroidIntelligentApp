package sjsu.cmpelkk.myandroidmulti.Firebase

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import sjsu.cmpelkk.myandroidmulti.MainActivity
import sjsu.cmpelkk.myandroidmulti.R
import java.security.AccessControlContext

private val NOTIFICATION_ID = 0

fun NotificationManager.sendNotification(messageBody: String, applicationContext: Context) {
    val contentIntent = Intent(applicationContext, MainActivity::class.java) //open MainActivity
    val contentPendingIntent = PendingIntent.getActivity(
        applicationContext,
        NOTIFICATION_ID,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT //do not create a new notification
    )

    val builder = NotificationCompat.Builder(
        applicationContext,
        applicationContext.getString(R.string.notification_channel_id)
    ).setSmallIcon(R.drawable.myalarm)
        .setContentTitle(applicationContext.getString(R.string.notification_channel_name))
        .setContentText(messageBody)
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)

    notify(NOTIFICATION_ID, builder.build())
}

fun NotificationManager.cancelNotification() {
    cancelAll()
}