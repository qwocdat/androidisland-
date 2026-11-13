#version 300 es
precision highp float;

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uElementTexture;
uniform vec2 uMotionVector;      // (vx, vy) in UV space proportional to Island edge velocity
uniform float uAbsorptionPhase;  // 0.0 = untouched, 0.0..0.5 = Approach/Contact stretch, 0.5..1.0 = Absorbed into Island
uniform float uIslandEdgeDir;    // +1.0 if Island is to the right (left status bar icons), -1.0 if Island is to the left

void main() {
    // Liquid Necking & Stretch toward the approaching Dynamic Island edge
    vec2 centered = vUv - vec2(0.5);
    float stretchX = 1.0 + 0.45 * uAbsorptionPhase;
    float pinchY = max(0.25, 1.0 - 0.55 * uAbsorptionPhase * (0.5 + 0.5 * uIslandEdgeDir * (vUv.x - 0.5)));
    vec2 warpedUv = vec2(
        0.5 + (centered.x - 0.18 * uAbsorptionPhase * uIslandEdgeDir) / stretchX,
        0.5 + centered.y / pinchY
    );

    // 9-tap Gaussian-weighted directional velocity motion blur along uMotionVector
    const int TAPS = 9;
    float weights[9] = float[9](
        0.05, 0.09, 0.12, 0.15, 0.18, 0.15, 0.12, 0.09, 0.05
    );
    vec4 accum = vec4(0.0);
    for (int i = 0; i < TAPS; ++i) {
        float offsetFraction = (float(i) - 4.0) / 4.0;
        vec2 sampleUv = clamp(warpedUv + uMotionVector * offsetFraction, vec2(0.0), vec2(1.0));
        accum += texture(uElementTexture, sampleUv) * weights[i];
    }

    // Coalescence into the liquid boundary at the end of Phase C (only after full physical contact & compression)
    float liquidMerge = 1.0 - smoothstep(0.78, 1.0, uAbsorptionPhase);
    fragColor = vec4(accum.rgb, accum.a * liquidMerge);
}
