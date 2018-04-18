package mad24.polito.it.registrationmail;

public class User {
    String email;
    String name;
    String city;
    String phone;
    String bio;
    //String[] genres;

    public User() {

    }

    public User(String email, String name, String city, String phone, String bio) {
        this.email = email;
        this.name = name;
        this.city = city;
        this.phone = phone;
        this.bio = bio;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getPhone() {
        return phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getBio() {
        return bio;

    }

    public String getEmail() {

        return email;
    }
}
