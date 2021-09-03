#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_lowerLeftFlow;
varying float v_distanceFromLowerLeft;
varying vec2 v_upperLeftFlow;
varying float v_distanceFromUpperLeft;
varying vec2 v_upperRightFlow;
varying float v_distanceFromUpperRight;
varying vec2 v_lowerRightFlow;
varying float v_distanceFromLowerRight;
varying vec2 v_texCoords0;
varying vec2 v_texCoords1;
uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform float u_time;

vec4 calculateContribution(in vec2 flow, in float nearness) {
    nearness = (nearness * nearness) * 2.0; // enhance contribution near to vertex rather than linear scale
    vec2 texCoords0 = vec2(v_texCoords0.x, v_texCoords0.y); // Clone to avoid changing each time
    flow.x = -flow.x; // As glsl is upper-left origin

    flow *= u_time;

    vec4 wave_color = texture2D(u_texture1, fract(v_texCoords1 - flow));
    vec4 slower_wave_color = texture2D(u_texture1, fract(v_texCoords1 - (flow * 0.75)));

    texCoords0 = fract(texCoords0 + flow);

    float distortU = mix(texCoords0.x, wave_color.x, 0.1);
    float distortY = mix(texCoords0.y, slower_wave_color.y, 0.1);

    vec4 color_with_distortU = texture2D(u_texture0, fract(vec2(distortU, texCoords0.y)));
    vec4 color_with_distortY = texture2D(u_texture0, fract(vec2(texCoords0.x, distortY)));

    return vec4(mix(color_with_distortU, color_with_distortY, 0.5).rgb, nearness);
}

void main() {
    vec4 lowerLeftContribution = calculateContribution(v_lowerLeftFlow, clamp(1.0 - v_distanceFromLowerLeft, 0.0, 1.0));
    vec4 upperLeftContribution = calculateContribution(v_upperLeftFlow, clamp(1.0 - v_distanceFromUpperLeft, 0.0, 1.0));
    vec4 upperRightContribution = calculateContribution(v_upperRightFlow, clamp(1.0 - v_distanceFromUpperRight, 0.0, 1.0));
    vec4 lowerRightContribution = calculateContribution(v_lowerRightFlow, clamp(1.0 - v_distanceFromLowerRight, 0.0, 1.0));

    float totalContributions = lowerLeftContribution.a + upperLeftContribution.a + upperRightContribution.a + lowerRightContribution.a;

    gl_FragColor = vec4(
        ((lowerLeftContribution.rgb * lowerLeftContribution.a) +
        (upperLeftContribution.rgb * upperLeftContribution.a) +
        (upperRightContribution.rgb * upperRightContribution.a) +
        (lowerRightContribution.rgb * lowerRightContribution.a)) / totalContributions,
        v_color.a
    );
}