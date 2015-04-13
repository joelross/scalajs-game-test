uniform mat4 projection;
uniform mat4 modelView;
uniform mat4 modelViewInvTr;

attribute vec3 position;
attribute vec3 normal;

varying vec3 varNormal;
varying vec3 varView;

void main(void) {
  vec4 pos = modelView * vec4(position, 1.0);
  gl_Position = projection * pos;
  varNormal = normalize((modelViewInvTr * vec4(normal, 1.0)).xyz);
  varView = normalize(-pos.xyz);
}
