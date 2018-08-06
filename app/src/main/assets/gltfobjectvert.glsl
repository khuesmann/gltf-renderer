// Uniforms
uniform mat4 u_ModelViewProjection;

// Attributes
attribute vec3 a_Position;
attribute vec3 a_Normal;
attribute vec4 a_Tanget;
attribute vec2 a_Texcoord_0; // color texture
attribute vec2 a_Texcoord_1; // normal map
attribute vec2 a_Texcoord_2; // occlusion map
attribute vec2 a_Texcoord_3; // environment map
attribute vec4 a_color_0;
//attribute vec4 a_joints_0;
//attribute vec4 a_weights_0;

// Varyings
varying vec2 uv_0;
varying vec2 uv_1;
varying vec2 uv_2;
varying vec2 uv_3;

varying vec4 col_0;


void main() {

    // By-pass texture coords.
    uv_0 = a_Texcoord_0;
    uv_1 = a_Texcoord_1;
    uv_2 = a_Texcoord_2;
    uv_3 = a_Texcoord_3;

    // Calculate vertex position in world space.
    gl_Position = u_ModelViewProjection * vec4(a_Position, 1);
}
