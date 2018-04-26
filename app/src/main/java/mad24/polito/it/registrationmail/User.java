package mad24.polito.it.registrationmail;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class User
{
    /**
     * The user's name
     */
    private String name = null;

    /**
     * The user's email
     */
    private String email = null;

    /**
     * The user's gender.
     * Todo: check if this is actually useful
     */
    private String gender = null;

    /**
     * The user's app locale.
     * Todo: check the value of this
     */
    private String locale = "en_US";

    /**
     *  The user's timezone
     */
    private Integer timezone = 0;

    /**
     * The user's phone
     */
    private String phone = null;

    /**
     * Is user verified?
     */
    private Boolean isVerified = false;

    /**
     * User bio
     */
    private String bio = null;

    /**
     * User's favorite genres
     */
    private List<String> genres = null;

    /**
     * User's location
     */
    private String city = null;

    /**
     * User's profile picture
     */
    private String picture = null;

    User()
    {
        genres = new ArrayList<>();
    }

    public static Boolean logged()
    {
        return FirebaseAuth.getInstance().getCurrentUser() == null;
    }

    /**
     * Set the user's name
     * @param name the name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the user's email
     * @return the email
     */
    public String getEmail()
    {
        return email;
    }

    /**
     * Sets the user's email
     * @param email
     */
    public void setEmail(String email)
    {
        this.email = email;
    }

    /**
     * Gets user's gender.
     * @return the gender
     */
    public String getGender()
    {
        return gender;
    }

    /**
     * Sets the user's gender
     * @param gender the gender
     */
    public void setGender(String gender)
    {
        this.gender = gender;
    }

    /**
     * Gets the user's locale
     * @return the locale
     */
    public String getLocale()
    {
        return locale;
    }

    /**
     * Sets the user's locale
     * @param locale
     */
    public void setLocale(String locale)
    {
        this.locale = locale;
    }

    /**
     * Gets the user's timezone
     * @return the timezone in difference from UTC
     */
    public Integer getTimezone()
    {
        return timezone;
    }

    /**
     * Sets the user's Timezone
     * @param timezone the difference from UTC in hours
     */
    public void setTimezone(Integer timezone)
    {
        this.timezone = timezone;
    }

    /**
     * Gets the user's name
     * @return the user's name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Gets the user's phone
     * @return the user's phone
     */
    public String getPhone()
    {
        return phone;
    }

    /**
     * Sets the user's phone
     * @param phone the user's phone
     */
    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    /**
     * Gets whether the user is verified
     * @return true or false
     */
    public Boolean getVerified()
    {
        return isVerified;
    }

    /**
     * Sets users verification
     * @param verified true or false
     */
    public void setVerified(Boolean verified)
    {
        isVerified = verified;
    }

    public String getBio()
    {
        return bio;
    }

    public void setBio(String bio)
    {
        this.bio = bio;
    }

    public List<String> getGenres()
    {
        //  https://stackoverflow.com/a/4042464/3497202
        return genres;
    }

    /**
     * Adds a new genre to user's favorites
     * Todo: check for genre ids
     * @param genre
     */
    public void addFavoriteGenre(String genre)
    {
        genres.add(genre);
    }

    /**
     * Sets user's favorite genres
     * @param genres
     */
    public void setGenres(List<String> genres)
    {
        this.genres = genres;
    }

    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    public String getPicture()
    {
        return picture;
    }

    public void setPicture(String picture)
    {
        this.picture = picture;
    }
}
