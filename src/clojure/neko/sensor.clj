(ns neko.sensor
  "Reactive cells for Android sensor data.

  Cells update automatically as sensor readings arrive:

    (def accel (sensor-cell :accelerometer))
    @accel  ;; => [0.0 9.8 0.0]  (float vector, [x y z])
    (stop-sensor! accel)

  The cell value is a Clojure vector of floats from SensorEvent.values.
  3-axis sensors return [x y z]; scalar sensors return [v].

  Use available? to guard against missing hardware:

    (when (available? :light)
      (def lux-cell (sensor-cell :light)))

  Supported sensor keywords:
    :accelerometer, :gyroscope, :gravity, :linear-acceleration,
    :magnetic-field, :rotation-vector,
    :light, :pressure, :proximity,
    :relative-humidity, :ambient-temperature,
    :step-counter, :step-detector"
  (:require [neko.reactive :as reactive])
  (:import android.content.Context
           android.hardware.Sensor
           android.hardware.SensorEvent
           android.hardware.SensorEventListener
           android.hardware.SensorManager
           neko.App))

;; ---------------------------------------------------------------------------
;; Sensor type keyword → android.hardware.Sensor/TYPE_* constant
;; ---------------------------------------------------------------------------

(def ^:private sensor-types
  {:accelerometer       Sensor/TYPE_ACCELEROMETER
   :magnetic-field      Sensor/TYPE_MAGNETIC_FIELD
   :gyroscope           Sensor/TYPE_GYROSCOPE
   :light               Sensor/TYPE_LIGHT
   :pressure            Sensor/TYPE_PRESSURE
   :proximity           Sensor/TYPE_PROXIMITY
   :gravity             Sensor/TYPE_GRAVITY
   :linear-acceleration Sensor/TYPE_LINEAR_ACCELERATION
   :rotation-vector     Sensor/TYPE_ROTATION_VECTOR
   :relative-humidity   Sensor/TYPE_RELATIVE_HUMIDITY
   :ambient-temperature Sensor/TYPE_AMBIENT_TEMPERATURE
   :step-counter        Sensor/TYPE_STEP_COUNTER
   :step-detector       Sensor/TYPE_STEP_DETECTOR})

;; ---------------------------------------------------------------------------
;; Delay keyword → SensorManager delay constant
;; ---------------------------------------------------------------------------

(def ^:private delay-constants
  {:fastest SensorManager/SENSOR_DELAY_FASTEST
   :game    SensorManager/SENSOR_DELAY_GAME
   :ui      SensorManager/SENSOR_DELAY_UI
   :normal  SensorManager/SENSOR_DELAY_NORMAL})

;; ---------------------------------------------------------------------------
;; Registry: cell identity → [SensorManager, SensorEventListener]
;; Enables stop-sensor! to unregister without the caller holding a reference
;; to the listener.
;; ---------------------------------------------------------------------------

(def ^:private active-sensors (atom {}))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

(defn available?
  "Returns true if sensor-kw is supported on this device."
  ([sensor-kw]
   (available? App/instance sensor-kw))
  ([^Context context sensor-kw]
   (let [^SensorManager sm (.getSystemService context Context/SENSOR_SERVICE)
         sensor-type (if (keyword? sensor-kw)
                       (get sensor-types sensor-kw)
                       (int sensor-kw))]
     (boolean (when sensor-type (.getDefaultSensor sm sensor-type))))))

(defn sensor-cell
  "Returns a reactive Cell that updates with sensor readings, or nil if the
  sensor is not available on this device.

  sensor-kw  - keyword identifying the sensor (see namespace docstring)
  context    - Android Context (defaults to App/instance)

  Options (keyword arguments):
    :delay  - sampling rate: :game (default), :fastest, :ui, :normal,
              or an integer number of microseconds.
              Note: :fastest (0 µs) requires the HIGH_SAMPLING_RATE_SENSORS
              permission on Android 12+.

  The cell value is a vector of floats from SensorEvent.values.

  Call (stop-sensor! cell) to unregister the listener when done."
  ([sensor-kw]
   (sensor-cell App/instance sensor-kw :delay :game))
  ([sensor-kw-or-context sensor-kw-or-delay]
   (if (instance? Context sensor-kw-or-context)
     (sensor-cell sensor-kw-or-context sensor-kw-or-delay :delay :game)
     ;; (sensor-cell :sensor-kw :delay delay-kw) shorthand
     (sensor-cell App/instance sensor-kw-or-context :delay sensor-kw-or-delay)))
  ([^Context context sensor-kw & {:keys [delay] :or {delay :game}}]
   (let [^SensorManager sm (.getSystemService context Context/SENSOR_SERVICE)
         sensor-type        (if (keyword? sensor-kw)
                              (get sensor-types sensor-kw)
                              (int sensor-kw))
         ^Sensor sensor     (when sensor-type (.getDefaultSensor sm sensor-type))]
     (when sensor
       (let [c         (reactive/cell nil)
             delay-val (if (keyword? delay)
                         (get delay-constants delay
                              SensorManager/SENSOR_DELAY_FASTEST)
                         (int delay))
             listener  (proxy [SensorEventListener] []
                         (onSensorChanged [^SensorEvent event]
                           (reset! c (vec (. event -values))))
                         (onAccuracyChanged [sensor accuracy]))]
         (.registerListener sm listener sensor delay-val)
         (swap! active-sensors assoc (System/identityHashCode c) [sm listener])
         c)))))

(defn stop-sensor!
  "Unregisters the sensor listener backing cell.
  The cell stops receiving updates after this call."
  [cell]
  (when cell
    (when-let [[^SensorManager sm listener]
               (get @active-sensors (System/identityHashCode cell))]
      (.unregisterListener sm listener)
      (swap! active-sensors dissoc (System/identityHashCode cell)))))
