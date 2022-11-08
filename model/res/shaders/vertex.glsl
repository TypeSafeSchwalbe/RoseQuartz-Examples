layout(location=0) in vec3 in_position;
layout(location=1) in vec3 in_normal;
layout(location=2) in vec2 in_tex_mapping;

out vec2 fragment_tex_mapping;
out float fragment_diffuse;

uniform mat4 MODEL_MATRIX;
uniform mat4 PROJECTION_VIEW_MATRIX;

uniform vec3 LIGHT_POSITION;
uniform float LIGHT_STRENGTH;

void main() {
	vec4 transformed_position = MODEL_MATRIX * vec4(in_position, 1.0);
	vec4 transformed_normal = MODEL_MATRIX * vec4(in_normal, 1.0);
	gl_Position = PROJECTION_VIEW_MATRIX * transformed_position;
	fragment_tex_mapping = in_tex_mapping;
	fragment_diffuse = getLightDiffuse(LIGHT_POSITION, LIGHT_STRENGTH, transformed_position.xyz, transformed_normal.xyz); // function included in every shader
}
