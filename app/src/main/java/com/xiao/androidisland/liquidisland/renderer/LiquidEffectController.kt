package com.xiao.androidisland.liquidisland.renderer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Stable
class LiquidEffectController(
    val isRuntimeShaderSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
) {
    var gravityX: Float by mutableFloatStateOf(0f)
        private set
    var gravityY: Float by mutableFloatStateOf(-9.81f)
        private set

    fun updateGravity(gx: Float, gy: Float) {
        gravityX = gx
        gravityY = gy
    }

    val quantizedGravityAngle: State<Float> = derivedStateOf {
        computeQuantizedGravityAngle(gravityX, gravityY)
    }

    fun computePrimaryLightPosition(extraDegrees: Float): Offset {
        val rad = quantizedGravityAngle.value + (extraDegrees * PI / 180.0).toFloat()
        return Offset(
            x = LIGHT_REF_X + cos(rad),
            y = LIGHT_REF_Y + sin(rad),
        )
    }

    companion object {
        const val LIGHT_REF_X = 0.5f
        const val LIGHT_REF_Y = 0.7f
        const val GRAVITY_DIR_THRESHOLD_SQ = 0.01f
        val GRAVITY_ANGLE_STEP_RAD = (3.0 * PI / 180.0).toFloat()

        fun computeQuantizedGravityAngle(gx: Float, gy: Float): Float {
            val gMagSq = gx * gx + gy * gy
            return if (gMagSq > GRAVITY_DIR_THRESHOLD_SQ) {
                (atan2(gy, gx) / GRAVITY_ANGLE_STEP_RAD).roundToInt() * GRAVITY_ANGLE_STEP_RAD
            } else {
                (-PI / 2.0).toFloat()
            }
        }
    }
}

@Composable
fun rememberLiquidEffectController(): LiquidEffectController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { LiquidEffectController() }

    DisposableEffect(context, lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                if (values.size >= 2) {
                    val gx = (-values[0] / 9.81f).coerceIn(-1f, 1f)
                    val gy = (values[1] / 9.81f).coerceIn(-1f, 1f)
                    controller.updateGravity(gx, gy)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (gravitySensor != null) {
                        sensorManager?.registerListener(
                            listener,
                            gravitySensor,
                            SensorManager.SENSOR_DELAY_UI,
                        )
                    }
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    sensorManager?.unregisterListener(listener)
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager?.unregisterListener(listener)
        }
    }

    return controller
}
