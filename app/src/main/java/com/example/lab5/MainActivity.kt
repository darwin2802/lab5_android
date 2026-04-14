package com.example.lab5
import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var maxGForce = 0f
    private val sessionData = ArrayList<Float>()

    private lateinit var tvGForce: TextView
    private lateinit var tvMaxG: TextView
    private lateinit var chartContainer: LinearLayout
    private lateinit var stabilityBar: View
    private lateinit var etThreshold: EditText
    private lateinit var mainLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvGForce = findViewById(R.id.tv_gforce)
        tvMaxG = findViewById(R.id.tv_max_g)
        chartContainer = findViewById(R.id.chart_container)
        stabilityBar = findViewById(R.id.stability_bar)
        etThreshold = findViewById(R.id.et_threshold)
        mainLayout = findViewById(R.id.main_root_layout)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        findViewById<Button>(R.id.btn_open_graph).setOnClickListener {
            showGraphDialog()
        }

        findViewById<Button>(R.id.btn_reset).setOnClickListener {
            maxGForce = 1.0f
            sessionData.clear()
            chartContainer.removeAllViews()
            tvMaxG.text = "Peak recorded: 1.00 G"
            mainLayout.setBackgroundColor(Color.parseColor("#0F172A"))
        }
    }

    private fun showGraphDialog() {
        val chartView = AnalyticChartView(this, null).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 800)
            setData(sessionData)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.parseColor("#0F172A"))
            val info = TextView(context).apply {
                text = " Pinch to Zoom • Peaks highlighted"
                setTextColor(Color.GRAY)
                textSize = 14f
                setPadding(0, 0, 0, 20)
            }
            addView(info)
            addView(chartView)
        }
        AlertDialog.Builder(this)
            .setTitle("G-Force Analytics")
            .setView(layout)
            .setPositiveButton("CLOSE", null)
            .show()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val g = sqrt(event.values[0]*event.values[0] + event.values[1]*event.values[1] + event.values[2]*event.values[2]) / 9.80665f

            if (g > maxGForce) maxGForce = g
            tvGForce.text = String.format(Locale.US, "%.2f G", g)
            tvMaxG.text = String.format(Locale.US, "Peak recorded: %.2f G", maxGForce)

            val threshold = etThreshold.text.toString().toFloatOrNull() ?: 2.5f

            if (g > threshold) {
                mainLayout.setBackgroundColor(Color.parseColor("#7F1D1D"))
            } else {
                mainLayout.setBackgroundColor(Color.parseColor("#0F172A"))
            }

            if (System.currentTimeMillis() % 5 == 0L) {
                sessionData.add(g)
                if (sessionData.size > 200) sessionData.removeAt(0)
            }

            val bar = View(this)
            bar.layoutParams = LinearLayout.LayoutParams(12, (g * 35).toInt().coerceIn(10, 280)).apply { setMargins(2, 0, 2, 0) }
            bar.setBackgroundColor(if (g > threshold) Color.RED else Color.parseColor("#38BDF8"))
            chartContainer.addView(bar)

            if (chartContainer.childCount > 120) chartContainer.removeViewAt(0)

            val jitter = abs(g - 1.0f)
            stabilityBar.layoutParams.width = (jitter * 1500).toInt().coerceIn(50, 800)
            stabilityBar.requestLayout()
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}