package mad24.polito.it.models;

public class Rating
{
    private int stars = 0;
    private String comment = "";

    public Rating()
    {
        this(0, "");
    }

    public Rating(int _stars, String _comment)
    {
        stars = _stars;
        comment = _comment;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
