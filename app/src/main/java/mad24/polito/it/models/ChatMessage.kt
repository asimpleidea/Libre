package mad24.polito.it.models

import android.R.attr.data
import com.google.firebase.firestore.Exclude


class ChatMessage constructor(_content: String = "", _by : String = "", _sent: String = "")
{
    var content = _content
    var by = _by
    var sent = _sent

    //  Because it is not received yet
    var received = "0"

    @Exclude
    var time : String = ""

    @Exclude
    var date : String = ""

    //@Exclude
    //var dateTime : Date = ""
}