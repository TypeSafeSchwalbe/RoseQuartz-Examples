package rosequartz.example.triangle;

import rosequartz.Project;
import rosequartz.RoseQuartz;
import rosequartz.files.Resource;

import rosequartz.gfx.VertexArray;
import rosequartz.gfx.ShaderProgram;
import rosequartz.gfx.RenderTarget;

import rosequartz.ecb.ECB;
import rosequartz.gfx.GraphicsPipeline;

public class Main extends Project {

  @Override
  public void main() {
    // INIT
    // create VertexArray with triangle data
    VertexArray triangleArray = new VertexArray(2, 3) // Vertex looks like this: [vec2 (position), vec3 (color)]
        // we will just use NDC for positions directly, where (X) -1 is left and 1 right, and (Y) -1 is bottom and 1 is top.
        .vertex(  0,     0.5f,   1, 0, 0 ) // red vertex
        .vertex(  0.3f, -0.5f,   0, 1, 0 ) // green vertex
        .vertex( -0.3f, -0.5f,   0, 0, 1 ) // blue vertex
        .fragment( 0, 1, 2 ) // combine all 3 to a triangle (counter-clockwise, like listed above)
        .upload();
    // create ShaderProgram from source files in /res/shaders/vertex.glsl and /res/shaders/fragment.glsl
    ShaderProgram triangleProgram = new ShaderProgram(new Resource("shaders/vertex.glsl"), new Resource("shaders/fragment.glsl"))
        .select(); // use these shaders
    // FRAME
    ECB.add(new GraphicsPipeline(() -> { // add new GraphicsPipeline that runs the given lambda every frame
      // clear the screen (default RenderTarget)
      RenderTarget.getDefault().clearColor(1, 1, 1, 1);
      // render the triangle
      triangleArray.render();
    }));
  }

}
