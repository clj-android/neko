package neko;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import clojure.lang.RT;
import clojure.lang.IFn;
import java.lang.reflect.Method;

public class App extends Application {

    private static String TAG = "neko.App";
    public static Application instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        try {
            Class<?> androidCLclass = Class.forName("clojure.lang.AndroidDynamicClassLoader");
            Method setContext = androidCLclass.getMethod("setContext", Context.class);
            setContext.invoke(null, this);
        } catch (ClassNotFoundException e) {
            Log.i(TAG, "AndroidDynamicClassLoader not found — REPL support not available.");
        } catch (Exception e) {
            Log.e(TAG, "setContext method not found, check if your Clojure dependency is correct.");
        }
    }

    // This method is only necessary for asynchronous loading. Clojure is
    // perfectly capable of loading itself the first time anything from it is
    // called.
    public static void loadClojure() {
        IFn load = (IFn)RT.var("clojure.core", "load");
        load.invoke("/neko/activity");

        try {
            load.invoke("/neko/tools/repl");
            IFn init = (IFn)RT.var("neko.tools.repl", "init");
            init.invoke();
        } catch (Exception e) {
            Log.i(TAG, "Could not find neko.tools.repl, REPL not available.");
        }
    }

    public static void loadAsynchronously(final String activityClass, final Runnable callback) {
        new Thread(Thread.currentThread().getThreadGroup(),
                   new Runnable(){
                       @Override
                       public void run() {
                           loadClojure();

                           try {
                               Class.forName(activityClass);
                           } catch (ClassNotFoundException e) {
                               Log.e(TAG, "Failed loading activity " + activityClass, e);
                           }

                           callback.run();
                       }
                   },
                   "ClojureLoadingThread",
                   1048576 // = 1MB, thread stack size in bytes
                   ).start();
    }

}
