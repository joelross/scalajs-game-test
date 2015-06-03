#ifdef GL_ES
  precision mediump float;
#endif

uniform vec3 ambientColor;
uniform vec3 diffuseColor;

varying vec3 varNormal;
varying vec3 varView;

void main(void) {
  gl_FragColor = vec4(ambientColor + diffuseColor * dot(normalize(varView), normalize(varNormal)), 1.0);
}
