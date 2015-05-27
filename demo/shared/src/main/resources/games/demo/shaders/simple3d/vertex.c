uniform mat4 projection;
uniform mat4 modelView;
uniform mat3 normalModelView;

attribute vec3 position;
attribute vec3 normal;

varying vec3 varNormal;
varying vec3 varView;

void main(void) {
  vec4 pos = modelView * vec4(position, 1.0);
  gl_Position = projection * pos;
  varNormal = normalize(normalModelView * normal);
  varView = -pos.xyz;
}
