package mad24.polito.it.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import mad24.polito.it.R
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import mad24.polito.it.chats.ChatActivity
import java.net.HttpURLConnection
import java.net.URI


class MessagingService : FirebaseMessagingService()
{
    private var lastNotificationId = 0
    private lateinit var notificationBuilder : NotificationCompat.Builder

    override fun onMessageReceived(p0: RemoteMessage?)
    {
        super.onMessageReceived(p0)

        if(p0 == null) return

        //  Set up the notification builder
        notificationBuilder = NotificationCompat.Builder(applicationContext, getString(R.string.channel_name))

        //  Set the icon
        notificationBuilder.setSmallIcon(R.drawable.ic_icon_notification)

        //  Does it have a notification body?
        if(p0.notification == null) return

        notificationBuilder.setContentTitle(p0.notification!!.title)
                .setContentText(p0.notification!!.body)
                .setDefaults(Notification.DEFAULT_ALL)


        //  Set it to be a heads up display
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) notificationBuilder.priority = NotificationManager.IMPORTANCE_HIGH
        else
        {
            notificationBuilder.priority = NotificationCompat.PRIORITY_HIGH
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) notificationBuilder.setVibrate(LongArray(0))
        }

        //  Does it have a payload?
        if (p0.data != null && p0.data.isNotEmpty())
        {
            val data = p0.data

            //  Get the chat id, so that when we tap the notification, we get directed there
            if(data.containsKey("chat_id") && data.containsKey("partner_id"))
            {
                val intent = Intent(this, ChatActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra("partner_id", data.getValue("partner_id"))

                var pendingIntent : PendingIntent? = null
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                {
                    val stackBuilder = TaskStackBuilder.create(this)
                    stackBuilder.addNextIntentWithParentStack(intent)
                    pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
                }
                else pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

                //  Merge all this stuff together
                notificationBuilder.setContentIntent(pendingIntent).setAutoCancel(true)
            }

            //  Get my partner's image
            if(data.containsKey("partner_pic") && data.getValue("partner_pic").isNotBlank())
            {
                if(data.getValue("partner_pic") == "unknown")
                {
                    notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.unknown_user))
                    send()
                }
                else
                {
                    val prefix = (fun() : String
                    {
                        if(data.getValue("partner_pic") == "thumb") return "thumb_"
                        return ""
                    }())

                    FirebaseStorage.getInstance().reference.child("profile_pictures/$prefix${data.getValue("partner_id")}")
                            .downloadUrl.addOnSuccessListener { uri ->

                        //  Get the image url
                        val u = URI(uri.toString())
                        val connection = u.toURL().openConnection() as HttpURLConnection
                        connection.doInput = true
                        connection.connect()

                        //  Get the input stream
                        val ins = connection.inputStream
                        val bitmap = BitmapFactory.decodeStream(ins)

                        //  Finally set the image
                        notificationBuilder.setLargeIcon(bitmap)

                        //  Send it
                        send()
                    }.addOnFailureListener {_ ->

                        //  Send it without the user's image
                        send()
                    }
                }
            }
            else send()
        }
    }

    private fun send()
    {
        //  Finally, display it
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(++lastNotificationId, notificationBuilder.build())
    }
}
