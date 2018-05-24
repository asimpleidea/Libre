package mad24.polito.it.models;

import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;

public class UserMail {

    String email;
    String name;
    String city;
    String idCity;
    String phone;
    String bio;
    ArrayList<Integer> genres;
    HashMap<String, Boolean> books;
    double lat;
    double lon;
    boolean fb = false;

    UserStatus status = null;

    public UserMail()
    {
    }

    public UserMail(String email, String name, String city, String idCity, String phone, String bio, ArrayList<Integer> genres, ArrayList<String> books, double lat, double lon) {
        this.email = email;
        this.name = name;
        this.city = city;
        this.idCity = idCity;

        this.phone = phone;
        this.bio = bio;
        this.genres = genres;
        this.lat = lat;
        this.lon = lon;

        this.books = new HashMap<String, Boolean>();
        for(String b : books)
            this.books.put(b, true);

        status = new UserStatus(false, "", "", "");
    }

    public boolean isFb() { return fb; }

    public void setFb(boolean fb) { this.fb = fb; }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getIdCity() {
        return idCity;
    }

    public void setIdCity(String idCity) {
        this.idCity = idCity;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public ArrayList<Integer> getGenres() {
        return genres;
    }

    public void setGenres(ArrayList<Integer> genres) {
        this.genres = genres;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public HashMap<String, Boolean> getBooks() {
        return books;
    }

    public void setBooks(HashMap<String, Boolean> books) {
        this.books = books;
    }

    public UserStatus getStatus() { return status; }

    public void setStatus(UserStatus status) { this.status = status; }
}
