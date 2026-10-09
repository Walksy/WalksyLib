#version 150
in vec3 Position;
out vec2 texCoord;
void main() {
#ifdef WALKSYLIB_VULKANMOD
    texCoord = vec2(Position.x, 1.0 - Position.y);
#else
    texCoord = Position.xy;
#endif
    vec2 ndc = Position.xy * 2.0 - 1.0;
    gl_Position = vec4(ndc, 0.0, 1.0);
}
