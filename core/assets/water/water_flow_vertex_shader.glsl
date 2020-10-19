attribute vec4 a_position;

attribute vec4 a_color;
attribute vec2 a_lowerLeftFlow;
attribute float a_distanceFromLowerLeft;
attribute vec2 a_upperLeftFlow;
attribute float a_distanceFromUpperLeft;
attribute vec2 a_upperRightFlow;
attribute float a_distanceFromUpperRight;
attribute vec2 a_lowerRightFlow;
attribute float a_distanceFromLowerRight;
attribute vec2 a_texCoord0;
attribute vec2 a_texCoord1;

uniform mat4 u_projTrans;

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

void main() {
    v_color = a_color;
    v_lowerLeftFlow = a_lowerLeftFlow;
    v_distanceFromLowerLeft = a_distanceFromLowerLeft;
    v_upperLeftFlow = a_upperLeftFlow;
    v_distanceFromUpperLeft = a_distanceFromUpperLeft;
    v_upperRightFlow = a_upperRightFlow;
    v_distanceFromUpperRight = a_distanceFromUpperRight;
    v_lowerRightFlow = a_lowerRightFlow;
    v_distanceFromLowerRight = a_distanceFromLowerRight;
    v_texCoords0 = a_texCoord0;
    v_texCoords1 = a_texCoord1;
    gl_Position = u_projTrans * a_position;
}