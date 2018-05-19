package mad24.polito.it.models

import com.google.firebase.firestore.Exclude
import java.util.*


class ChatMessage constructor(_content: String = "", _by : String = "", _sent: String = "", _firstOfTheDay : Boolean = false)
{
    var content = _content
    var by = _by
    var sent = _sent

    //  Because it is not received yet
    var received = "0"

    var time : String = ""
        @Exclude get

    var date : String = ""
        @Exclude get

    var day : String = ""
        @Exclude get

    var firstOfTheDay : Boolean = _firstOfTheDay

    //@Exclude
    //var dateTime : Date = ""
}