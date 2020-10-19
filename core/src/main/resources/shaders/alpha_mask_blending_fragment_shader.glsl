#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords0;
varying vec2 v_texCoords1;
uniform sampler2D u_texture0;
uniform sampler2D u_texture1;

void main() {
    vec4 texColor = texture2D(u_texture0, v_texCoords0);
    vec4 maskColor = texture2D(u_texture1, v_texCoords1);
    gl_FragColor = vec4(
        vec3(v_color * texColor),
        maskColor.a
    );
}
