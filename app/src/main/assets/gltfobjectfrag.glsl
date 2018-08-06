precision mediump float;

// Uniforms
uniform int u_UseColors;

// Sampler
uniform sampler2D u_Texture_0;
uniform sampler2D u_Texture_1;
uniform sampler2D u_Texture_2;
uniform sampler2D u_Texture_3;

// Varyings
varying vec2 uv_0;
varying vec2 uv_1;
varying vec2 uv_2;
varying vec2 uv_3;

varying vec4 col_0;


void main() {
    vec4 fragColor;
    if(u_UseColors == 0) {
        fragColor = texture2D(u_Texture_0, uv_0);
    } else {
        fragColor = col_0;
    }

    gl_FragColor = fragColor;
}
