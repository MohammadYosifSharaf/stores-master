package ml.dukan.stores.FirebaseOverrides;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by khaled on 21/06/17.
 */

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        String token = FirebaseInstanceId.getInstance().getToken();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (!(user==null||token==null)){
            String uid = user.getUid();
            FirebaseDatabase.getInstance().getReference()
                    .child("markets").child(uid).child("tokens").child(token).setValue(true);
        }
    }
}
