package neko;

import android.app.Application;

/**
 * Application subclass that stores a global application context.
 *
 * <p>neko's convenience functions (toast, shared-prefs, sensor, etc.) use
 * {@link #instance} as the default context when no explicit context is
 * provided. If your app uses a different Application class (e.g.
 * {@code ClojureApp} from runtime-core), set this field early — ideally
 * in {@code Application.onCreate()} — via reflection or by calling
 * {@code neko.context/set-app-context!} from Clojure.</p>
 */
public class App extends Application {

    /** Global application instance used by neko convenience functions. */
    public static Application instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
