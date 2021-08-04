package info.deconinck.inclinometer;

import android.app.Application;
import info.deconinck.wtfile.util.SharedUtil;

public class InitApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        SharedUtil.init(getApplicationContext());
    }


}
