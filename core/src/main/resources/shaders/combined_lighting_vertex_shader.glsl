attribute vec4 a_position;

varying vec2 v_position;

void main() {
    v_position = vec2(a_position.x, a_position.y);
    gl_Position = a_position;
}