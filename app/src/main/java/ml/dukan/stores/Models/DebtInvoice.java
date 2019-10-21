package ml.dukan.stores.Models;

import com.google.firebase.database.DatabaseReference;

/**
 * Created by Khaled on 19/08/17.
 */

public class DebtInvoice {

    public DatabaseReference ref;
    public boolean paid;
    public long date;
    public long paid_date;
    public float partial_payment;
    public float total_price;

    public DebtInvoice(){}

    public void copy(DebtInvoice obj){
        paid = obj.paid;
        partial_payment = obj.partial_payment;
        total_price = obj.total_price;
    }
}
