package mad24.polito.it.models;

import java.util.ArrayList;
import java.util.Date;

public class Book {

    private String user_id;
    private String book_id;

    private String borrowing_id = "";
    private String title;
    private String author;
    private String isbn;
    private String location;
    private String publisher;
    private String editionYear;
    private String condition;

    private String bookImageLink;

    private Date date;

    private ArrayList<Integer> genres;

    public Book() {
    }

    public Book(String title, String author, String isbn, String location, String publisher, String editionYear,
                String condition, String bookImageLink, String book_id, String uid, Date date, ArrayList<Integer> genres) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.location = location;
        this.publisher = publisher;
        this.editionYear = editionYear;
        this.condition = condition;

        this.bookImageLink = bookImageLink;
        this.book_id = book_id;
        this.user_id = uid;
        this.date = date;
        this.genres = genres;
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

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
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

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public void setBook_id(String book_id) {
        this.book_id = book_id;
    }

    public void setBookImageLink(String bookImageLink) {
        this.bookImageLink = bookImageLink;
    }

    public Date getDate() {
        return date;
    }

    public String getEditionYear() {
        return editionYear;
    }

    public void setEditionYear(String editionYear) {
        this.editionYear = editionYear;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public ArrayList<Integer> getGenres() {
        return genres;
    }

    public void setGenres(ArrayList<Integer> genres) {
        this.genres = genres;
    }

    public String getBorrowing_id() { return borrowing_id; }

    public void setBorrowing_id(String borrowing_id) { this.borrowing_id = borrowing_id; }

}
