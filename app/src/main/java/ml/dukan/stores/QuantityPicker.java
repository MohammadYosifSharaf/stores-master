package ml.dukan.stores;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import ml.dukan.stores.CustomViews.TextViewSquared;
import ml.dukan.stores.listeners.QuantityChangeListener;

/**
 * Created by khaled on 14/07/17.
 */

public class QuantityPicker extends Dialog {


    int quantity, max_quantity, position;
    QuantityChangeListener listener;
    TextView confirm;
    public QuantityPicker(Context context, int position, int quantity, int max_quantity) {
        super(context);
        this.position = position;
        this.quantity = quantity == -1 ? max_quantity : quantity;
        this.max_quantity = max_quantity;

    }


    public QuantityPicker setQuantityChangeListener(QuantityChangeListener listener){
        this.listener = listener;
        return this;
    }

    private void fitScreen(){
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(lp);
    }


    TextView quantityTV;
    TextViewSquared incBtn, decBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_quantity_picker);


        fitScreen();

        setCanceledOnTouchOutside(false);



        quantityTV = (TextView) findViewById(R.id.dialog_quantity_tv);
        confirm = (TextView) findViewById(R.id.dialog_quantity_confirm);
        incBtn = (TextViewSquared) findViewById(R.id.dialog_quantity_plus);
        decBtn = (TextViewSquared) findViewById(R.id.dialog_quantity_minus);
        quantityTV.setText(String.valueOf(quantity));


        incBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                increase();
            }
        });

        decBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                decrease();
            }
        });

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirm();
            }
        });
    }


    private void confirm (){
        int q = Integer.parseInt(quantityTV.getText().toString());
        listener.onChange(position, q);
        dismiss();
    }


    public void increase(){
        int q = Integer.parseInt(quantityTV.getText().toString());
        if (q == max_quantity) return;
        q++;
        quantityTV.setText(String.valueOf(q));
    }

    public void decrease(){
        int q = Integer.parseInt(quantityTV.getText().toString());
        if (q == 1) return;
        q--;
        quantityTV.setText(String.valueOf(q));
    }
}
