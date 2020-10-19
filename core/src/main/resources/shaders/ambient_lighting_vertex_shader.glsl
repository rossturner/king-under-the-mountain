attribute vec4 a_position;
attribute vec4 a_color;
attribute float a_luminosity;

uniform mat4 u_projTrans;
uniform vec3 u_lightColor;

varying float v_luminosity;

void main() {
    v_luminosity = a_luminosity;
    gl_Position = u_projTrans * a_position;
}