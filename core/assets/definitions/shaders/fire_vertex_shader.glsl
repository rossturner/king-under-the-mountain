attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
attribute float a_seed;

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_posCoords;
varying float v_seed;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    v_posCoords = a_position.xy;
    v_seed = a_seed;
    gl_Position = u_projTrans * a_position;
}