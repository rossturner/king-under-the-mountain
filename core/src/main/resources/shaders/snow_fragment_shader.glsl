#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_snowAmount;

#define SNOW_DIR vec3(0.0, 1.0, 0.0)

#define SNOW_PROGRESS 0.8

void main() {

    vec4 normalColor = v_color * texture2D(u_texture, v_texCoords);
    vec3 normalisedVec = vec3 (
        (normalColor.r * 2.0) - 1.0,
        (normalColor.g * 2.0) - 1.0,
        (normalColor.b * 2.0) - 1.0
    );

    float lightIncidenceAmount = dot(SNOW_DIR, normalisedVec);

    float smoothed = smoothstep(1.0 - u_snowAmount, 1.3 - u_snowAmount, lightIncidenceAmount);

    // allow max to alpha 0.85 so we don't get pure white
    gl_FragColor = vec4(smoothed, smoothed, smoothed, min(0.85, min(smoothed, normalColor.a)));
}
