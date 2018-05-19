package mad24.polito.it.models

class Chat constructor(_chat : String = "",
                       _last_message_time : String = "",
                       _last_message_by : String = "",
                       _last_message_id : String = "",
                       _preview : String = "",
                       _partner_id : String = "")
{
    val chat_id = _chat
    val last_message_time = _last_message_time
    val last_message_id = _last_message_id
    val last_message_by = _last_message_by
    val partner_id = _partner_id
    val preview = _preview
}