package mad24.polito.it.models;

public class Book {

    private String user_id;
    private String book_id;

    private String title;
    private String author;
    private String isbn;
    private String location;

    private String bookImageLink;

    public Book() {
    }

    public Book(String title, String author, String isbn, String bookImageLink, String book_id, String uid) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.bookImageLink = bookImageLink;
        this.book_id = book_id;
        this.user_id = uid;
    }

    public Book(String s, String s1, String s2) {
        this.title = s;
        this.author = s1;
        this.isbn = s2;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
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

    public String getBookImageLink() {
        return bookImageLink;
    }
}
