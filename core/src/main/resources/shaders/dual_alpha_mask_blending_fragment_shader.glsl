#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords0;
varying vec2 v_texCoords1;
varying vec2 v_texCoords2;
uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform sampler2D u_texture2;

void main() {
    vec4 texColor = texture2D(u_texture0, v_texCoords0);
    vec4 mask1Color = texture2D(u_texture1, v_texCoords1);
    vec4 mask2Color = texture2D(u_texture2, v_texCoords2);
    gl_FragColor = vec4(
        vec3(v_color * texColor),
        min(mask1Color.a, mask2Color.a)
    );
}
