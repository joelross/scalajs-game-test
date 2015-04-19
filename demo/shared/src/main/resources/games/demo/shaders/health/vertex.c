attribute vec3 position;
attribute vec3 color;

varying vec3 varColor;

void main(void) {
  gl_Position = vec4(position, 1.0);
  varColor = color;
}
