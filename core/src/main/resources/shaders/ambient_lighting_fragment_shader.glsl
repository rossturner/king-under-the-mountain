#ifdef GL_ES
    precision mediump float;
#endif

uniform vec3 u_lightColor;
varying float v_luminosity;

void main() {
    gl_FragColor = vec4(u_lightColor, v_luminosity);
}
