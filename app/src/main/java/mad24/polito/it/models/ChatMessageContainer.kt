package mad24.polito.it.models

class ChatMessageContainer constructor(partecipants : Partecipants)
{
    //var messages : HashMap<String, ChatMessage> = HashMap()

    class Partecipants
    {
        var partecipants : HashMap<String, User> = HashMap(2)

        public fun addPartecipant(id: String, u: User)
        {
            partecipants.put(id, u)
        }

        class User constructor(_last_here : String)
        {
            //  NOTE: firebase models don't like when you call a variable with is_something because it automatically
            //  deletes the is. So I have to write it like this, isis =D
            var isIs_typing : Boolean = false
            var last_here : String = _last_here
        }
    }
}