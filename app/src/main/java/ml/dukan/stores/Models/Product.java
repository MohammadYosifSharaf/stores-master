package ml.dukan.stores.Models;

/**
 * Created by khaled on 29/01/17.
 */

public class Product {

    public String name;
    public int category;
    public String substituted_name;
    public  String barcode, substituted_barcode;
    public boolean seen;
    public boolean unavailable;
    public int quantity = 1;
    public float price;
    public int adjustment;
    public long browse;
    public long ad;
    public Product(){}

    public String getImagePath(){
        return "products_images/"+barcode+ (category == 8 ? ".png" : ".jpg");
    }

    public Product(String name, String barcode, float price, int quantity) {
        this.name = name;
        this.barcode = barcode;
        this.price = price;
        this.quantity = quantity;
    }


    public float getTotalPrice () {
        return price * quantity;
    }


    public float getTotalPrice (int quantity) {
        return price * quantity;
    }

    @Override
    public String toString() {
        return String.format("Name: %s Price: %f", name, price);
    }
}
