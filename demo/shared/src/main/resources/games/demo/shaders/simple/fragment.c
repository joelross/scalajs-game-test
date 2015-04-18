#ifdef GL_ES
  precision mediump float;
#endif

varying vec3 varColor;

void main(void) {
  gl_FragColor = vec4(varColor, 1.0);
}
