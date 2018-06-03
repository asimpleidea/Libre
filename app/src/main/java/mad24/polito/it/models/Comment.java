package mad24.polito.it.models;

public class Comment {
    private float stars = 0;
    private String comment = "";
    private String user = null;

    public Comment() {}

    public Comment(String user, int _stars, String _comment)
    {
        this.user = user;
        this.stars = _stars;
        this.comment = _comment;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public float getStars() {
        return stars;
    }

    public void setStars(float stars) {
        this.stars = stars;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
