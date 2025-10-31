package com.example.team_23_kotlin.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.team_23_kotlin.MainActivity
import com.example.team_23_kotlin.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "📩 Message received from: ${remoteMessage.from}")
        Log.d("FCM", "📦 Data payload: ${remoteMessage.data}")

        // ✅ Leer datos del mensaje (vienen de la Cloud Function)
        val title = remoteMessage.data["title"] ?: "New Product!"
        val body = remoteMessage.data["body"] ?: "Check out what's new."
        val postId = remoteMessage.data["postId"]

        sendNotification(title, body, postId)
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "🆕 New FCM token: $token")

        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid ?: return

        val userRef = firestore.collection("users").document(userId)
        val updates = mapOf(
            "fcmToken" to token,
            "fcmTokenUpdatedAt" to com.google.firebase.Timestamp.now()
        )

        userRef.set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d("FCM", "✅ FCM token saved successfully for user $userId")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "❌ Error saving FCM token", e)
            }
    }

    private fun sendNotification(title: String, messageBody: String, postId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("postId", postId) // ✅ lo enviamos a MainActivity
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "important_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Important Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new products"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }



}
