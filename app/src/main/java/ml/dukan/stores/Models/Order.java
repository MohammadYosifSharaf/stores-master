package ml.dukan.stores.Models;

import android.content.Context;

import java.util.ArrayList;

import ml.dukan.stores.HelperClasses.Util;
import ml.dukan.stores.R;

/**
 * Created by khaled on 20/06/17.
 */

public class Order {

    public String note;
    public String uid;
    public String payment_method;
    public float totalPrice;
    public User personal_information;
    public float discount;
    public long date;
    public int status;
    public String firebase_key;
    public ArrayList<Product> products = new ArrayList<>();

    public String userToString(Context context){
        return personal_information != null ? context.getResources().getString(R.string.order_fresh_customer_info, personal_information.name, personal_information.number, personal_information.address)
                : "";
    }

    public String invoiceDetails(Context context){
        return context.getResources().getString(R.string.order_browser_invoice_information, totalPrice, products.size(), Util.getOverallQuantity(products));
    }



    @Override
    public String toString(){
        return String.format("Client: %s\tMethod: %s\tDate: %s\tPrice: %.2f\tFire_Key: %s\n\nOrders:%n\t\t%s\n",
               uid, payment_method, date, totalPrice, firebase_key, orderToString());
    }

    public String orderToString(){
        StringBuilder b = new StringBuilder();
        for (Product p : products){
            b.append(String.format("Name: %s\tBarcode: %s\tQuantity: %d%n", p.name, p.barcode, p.quantity));
        }
        return b.toString();
    }
}
