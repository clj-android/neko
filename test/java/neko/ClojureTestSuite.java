package neko;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import static org.junit.Assert.fail;

/**
 * JUnit bridge that runs Clojure test namespaces under Robolectric.
 *
 * clojure.test output prints to stdout; Gradle's testLogging config
 * ensures it appears in the console on failure.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ClojureTestSuite {

    /**
     * Test namespaces that don't require gen-class / defactivity.
     *
     * Excluded (require defactivity / gen-class which is incompatible with
     * Robolectric's instrumenting classloader):
     *   neko.t-activity, neko.t-debug, neko.dialog.t-alert,
     *   neko.t-intent (transitively requires neko.t-activity)
     */
    private static final String[] TEST_NAMESPACES = {
        "neko.t-ui",
        "neko.t-reactive",
        "neko.t-threading",
        "neko.t-context",
        "neko.t-data",
        "neko.t-doc",
        "neko.t-find-view",
        "neko.t-log",
        "neko.t-notify",
        "neko.t-utils",
        "neko.listeners.t-view",
        "neko.listeners.t-adapter-view",
        "neko.listeners.t-text-view",
        "neko.ui.t-adapters",
        "neko.ui.t-listview",
        "neko.compliment.t-ui-widgets-and-attributes",
    };

    @BeforeClass
    public static void initNeko() {
        try {
            Class<?> appClass = Class.forName("neko.App");
            java.lang.reflect.Field instance = appClass.getField("instance");
            instance.set(null, RuntimeEnvironment.getApplication());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize neko.App", e);
        }
    }

    @Test
    public void runAllClojureTests() {
        IFn require = Clojure.var("clojure.core", "require");
        IFn symbol = Clojure.var("clojure.core", "symbol");

        require.invoke(symbol.invoke("clojure.test"));

        for (String ns : TEST_NAMESPACES) {
            try {
                require.invoke(symbol.invoke(ns));
            } catch (Exception e) {
                throw new RuntimeException("Failed to require " + ns, e);
            }
        }

        IFn eval = Clojure.var("clojure.core", "eval");
        IFn readString = Clojure.var("clojure.core", "read-string");

        // Run tests with output going to stdout (captured by Gradle).
        // Return only the fail+error count as a plain number.
        StringBuilder sb = new StringBuilder();
        sb.append("(let [result (clojure.test/run-tests");
        for (String ns : TEST_NAMESPACES) {
            sb.append(" '").append(ns);
        }
        sb.append(")] (+ (:fail result) (:error result)))");

        Object failCount = eval.invoke(readString.invoke(sb.toString()));
        long fails = ((Number) failCount).longValue();
        if (fails > 0) {
            fail(fails + " clojure.test failure(s) — see test output above");
        }
    }
}
