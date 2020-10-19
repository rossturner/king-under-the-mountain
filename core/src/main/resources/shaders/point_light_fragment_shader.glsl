#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_texCoords;
varying vec2 v_positionRelativeToLight;
uniform sampler2D u_texture;
uniform vec3 u_lightPosition;
uniform vec3 u_lightColor;

void main() {
    vec2 correctedCoords = vec2(
        (v_texCoords.x + 1.0) / 2.0,
        (v_texCoords.y + 1.0) / 2.0
    );

    vec4 normalVec = texture2D(u_texture, correctedCoords);
    // TODO let normal 1.0 - alpha = height from 0 to 1
    vec3 normalisedVec = vec3 (
        (normalVec.r * 2.0) - 1.0,
        (normalVec.g * 2.0) - 1.0,
        (normalVec.b * 2.0) - 1.0
    );
    normalisedVec = normalize(normalisedVec);

    float lightHeight = 0.2; // TODO pass this in as a uniform
    vec3 lightDir = vec3 (
        -v_positionRelativeToLight.x,
        -v_positionRelativeToLight.y,
        lightHeight
    );
    lightDir = normalize(lightDir);

    float lightIncidenceAmount = dot(lightDir, normalisedVec);

    float distanceToLight = length(v_positionRelativeToLight);
    float falloffVarA = 0.1; // FIXME Make these constants
    float falloffVarB = 1.0;
    float attenuation = 1.0 / (1.0 + (falloffVarA*distanceToLight) + (falloffVarB*distanceToLight*distanceToLight));
    gl_FragColor = vec4(u_lightColor, (1.0 - distanceToLight) * attenuation * lightIncidenceAmount);
}
