package mad24.polito.it.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceIdService
import com.google.firebase.iid.FirebaseInstanceId



class MessagingInstanceIDService : FirebaseInstanceIdService()
{

    override fun onTokenRefresh()
    {
        super.onTokenRefresh()

        // Get updated InstanceID token.
        val refreshedToken = FirebaseInstanceId.getInstance().token

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if(uid != null)
        {
            FirebaseDatabase.getInstance().reference
                    .child("users")
                    .child(uid)
                    .child("device_token")
                    .setValue(refreshedToken) { err, d ->

                        if(err != null) Log.d("CHAT", "error while updating token in service")
                    }
        }

    }
}
