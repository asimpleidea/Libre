package mad24.polito.it.models;

public class Borrowing
{
    private String book_id = "";
    private String from = "";
    private String to = "";
    private long started_at = 0;
    private long returned_at = 0;
    private Rating owner_rating = null;
    private Rating borrower_rating = null;

    public Borrowing()
    {
        this("", "", "", 0, 0, null, null);
    }

    public Borrowing(String _book_id, String _from, String _to, long _started, long _returned)
    {
        this(_book_id, _from, _to, _started, _returned, null, null);
    }

    public Borrowing(String _book_id, String _from, String _to, long _started, long _returned, Rating _owner, Rating _borrower)
    {
        book_id = _book_id;
        from = _from;
        to = _to;
        started_at = _started;
        returned_at = _returned;
        owner_rating = _owner;
        borrower_rating = _borrower;
    }

    public class Rating
    {
        private int stars = 0;
        private String comment = "";

        Rating()
        {
            this(0, "");
        }

        Rating(int _stars, String _comment)
        {
            stars = _stars;
            comment = _comment;
        }
    }

    public String getBook_id() {
        return book_id;
    }

    public void setBook_id(String book_id) {
        this.book_id = book_id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public long getStarted_at() {
        return started_at;
    }

    public void setStarted_at(long started_at) {
        this.started_at = started_at;
    }

    public long getReturned_at() {
        return returned_at;
    }

    public void setReturned_at(long returned_at) {
        this.returned_at = returned_at;
    }

    public Rating getOwner_rating() {
        return owner_rating;
    }

    public void setOwner_rating(Rating owner_rating) {
        this.owner_rating = owner_rating;
    }

    public Rating getBorrower_rating() {
        return borrower_rating;
    }

    public void setBorrower_rating(Rating borrower_rating) {
        this.borrower_rating = borrower_rating;
    }
}
