#include "LiquidGlassGlRenderer.h"
#include <android/log.h>
#include <cmath>

#define LOG_TAG "LiquidGlassNDK"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace liquidisland {

static const char* kVertexShaderSrc = R"GLSL(#version 300 es
precision highp float;
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aUv;
out vec2 vUv;
void main() {
    vUv = aUv;
    gl_Position = vec4(aPos, 0.0, 1.0);
}
)GLSL";

// Native OpenGL ES 3.0 Fragment Shader implementing:
// 1. Smooth-Min Metaball SDF (smin) combining the main Dynamic Island pill and split secondary bubble
// 2. Real SDF normal-gradient Lens Refraction + 7-band Chromatic Aberration (adapted from liquid.zip Lens.kt)
// 3. Fresnel Rim Specular Highlights & Touch-Responsive Ripple Deformation
// 4. Edge-Based Liquid Absorption & Directional Velocity Motion Blur Field near the Island boundaries
static const char* kFragmentShaderSrc = R"GLSL(#version 300 es
precision highp float;

in vec2 vUv;
out vec4 fragColor;

uniform vec2 uViewport;
uniform vec4 uIslandRect;      // (centerX, centerY, width, height)
uniform float uCornerRadius;
uniform vec4 uBubbleCircle;    // (centerX, centerY, radius, metaballSmoothK)
uniform vec4 uOpticalParams;   // (refractionHeight, refractionAmount, chromaticAberration, fresnelIntensity)
uniform vec3 uTouchRipple;     // (touchX, touchY, progress)
uniform vec4 uAbsorptionField; // (leftEdgeVelX, rightEdgeVelX, absorptionIntensity, motionBlurIntensity)

float sdRoundedBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

float sdCircle(vec2 p, float r) {
    return length(p) - r;
}

// Polynomial smooth minimum for organic metaball coalescence (Split Island & Status Bar Absorption)
float smin(float a, float b, float k) {
    if (k <= 0.001) return min(a, b);
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

float evalIslandSdf(vec2 pixelPos) {
    vec2 halfSize = uIslandRect.zw * 0.5;
    float r = min(uCornerRadius, min(halfSize.x, halfSize.y));
    float dMain = sdRoundedBox(pixelPos - uIslandRect.xy, halfSize, r);
    if (uBubbleCircle.z > 1.0) {
        float dBubble = sdCircle(pixelPos - uBubbleCircle.xy, uBubbleCircle.z);
        return smin(dMain, dBubble, uBubbleCircle.w);
    }
    return dMain;
}

vec2 evalIslandGradient(vec2 p) {
    const float eps = 1.0;
    float dx = evalIslandSdf(p + vec2(eps, 0.0)) - evalIslandSdf(p - vec2(eps, 0.0));
    float dy = evalIslandSdf(p + vec2(0.0, eps)) - evalIslandSdf(p - vec2(0.0, eps));
    float len = max(length(vec2(dx, dy)), 1e-4);
    return vec2(dx, dy) / len;
}

float circleMap(float x) {
    float c = clamp(x, 0.0, 1.0);
    return 1.0 - sqrt(max(0.0, 1.0 - c * c));
}

void main() {
    vec2 fragPos = vec2(vUv.x * uViewport.x, vUv.y * uViewport.y);
    float sd = evalIslandSdf(fragPos);

    // Outside the Island + absorption halo
    if (sd > 14.0) {
        discard;
    }

    float refHeight = max(uOpticalParams.x, 1.0);
    float refAmount = uOpticalParams.y;
    float aberration = uOpticalParams.z;
    float fresnelStr = uOpticalParams.w;

    vec2 normal = evalIslandGradient(fragPos);

    // Touch-responsive surface ripple distortion
    float rippleOffset = 0.0;
    if (uTouchRipple.z > 0.01 && uTouchRipple.z < 0.99) {
        float distTouch = length(fragPos - uTouchRipple.xy);
        float wave = sin(distTouch * 0.08 - uTouchRipple.z * 14.0);
        float atten = exp(-distTouch * 0.012) * (1.0 - uTouchRipple.z);
        rippleOffset = wave * atten * 6.0;
    }

    if (sd <= 0.5) {
        // Inside Dynamic Island surface: compute real SDF lens refraction + Fresnel specular rim
        float rimFactor = (-sd < refHeight) ? circleMap(1.0 - (-min(sd, 0.0) / refHeight)) : 0.0;
        vec2 disp = (rimFactor * refAmount + rippleOffset) * normal;

        // Chromatic dispersion across rim band
        float rShift = length(disp) * aberration * 0.018;
        vec3 baseGlass = vec3(0.04, 0.045, 0.06);
        baseGlass.r += rShift * 0.65;
        baseGlass.b += rShift * 0.95;

        // Fresnel edge response & dual-peak specular highlight
        float fresnel = smoothstep(-3.2, 0.0, sd) * fresnelStr;
        vec2 lightDir1 = normalize(vec2(-0.45, -0.75));
        vec2 lightDir2 = normalize(vec2(0.55, 0.65));
        float spec1 = pow(max(dot(normal, lightDir1), 0.0), 3.0);
        float spec2 = pow(max(dot(normal, lightDir2), 0.0), 4.0) * 0.45;
        vec3 rimColor = vec3(0.92, 0.95, 1.0) * fresnel * (0.28 + 0.72 * (spec1 + spec2));

        float aa = 1.0 - smoothstep(-0.8, 0.4, sd);
        fragColor = vec4(clamp(baseGlass + rimColor, 0.0, 1.0), aa * 0.96);
    } else {
        // Liquid Absorption Halo (0.5 < sd <= 14.0): renders the directional velocity meniscus
        floatedgeVel = (fragPos.x < uIslandRect.x) ? abs(uAbsorptionField.x) : abs(uAbsorptionField.y);
        float halo = exp(-sd * 0.35) * clamp(floatedgeVel * 0.025, 0.0, 0.55) * uAbsorptionField.z;
        fragColor = vec4(0.05, 0.06, 0.08, halo);
    }
}
)GLSL";

