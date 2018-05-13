package mad24.polito.it.models;

public class UserStatus
{
    private boolean online = false;
    private String last_online = "";
    private String in_chat = "home";

    public UserStatus() { }

    public UserStatus(boolean _online, String _last_online, String _in_chat)
    {
        online = _online;
        last_online = _last_online;
        in_chat = _in_chat;
    }

    public boolean isOnline() { return online; }

    public void setOnline(boolean online) { this.online = online; }

    public String getLast_online() { return last_online; }

    public void setLast_online(String last_online) { this.last_online = last_online; }

    public String getIn_chat() { return in_chat; }

    public void setIn_chat(String in_chat) { this.in_chat = in_chat; }
}
