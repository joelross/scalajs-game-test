#ifdef GL_ES
  precision mediump float;
#endif

varying vec3 varNormal;
varying vec3 varView;

const vec3 diffuseColor = vec3(0.5, 0.5, 0.5);

void main(void) {
  gl_FragColor = vec4(diffuseColor * dot(normalize(varView), normalize(varNormal)), 1.0);
}
