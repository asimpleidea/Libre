package mad24.polito.it.models

class ChatMessage constructor(_content: String, _by : String, _sent: String)
{
    var content = _content
    var by = _by
    var sent = _sent

    //  Because it is not received yet
    var received = "0"
}