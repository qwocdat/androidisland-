#pragma once

#include <GLES3/gl3.h>
#include <mutex>

namespace liquidisland {

struct IslandUniformState {
    float viewportW = 1080.0f;
    float viewportH = 2400.0f;
    // Main Island capsule geometry in pixels
    float islandCenterX = 540.0f;
    float islandCenterY = 80.0f;
    float islandWidth = 360.0f;
    float islandHeight = 96.0f;
    float cornerRadius = 48.0f;
    // Detached secondary bubble (for Split Dual Island state: 00:09, 00:25, 00:49)
    float bubbleCenterX = 540.0f;
    float bubbleCenterY = 80.0f;
    float bubbleRadius = 0.0f;
    float metaballSmoothK = 22.0f;
    // Optical parameters (Liquid Glass refraction, dispersion, Fresnel, touch ripple)
    float refractionHeight = 36.0f;
    float refractionAmount = 32.0f;
    float chromaticAberration = 0.22f;
    float fresnelIntensity = 0.78f;
    float touchX = -1000.0f;
    float touchY = -1000.0f;
    float touchRippleProgress = 0.0f;
    // Edge velocity for directional motion blur & liquid absorption
    float leftEdgeVelocityX = 0.0f;
    float rightEdgeVelocityX = 0.0f;
    float absorptionIntensity = 1.0f;
    float motionBlurIntensity = 1.0f;
};

class LiquidGlassGlRenderer {
public:
    LiquidGlassGlRenderer() = default;
    ~LiquidGlassGlRenderer();

    void onSurfaceCreated();
    void onSurfaceChanged(int width, int height);
    void onDrawFrame();
    void release();

    void updateIslandState(
        float cx, float cy, float w, float h, float radius,
        float bx, float by, float br, float sminK,
        float refHeight, float refAmount, float aberration,
        float leftVelX, float rightVelX,
        float absorptionStr, float blurStr
    );

    void updateTouchRipple(float tx, float ty, float progress);

private:
    GLuint compileShader(GLenum type, const char* source);
    GLuint linkProgram(const char* vertSrc, const char* fragSrc);

    GLuint programId_ = 0;
    GLuint vboId_ = 0;
    std::mutex stateMutex_;
    IslandUniformState state_{};
};

} // namespace liquidisland
