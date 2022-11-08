in vec2 fragment_tex_mapping;
in float fragment_diffuse;

out vec4 out_color;

uniform sampler2D TEXTURE_SAMPLER;
uniform float LIGHT_AMBIENT;

void main() {
	vec4 shaded_color = texture(TEXTURE_SAMPLER, fragment_tex_mapping) * ((1.0 - LIGHT_AMBIENT) * fragment_diffuse + LIGHT_AMBIENT);
	out_color = vec4(shaded_color.rgb, 1.0);
}
