package mad24.polito.it.registrationmail;

import android.net.Uri;

import java.util.ArrayList;

public class UserMail {
    String email;
    String name;
    String city;
    String phone;
    String bio;
    ArrayList<String> genres;
    String profileImage;

    public UserMail() {

    }

    public UserMail(String email, String name, String city, String phone, String bio, ArrayList<String> genres, String profileImage) {
        this.email = email;
        this.name = name;
        this.city = city;
        this.phone = phone;
        this.bio = bio;
        this.genres = genres;
        this.profileImage = profileImage;
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

    public ArrayList<String> getGenres() {
        return genres;
    }

    public void setGenres(ArrayList<String> genres) {
        this.genres = genres;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
