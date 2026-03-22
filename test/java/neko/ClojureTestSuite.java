package neko;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import static org.junit.Assert.assertEquals;

/**
 * JUnit bridge that runs Clojure test namespaces under Robolectric.
 *
 * Robolectric provides shadow implementations of Android framework classes
 * so that tests can create Views, Activities, etc. on the JVM.
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

        // Capture clojure.test output so it appears in the JUnit report
        StringBuilder sb = new StringBuilder();
        sb.append("(let [sw (java.io.StringWriter.)");
        sb.append("      result (binding [*out* sw] (clojure.test/run-tests");
        for (String ns : TEST_NAMESPACES) {
            sb.append(" '").append(ns);
        }
        sb.append("))");
        sb.append("      output (str sw)");
        sb.append("      fails (+ (:fail result) (:error result))]");
        sb.append("  (when (pos? fails) (println output))");
        sb.append("  fails)");

        Object failCount = eval.invoke(readString.invoke(sb.toString()));
        assertEquals("Clojure test failures/errors", 0L, ((Number) failCount).longValue());
    }
}
