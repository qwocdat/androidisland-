#include <jni.h>
#include "LiquidGlassGlRenderer.h"

using liquidisland::LiquidGlassGlRenderer;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_xiao_androidisland_liquidisland_renderer_LiquidGlassNativeBridge_nativeCreateRenderer(
    JNIEnv* /*env*/,
    jobject /*thiz*/
) {
    auto* renderer = new LiquidGlassGlRenderer();
    return reinterpret_cast<jlong>(renderer);
}

JNIEXPORT void JNICALL
Java_com_xiao_androidisland_liquidisland_renderer_LiquidGlassNativeBridge_nativeDestroyRenderer(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle
) {
    auto* renderer = reinterpret_cast<LiquidGlassGlRenderer*>(handle);
    delete renderer;
}

JNIEXPORT void JNICALL
Java_com_xiao_androidisland_liquidisland_renderer_LiquidGlassNativeBridge_nativeOnSurfaceCreated(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle
) {
    auto* renderer = reinterpret_cast<LiquidGlassGlRenderer*>(handle);
    if (renderer) renderer->onSurfaceCreated();
}

JNIEXPORT void JNICALL
Java_com_xiao_androidisland_liquidisland_renderer_LiquidGlassNativeBridge_nativeOnSurfaceChanged(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle,
    jint width,
    jint height
) {
    auto* renderer = reinterpret_cast<LiquidGlassGlRenderer*>(handle);
    if (renderer) renderer->onSurfaceChanged(width, height);
}

JNIEXPORT void JNICALL
Java_com_xiao_androidisland_liquidisland_renderer_LiquidGlassNativeBridge_nativeOnDrawFrame(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle
) {
    auto* renderer = reinterpret_cast<LiquidGlassGlRenderer*>(handle);
    if (renderer) renderer->onDrawFrame();
}

JNIEXPORT void JNICALL
Java_com_xiao_androidisland_liquidisland_renderer_LiquidGlassNativeBridge_nativeUpdateIslandState(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle,
    jfloat cx, jfloat cy, jfloat w, jfloat h, jfloat radius,
    jfloat bx, jfloat by, jfloat br, jfloat sminK,
    jfloat refHeight, jfloat refAmount, jfloat aberration,
    jfloat leftVelX, jfloat rightVelX,
    jfloat absorptionStr, jfloat blurStr
) {
    auto* renderer = reinterpret_cast<LiquidGlassGlRenderer*>(handle);
    if (renderer) {
        renderer->updateIslandState(
            cx, cy, w, h, radius,
            bx, by, br, sminK,
            refHeight, refAmount, aberration,
            leftVelX, rightVelX,
            absorptionStr, blurStr
        );
    }
}

JNIEXPORT void JNICALL
Java_com_xiao_androidisland_liquidisland_renderer_LiquidGlassNativeBridge_nativeUpdateTouchRipple(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle,
    jfloat tx,
    jfloat ty,
    jfloat progress
) {
    auto* renderer = reinterpret_cast<LiquidGlassGlRenderer*>(handle);
    if (renderer) renderer->updateTouchRipple(tx, ty, progress);
}

} // extern "C"
