package mad24.polito.it.models

class Chat constructor(_chat : String = "",
                       _last_message_time : String = "",
                       _last_message_by : String = "",
                       _last_message_id : String = "",
                       _preview : String = "",
                       _partner_id : String = "",
                       _my_last_here : String = "",
                       _book_id : String = "")
{
    val chat_id = _chat
    var last_message_time = _last_message_time
    var last_message_id = _last_message_id
    var last_message_by = _last_message_by
    val partner_id = _partner_id
    var preview = _preview
    var my_last_here = _my_last_here
    var book_id = _book_id
}