package ml.dukan.stores.HelperClasses;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

import ml.dukan.stores.Models.Order;
import ml.dukan.stores.Models.Product;
import ml.dukan.stores.Models.User;

/**
 * Created by khaled on 26/06/17.
 */

public class DatabaseAdapter {

    private DatabaseHelper helper;

    public DatabaseAdapter(Context context) {
        if (helper == null){
            helper = DatabaseHelper.getInstance(context);
        }
    }



    public void drop (){
        SQLiteDatabase db =helper.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS '" + helper.ORDERS + "'");
        db.execSQL(helper.CREATE_ORDERS_TABLE);
    }


    static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "Store_App8";
        private static final int DB_VER = 1;
        private static DatabaseHelper instance;

        private static final String BARCODE = "barcode";
        private static final String PRICE = "price";
        private static final String NAME = "name";
        private static final String IMAGE_URL = "image_url";
        private static final String AI = "ai";
        private static final String ORDERS = "orders";
        private static final String ORDER_NUMBER = "id";
        private static final String DATE = "date";
        private static final String PAYMENT_METHOD = "payment_method";
        private static final String QUANTITY = "QUANTITY";
        private static final String TOTAL_PRICE = "total_price";
        private static final String STATUS = "status";
        private static final String FIREBASE_KEY = "firekey";
        private static final String CLIENT_UID = "client_uid";
        private static final String CLIENT_NAME = "client_name";
        private static final String CLIENT_NUMBER = "client_number";
        private static final String CLIENT_ADDRESS = "client_address";
        private static final String CLIENT_LAT = "client_lat";
        private static final String CLIENT_LNG = "client_LNG";
        public static DatabaseHelper getInstance(Context context){
            if (instance == null)
                instance = new DatabaseHelper(context);
            return instance;
        }



        private static final String CREATE_ORDERS_TABLE = "CREATE TABLE "+ORDERS+" ("+ORDER_NUMBER+" INTEGER PRIMARY KEY AUTOINCREMENT, "+NAME+" VARCHAR, "+IMAGE_URL+" TEXT, "+BARCODE+" VARCHAR, "+PRICE+" FLOAT, "+QUANTITY+" INTEGER, "+TOTAL_PRICE+" FLOAT, "+DATE+" VARCHAR(15), "+PAYMENT_METHOD+" INTEGER, "+STATUS+" INTEGER, "+CLIENT_UID+" TEXT, "+CLIENT_NAME+" TEXT, "+CLIENT_NUMBER+" VARCHAR(15), "+CLIENT_ADDRESS+" TEXT, "+CLIENT_LAT+" VARCHAR(32), "+CLIENT_LNG+" VARCHAR(32), "+FIREBASE_KEY+" TEXT);";

        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VER);
        }


        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(CREATE_ORDERS_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        }
    }
}
