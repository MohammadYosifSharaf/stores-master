package ml.dukan.stores.Models;

/**
 * Created by khaled on 14/04/17.
 */

public class User {

    public String uid;
    public String name;
    public String number;
    public double latitude;
    public double longitude;
    public String address;
    public String notification_token;
    public String subscription_number;

    public User() {}

    public User(String name, String number, String address, double latitude, double longitude, String token) {
        this.name = name;
        this.number = number;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.notification_token = token;
    }


}
