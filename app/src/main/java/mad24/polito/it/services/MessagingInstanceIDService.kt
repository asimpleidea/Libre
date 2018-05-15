package mad24.polito.it.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.iid.FirebaseInstanceIdService
import com.google.firebase.iid.FirebaseInstanceId



class MessagingInstanceIDService : FirebaseInstanceIdService()
{
    private val tag : String  = "CHAT"
    override fun onCreate()
    {
        super.onCreate()
        Log.d(tag, "MessagingInstanceIDService onCreate")
    }

    override fun onTokenRefresh()
    {
        super.onTokenRefresh()

        // Get updated InstanceID token.
        val refreshedToken = FirebaseInstanceId.getInstance().token
        Log.d(tag, "Refreshed token: " + refreshedToken!!)

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        //sendRegistrationToServer(refreshedToken)
    }
}
