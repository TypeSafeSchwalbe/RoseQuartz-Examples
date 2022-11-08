package rosequartz.example.model;

import rosequartz.Project;
import rosequartz.RoseQuartz;
import rosequartz.files.Resource;

import rosequartz.gfx.*;

import rosequartz.ecb.ECB;
import rosequartz.math.Vec3;

public class Main extends Project {

	@Override
	public void main() {
		// INIT
		// load the Model
		Model model = new Model(
				new Resource("model/car.obj"),
				new VertexBuilder<>(Model.VertexStructure.VERTEX_POSITION, Model.VertexStructure.NORMAL_VECTOR, Model.VertexStructure.TEXTURE_MAPPING)
		);
		// load the Model texture
		Texture texture = new Texture(new Resource("model/car.png"));
		// create the camera
		PerspectiveCamera camera = new PerspectiveCamera(
				new CameraConfiguration()
						.setPosition(0, 10, 10)
						.setLookAt(0, 0, 0, 0, 1, 0)
		);
		// load the Shader
		ShaderProgram modelShader = new ShaderProgram(new Resource("shaders/vertex.glsl"), new Resource("shaders/fragment.glsl"))
				// set uniform values
				.setUniformTexture("TEXTURE_SAMPLER", texture)
				.setUniformMatrix4("PROJECTION_VIEW_MATRIX", camera.getProjectionViewMatrix())
				// set light position and ambient strength
				.setUniformVec("LIGHT_POSITION", new Vec3(2.5f, 1, 2.5f))
				.setUniformFloat("LIGHT_STRENGTH", 10)
				.setUniformFloat("LIGHT_AMBIENT", 0.1f)
				// use the shader
				.select();
		// create a ModelInstance
		ModelInstance modelInstance = new ModelInstance();
		// enable depth testing
		DepthTestingManager.get().setEnabled(true);
		// FRAME
		ECB.add(new GraphicsPipeline(() -> { // add new GraphicsPipeline that runs the given lambda every frame
			// clear the screen (default RenderTarget)
			RenderTarget.getDefault().clearColor(0, 0, 0, 1)
					.clearDepth(1);
			// rotate the model and upload the matrix
			modelInstance.rotateDegrees(0, (float) RoseQuartz.deltaTime() * 90f, 0);
			modelShader.setUniformMatrix4("MODEL_MATRIX", modelInstance.getModelMatrix());
			// render the model
			model.getVertexArray().render();
		}));
	}

}

