package mad24.polito.it.models;

import android.net.Uri;

import java.util.ArrayList;

public class UserMail {
    String email;
    String name;
    String city;
    String idCity;
    String phone;
    String bio;
    ArrayList<Integer> genres;

    public UserMail() {

    }

    public UserMail(String email, String name, String city, String idCity, String phone, String bio, ArrayList<Integer> genres) {
        this.email = email;
        this.name = name;
        this.city = city;
        this.idCity = idCity;

        this.phone = phone;
        this.bio = bio;
        this.genres = genres;
    }

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

}