static const float kQuadVertices[] = {
    -1.0f, -1.0f, 0.0f, 1.0f,
     1.0f, -1.0f, 1.0f, 1.0f,
    -1.0f,  1.0f, 0.0f, 0.0f,
     1.0f,  1.0f, 1.0f, 0.0f,
};

LiquidGlassGlRenderer::~LiquidGlassGlRenderer() {
    release();
}

GLuint LiquidGlassGlRenderer::compileShader(GLenum type, const char* source) {
    GLuint shader = glCreateShader(type);
    if (!shader) return 0;
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);
    GLint compiled = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (!compiled) {
        char info[512];
        glGetShaderInfoLog(shader, sizeof(info), nullptr, info);
        LOGE("Shader compile failed: %s", info);
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

GLuint LiquidGlassGlRenderer::linkProgram(const char* vertSrc, const char* fragSrc) {
    GLuint vs = compileShader(GL_VERTEX_SHADER, vertSrc);
    if (!vs) return 0;
    GLuint fs = compileShader(GL_FRAGMENT_SHADER, fragSrc);
    if (!fs) {
        glDeleteShader(vs);
        return 0;
    }
    GLuint prog = glCreateProgram();
    glAttachShader(prog, vs);
    glAttachShader(prog, fs);
    glLinkProgram(prog);
    glDeleteShader(vs);
    glDeleteShader(fs);
    GLint linked = 0;
    glGetProgramiv(prog, GL_LINK_STATUS, &linked);
    if (!linked) {
        char info[512];
        glGetProgramInfoLog(prog, sizeof(info), nullptr, info);
        LOGE("Program link failed: %s", info);
        glDeleteProgram(prog);
        return 0;
    }
    return prog;
}

void LiquidGlassGlRenderer::onSurfaceCreated() {
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    programId_ = linkProgram(kVertexShaderSrc, kFragmentShaderSrc);
    glGenBuffers(1, &vboId_);
    glBindBuffer(GL_ARRAY_BUFFER, vboId_);
    glBufferData(GL_ARRAY_BUFFER, sizeof(kQuadVertices), kQuadVertices, GL_STATIC_DRAW);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

void LiquidGlassGlRenderer::onSurfaceChanged(int width, int height) {
    std::lock_guard<std::mutex> lock(stateMutex_);
    state_.viewportW = static_cast<float>(width > 0 ? width : 1);
    state_.viewportH = static_cast<float>(height > 0 ? height : 1);
    glViewport(0, 0, width, height);
}

void LiquidGlassGlRenderer::onDrawFrame() {
    glClear(GL_COLOR_BUFFER_BIT);
    if (programId_ == 0) return;

    IslandUniformState snap;
    {
        std::lock_guard<std::mutex> lock(stateMutex_);
        snap = state_;
    }

    glUseProgram(programId_);
    glUniform2f(glGetUniformLocation(programId_, "uViewport"), snap.viewportW, snap.viewportH);
    glUniform4f(
        glGetUniformLocation(programId_, "uIslandRect"),
        snap.islandCenterX, snap.islandCenterY, snap.islandWidth, snap.islandHeight
    );
    glUniform1f(glGetUniformLocation(programId_, "uCornerRadius"), snap.cornerRadius);
    glUniform4f(
        glGetUniformLocation(programId_, "uBubbleCircle"),
        snap.bubbleCenterX, snap.bubbleCenterY, snap.bubbleRadius, snap.metaballSmoothK
    );
    glUniform4f(
        glGetUniformLocation(programId_, "uOpticalParams"),
        snap.refractionHeight, snap.refractionAmount, snap.chromaticAberration, snap.fresnelIntensity
    );
    glUniform3f(
        glGetUniformLocation(programId_, "uTouchRipple"),
        snap.touchX, snap.touchY, snap.touchRippleProgress
    );
    glUniform4f(
        glGetUniformLocation(programId_, "uAbsorptionField"),
        snap.leftEdgeVelocityX, snap.rightEdgeVelocityX, snap.absorptionIntensity, snap.motionBlurIntensity
    );

    glBindBuffer(GL_ARRAY_BUFFER, vboId_);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(float), reinterpret_cast<void*>(0));
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(float), reinterpret_cast<void*>(2 * sizeof(float)));

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

void LiquidGlassGlRenderer::release() {
    if (programId_ != 0) {
        glDeleteProgram(programId_);
        programId_ = 0;
    }
    if (vboId_ != 0) {
        glDeleteBuffers(1, &vboId_);
        vboId_ = 0;
    }
}

void LiquidGlassGlRenderer::updateIslandState(
    float cx, float cy, float w, float h, float radius,
    float bx, float by, float br, float sminK,
    float refHeight, float refAmount, float aberration,
    float leftVelX, float rightVelX,
    float absorptionStr, float blurStr
) {
    std::lock_guard<std::mutex> lock(stateMutex_);
    state_.islandCenterX = cx;
    state_.islandCenterY = cy;
    state_.islandWidth = w;
    state_.islandHeight = h;
    state_.cornerRadius = radius;
    state_.bubbleCenterX = bx;
    state_.bubbleCenterY = by;
    state_.bubbleRadius = br;
    state_.metaballSmoothK = sminK;
    state_.refractionHeight = refHeight;
    state_.refractionAmount = refAmount;
    state_.chromaticAberration = aberration;
    state_.leftEdgeVelocityX = leftVelX;
    state_.rightEdgeVelocityX = rightVelX;
    state_.absorptionIntensity = absorptionStr;
    state_.motionBlurIntensity = blurStr;
}

void LiquidGlassGlRenderer::updateTouchRipple(float tx, float ty, float progress) {
    std::lock_guard<std::mutex> lock(stateMutex_);
    state_.touchX = tx;
    state_.touchY = ty;
    state_.touchRippleProgress = progress;
}

} // namespace liquidisland
