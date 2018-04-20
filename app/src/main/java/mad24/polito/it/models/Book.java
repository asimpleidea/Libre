package mad24.polito.it.models;

public class Book {

    private String user_id;
    private String book_id;

    private String title;
    private String author;
    private String isbn;
    private String location;

    private String bookImageLink;
    private int photo;

    public Book() {
    }

    public Book(String title, String author, String isbn, int photo) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.photo = photo;
    }

    public Book(String title, String author, String isbn, String book_id) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.book_id = book_id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
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

    public String getBook_id() {
        return book_id;
    }
}
