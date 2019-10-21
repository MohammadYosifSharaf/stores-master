package ml.dukan.stores;


import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by Khaled on 25/08/17.
 */

public class MyApplication extends Application{


    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
