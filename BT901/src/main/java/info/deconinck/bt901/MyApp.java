package info.deconinck.bt901;

import android.app.Application;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

public class MyApp extends Application {

    // The driver class of CH34x needs to be under the APP class,
    // so that the life cycle of the helper class is the same as the life cycle of the entire application
    public static CH34xUARTDriver driver;

}
