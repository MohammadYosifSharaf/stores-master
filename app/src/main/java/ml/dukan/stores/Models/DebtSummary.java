package ml.dukan.stores.Models;

/**
 * Created by Khaled on 19/08/17.
 */

public class DebtSummary {
    public float total_price;
    public long current_invoice;
    public float partial_payment;
    public boolean enabled;
    public User profile;
    DebtSummary(){}

    public void copy (DebtSummary e){
        total_price = e.total_price;
        current_invoice = e.current_invoice;
        partial_payment = e.partial_payment;
        enabled = e.enabled;
    }
}
