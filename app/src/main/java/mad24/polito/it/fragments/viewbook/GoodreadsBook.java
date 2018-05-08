package mad24.polito.it.fragments.viewbook;

public class GoodreadsBook
{
    private String Title = null;
    private String ImageUrl = null;
    private String SmallImageUrl = null;
    private int PublicationYear;
    private String Publisher = null;
    private String Language = null;
    private String Description = null;
    private int RatingsCount;
    private String AvgRating;
    private int PagesCount;
    private String AuthorName = null;
    private String AuthorImageUrl = null;
    private String AuthorSmallImageUrl = null;

    public GoodreadsBook(){}

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getImageUrl() {
        return ImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        ImageUrl = imageUrl;
    }

    public String getSmallImageUrl() {
        return SmallImageUrl;
    }

    public void setSmallImageUrl(String smallImageUrl) {
        SmallImageUrl = smallImageUrl;
    }

    public int getPublicationYear() {
        return PublicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        PublicationYear = publicationYear;
    }

    public String getPublisher() {
        return Publisher;
    }

    public void setPublisher(String publisher) {
        Publisher = publisher;
    }

    public String getLanguage() {
        return Language;
    }

    public void setLanguage(String language) { Language = language; }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public int getRatingsCount() {
        return RatingsCount;
    }

    public void setRatingsCount(int ratingsCount) {
        RatingsCount = ratingsCount;
    }

    public String getAvgRating() {
        return AvgRating;
    }

    public void setAvgRating(String avgRating) {
        AvgRating = avgRating;
    }

    public int getPagesCount() {
        return PagesCount;
    }

    public void setPagesCount(int pagesCount) {
        PagesCount = pagesCount;
    }

    public String getAuthorName() {
        return AuthorName;
    }

    public void setAuthorName(String authorName) {
        AuthorName = authorName;
    }

    public String getAuthorImageUrl() {
        return AuthorImageUrl;
    }

    public void setAuthorImageUrl(String authorImageUrl) {
        AuthorImageUrl = authorImageUrl;
    }

    public String getAuthorSmallImageUrl() {
        return AuthorSmallImageUrl;
    }

    public void setAuthorSmallImageUrl(String authorSmallImageUrl) {
        AuthorSmallImageUrl = authorSmallImageUrl;
    }
}
