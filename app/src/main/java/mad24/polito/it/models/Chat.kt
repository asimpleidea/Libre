package mad24.polito.it.models

class Chat constructor(_chat : String = "" )
{
    val chat = _chat
    var last_message : LastMessage? = null

    class LastMessage constructor(_by : String = "", _time : String = "", _preview : String = "", _id : String = "")
    {
        val by = _by
        val time = _time
        val preview = _preview
        val id = _id
    }
}