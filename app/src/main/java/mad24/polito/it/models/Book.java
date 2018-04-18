package mad24.polito.it.models;

public class Book {

    private String title;
    private String author;
    private String location;
    private int photo;

    public Book(String title, String author, String location, int photo) {
        this.title = title;
        this.author = author;
        this.location = location;
        this.photo = photo;
    }

    public int getPhoto() {
        return photo;
    }

    public void setPhoto(int photo) {
        this.photo = photo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
