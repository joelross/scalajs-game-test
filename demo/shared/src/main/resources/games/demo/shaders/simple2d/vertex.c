uniform float scaleX;
uniform float scaleY;

attribute vec2 position;

void main(void) {
  gl_Position = vec4(position.x * scaleX, position.y * scaleY, 0.0, 1.0);
}
