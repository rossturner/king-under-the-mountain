attribute vec4 a_position;
attribute vec2 a_positionRelativeToLight;

uniform mat4 u_projTrans;
uniform vec3 u_lightPosition;
uniform vec3 u_lightColor;

varying vec2 v_texCoords;
varying vec2 v_positionRelativeToLight;

void main() {
    gl_Position = u_projTrans * a_position;
    v_positionRelativeToLight = a_positionRelativeToLight;
    v_texCoords = vec2(gl_Position.x, gl_Position.y);
}