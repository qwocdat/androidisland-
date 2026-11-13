package com.xiao.androidisland.liquidisland.renderer

import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * JNI Bridge to the C++17 / Android NDK OpenGL ES 3.0 renderer (`libliquidisland_gl.so`).
 * Safely falls back if native library loading is unavailable in a unit test JVM environment.
 */
object LiquidGlassNativeBridge {

    private const val TAG = "LiquidGlassNative"

    val isNativeLibraryLoaded: Boolean = try {
        System.loadLibrary("liquidisland_gl")
        true
    } catch (t: Throwable) {
        Log.w(TAG, "Native library liquidisland_gl not loaded in this process: ${t.message}")
        false
    }

    external fun nativeCreateRenderer(): Long
    external fun nativeDestroyRenderer(handle: Long)
    external fun nativeOnSurfaceCreated(handle: Long)
    external fun nativeOnSurfaceChanged(handle: Long, width: Int, height: Int)
    external fun nativeOnDrawFrame(handle: Long)
    external fun nativeUpdateIslandState(
        handle: Long,
        cx: Float, cy: Float, w: Float, h: Float, radius: Float,
        bx: Float, by: Float, br: Float, sminK: Float,
        refHeight: Float, refAmount: Float, aberration: Float,
        leftVelX: Float, rightVelX: Float,
        absorptionStr: Float, blurStr: Float,
    )
    external fun nativeUpdateTouchRipple(handle: Long, tx: Float, ty: Float, progress: Float)
}

class NativeLiquidIslandGlRenderer : GLSurfaceView.Renderer {

    private var nativeHandle: Long = if (LiquidGlassNativeBridge.isNativeLibraryLoaded) {
        LiquidGlassNativeBridge.nativeCreateRenderer()
    } else {
        0L
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        if (nativeHandle != 0L) {
            LiquidGlassNativeBridge.nativeOnSurfaceCreated(nativeHandle)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        if (nativeHandle != 0L) {
            LiquidGlassNativeBridge.nativeOnSurfaceChanged(nativeHandle, width, height)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        if (nativeHandle != 0L) {
            LiquidGlassNativeBridge.nativeOnDrawFrame(nativeHandle)
        }
    }

    fun updateGeometryAndOptics(
        cx: Float, cy: Float, w: Float, h: Float, radius: Float,
        bx: Float, by: Float, br: Float, sminK: Float,
        refHeight: Float, refAmount: Float, aberration: Float,
        leftVelX: Float, rightVelX: Float,
        absorptionStr: Float, blurStr: Float,
    ) {
        if (nativeHandle != 0L) {
            LiquidGlassNativeBridge.nativeUpdateIslandState(
                nativeHandle,
                cx, cy, w, h, radius,
                bx, by, br, sminK,
                refHeight, refAmount, aberration,
                leftVelX, rightVelX,
                absorptionStr, blurStr,
            )
        }
    }

    fun updateTouch(tx: Float, ty: Float, progress: Float) {
        if (nativeHandle != 0L) {
            LiquidGlassNativeBridge.nativeUpdateTouchRipple(nativeHandle, tx, ty, progress)
        }
    }

    fun release() {
        if (nativeHandle != 0L) {
            LiquidGlassNativeBridge.nativeDestroyRenderer(nativeHandle)
            nativeHandle = 0L
        }
    }
}
