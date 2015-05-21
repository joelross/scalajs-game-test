uniform float scaleX;
uniform float scaleY;

uniform mat3 transform;

attribute vec2 position;

void main(void) {
  vec2 transformed = (transform * vec3(position, 1.0)).xy;
  gl_Position = vec4(transformed.x * scaleX, transformed.y * scaleY, 0.0, 1.0);
}
