package mad24.polito.it.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import mad24.polito.it.R
import java.lang.Exception
import javax.sql.DataSource
import android.graphics.drawable.BitmapDrawable
import android.transition.Transition
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.BaseTarget
import com.bumptech.glide.request.target.SimpleTarget
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.app.NotificationManagerCompat
import com.bumptech.glide.Priority
import mad24.polito.it.chats.ChatActivity
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL


class MessagingService : FirebaseMessagingService()
{
    private val tag : String  = "CHAT"
    private var lastNotificationId = 0

    override fun onMessageReceived(p0: RemoteMessage?)
    {
        super.onMessageReceived(p0)

        if(p0 == null) return

        //  Set up the notification builder
        val notificationBuilder : NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, getString(R.string.channel_name))

        //  Set the icon
        notificationBuilder.setSmallIcon(R.drawable.ic_icon_notification)

        //  Does it have a payload?
        if (p0.data != null && p0.data.isNotEmpty())
        {
            val data = p0.data

            //  Get my partner's image
            if(data.containsKey("partner_pic") && data.getValue("partner_pic").isNotBlank())
            {
                //  Get the image url
                val u = URI(data.getValue("partner_pic"))
                val connection = u.toURL().openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()

                //  Get the input stream
                val ins = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(ins)

                //  Finally set the image
                notificationBuilder.setLargeIcon(bitmap)
            }

            //  Get the chat id, so that when we tap the notification, we get directed there
            if(data.containsKey("chat_id") && data.containsKey("partner_id"))
            {
                val intent = Intent(this, ChatActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra("chat", data.getValue("chat_id"))
                if(data.containsKey("last_message_id")) intent.putExtra("startId", data.getValue("last_message_id"))
                if(data.containsKey("last_message_time")) intent.putExtra("startTime", data.getValue("last_message_time") )
                intent.putExtra("partner_id", data.getValue("partner_id"))
                intent.putExtra("partner_name", data.getValue("partner_name"))

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
        }

        //  Does it have a notification body?
        if(p0.notification == null) return
        notificationBuilder.setContentTitle(p0.notification!!.title)
                            .setContentText(p0.notification!!.body)
                            .setDefaults(Notification.DEFAULT_ALL)


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) notificationBuilder.priority = NotificationManager.IMPORTANCE_HIGH
        else
        {
            notificationBuilder.priority = NotificationCompat.PRIORITY_HIGH
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) notificationBuilder.setVibrate(LongArray(0))
        }

        //  Finally, display it
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(++lastNotificationId, notificationBuilder.build())
    }
}
