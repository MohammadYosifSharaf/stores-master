package ml.dukan.stores.Models;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Khaled on 15/08/17.
 */

public class Market {

    public String uid;
    public String name;
    public String owner_name;
    public String phone_number;
    public Map<String, Object> tokens = new HashMap<>();
    public double range;
    public Boolean hasVeg;
    public boolean takesTax;
    public long cumulativeDuration;
    public int numDelivery;
    public  float cumulativeRating;
    public int numRatings;
    public String address;
    public double lat;
    public double lng;


    public Market(){};

    public Market(String name, String owner_name, String phone_number, String notification_token, double range, String address, double lat, double lng, boolean hasVeg, boolean takesTax) {
        this.name = name;
        this.owner_name = owner_name;
        this.phone_number = phone_number;
        this.tokens.put(notification_token, true);
        this.range = range;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.hasVeg = hasVeg;
        this.takesTax = takesTax;
    }
}
