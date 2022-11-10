# Guide to writing a RoseQuartz Engine Runtime
This is a guide that will try to explain how you can write your own runtime for   
the RoseQuartz game engine.

### Desktop build structure
When a project is compiled (on desktop), this is how  is structured:
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│/ (project build root)                                                           │
│                                                                                 │
│ ┌─────────────────────────────────────────────────────────────────────────────┐ │
│ │/libs                                                                        │ │
│ │                                                                             │ │
│ │ ┌───────────────────────┐ calls ┌──────────────────────────┐ ┌────────────┐ │ │
│ │ │Other project          ◄───────┤Project code              │ │Engine      │ │ │
│ │ │dependencies (*.jar)   │       │({project-name}.jar)      │ │Runtime     │ │ │
│ │ └───────────────────────┘       │                          │ │dependencies│ │ │
│ │                                 │                          │ │(*.jar)     │ │ │
│ │ ┌───────────────────────┐ calls │                          │ │            │ │ │
│ │ │Engine API (base, fx)  ◄───────┤                          │ │            │ │ │
│ │ │(rosequartz-api-{}.jar)│       │                          │ │            │ │ │
│ │ │ ┌─────────────────┐   │       │                          │ │            │ │ │
│ │ │ │Native interfaces│   │       │                          │ │            │ │ │
│ │ │ └──┬──────────────┘   │       │                          │ │            │ │ │
│ │ │    │                  ├───────►                          │ │            │ │ │
│ │ └────┼───────▲──────────┘ runs  └─▲────────────────────────┘ └─▲──────────┘ │ │
│ │      │       │          pipelines │                            │            │ │
│ └──────┼───────┼────────────────────┼────────────────────────────┼────────────┘ │
│        │       │registers           │calls main                  │calls         │
│        │       │implementations,    │method                      │              │
│        │calls  │calls ECB frames    │                            │              │
│ ┌──────┼───────┴────────────────────┴────────────────────────────┴────────────┐ │
│ │Engine│Runtime (runtime.jar)                                                 │ │
│ │ ┌────▼──────────────┐                                                       │ │
│ │ │Implementations for│                                                       │ │
│ │ │Native interfaces  │                                                       │ │
│ │ └───────────────────┘                                                       │ │
│ │                                                                             │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```
As you might see, the runtime is what actually *does* all the things the project requests.   
Because the runtime registers its own implementation of all features and then starts the project,
the engine runtime can be swapped for every target platform. It only needs to implement   
a set of features that the project can call. This means that the project code itself   
is independent from the platform it is run on.

We want to write our own implementation for our runtime, so we need to implement those interfaces,
call the project's main method and tell the ECB to call all pipelines every frame.  
There are also some other smaller details, so let's go trough this step by step.  
If you want to implement only the headless part of the API (only `rosequartz-api-base`, like for a server runtime),   
ignore all subpoints ending in "(api-fx)".

# Native implementations
[`rosequartz.Natives`](https://rosequartzdocs.netlify.app/rosequartz/natives) (rosequartz-api-base.jar) and [`rosequartz.NativesFX`](https://rosequartzdocs.netlify.app/rosequartz/nativesfx) (rosequartz-api-fx.jar)   
both provide interfaces (nested sub-classes) for all implementations they depend on.   
You will need to implement every interface they provide (don't implement NativesFX if you want to go headless)   
and - if their "constructor"-interface has more than one method - also implement their constructor in a nested   
subclass. We will come back to these later. This might look something like this (for example, a texture, which has multiple constructors):  
```java
package foo.bar;

import rosequartz.NativesFX;
import rosequartz.files.Resource;

public class Texture implements NativesFX.Texture {

    public static class Constructor implements NativesFX.Texture.Constructor {
        
        @Override
        public NativesFX.Texture construct(Resource r) { return new Texture(r); }
        
        @Override
        public NativesFX.Texture construct(int width, int height) { return new Texture(width, height); }
        
    }
    
    public Texture(Resource r) { ... }
    
    public Texture(int width, int height) { ... }
    
    @Override
    public void setMinFunction(rosequartz.gfx.Texture.ResizeFunction func) { ... }

    @Override
    public void setMagFunction(rosequartz.gfx.Texture.ResizeFunction func) { ... }

    @Override
    public int getWidth() { ... }

    @Override
    public int getHeight() { ... }

}
```
A class extending an interface that only has one way to construct it   
(like an [`rosequartz.NativesFX.AudioSource`](https://rosequartzdocs.netlify.app/rosequartz/nativesfx.audiosource)) only needs to have it's constructor match the only   
method in the constructor-sub-interface (so in this case, only a constructor with no parameters).

Remember that you probably also want to destroy your texture when it gets garbage collected.   
When the object gets garbage collected, revive it by adding it to a list of objects.   
At the end of the next frame, iterate over that list and destroy their OpenGL counterparts.  
(OpenGL just servers as an example here, consider this for all thread-based APIs that use  
`int` or `long` pointers instead of objects (or require you to destroy them), like OpenAL or OpenGL (lwjgl)).  

# Setup
First, let's talk about what needs to be done in the setting up phase of execution.   
You will first want to initialize everything the runtime needs to initialize, like (for example) OpenGL or OpenAL.   

### Registering native implementations to the engine API  (api-base)
Next you will need to register your implementations to the engine API.
This will require you to call something like this:   
(replace `/*<?????>*/` with the name of a class that implements said interface)
```java
import rosequartz.Natives;
import rosequartz.NativesFX; // ignore for headless

// base registrations
Natives._addConstructor((Natives.RoseQuartz.Constructor) /*<Natives.RoseQuartz>*/::new);
Natives._addConstructor((Natives.Resource.Constructor) /*<Natives.Resource>*/::new);
Natives._addConstructor((Natives.FileManager.Constructor) /*<Natives.FileManager>*/::new);
Natives._addConstructor(new /*<Natives.TCPSocket.Constructor>*/());
Natives._addConstructor(new /*<Natives.UDPSocket.Constructor>*/());
Natives._addConstructor((Natives.WebSocket.Constructor) /*<Natives.WebSocket>*/::new);
Natives._addConstructor((Natives.ObjectSerializer.Constructor) /*<Natives.ObjectSerializer>*/::new);
// fx (ignore for headless)
Natives._addConstructor((NativesFX.RoseQuartzFX.Constructor) /*<NativesFX.RoseQuartzFX>*/::new);
Natives._addConstructor((NativesFX.InputManager.Constructor) /*<NativesFX.InputManager>*/::new);
Natives._addConstructor((NativesFX.Audio.Constructor) /*<NativesFX.Audio>*/::new);
Natives._addConstructor((NativesFX.AudioSource.Constructor) /*<NativesFX.AudioSource>*/::new);
Natives._addConstructor((NativesFX.AudioListener.Constructor) /*<NativesFX.AudioListener>*/::new);
Natives._addConstructor(new /*<NativesFX.ShaderProgram.Constructor>*/());
Natives._addConstructor(new /*<NativesFX.Texture.Constructor>*/());
Natives._addConstructor((NativesFX.VertexArray.Constructor) /*<NativesFX.VertexArray>*/::new);
Natives._addConstructor((NativesFX.RenderTarget.Constructor) /*<NativesFX.RenderTarget>*/::new);
Natives._addConstructor((NativesFX.DepthTestingManager.Constructor) /*<NativesFX.DepthTestingManager>*/::new);
Natives._addConstructor((NativesFX.ConsoleManager.Constructor) /*<NativesFX.ConsoleManager>*/::new);
Natives._addConstructor((NativesFX.FileRequestManager.Constructor) /*<NativesFX.FileRequestManager>*/::new);
// finalize registration
Natives._freeze();
```

### Initializing engine API graphics (api-fx)
If you want to implement graphics, you will also have to call these methods directly after:
```java
import rosequartz.gfx.Graphics;

Graphics._setMainThread(); // set this thread to be the main thread
Graphics._initializeStatics(); // create static graphics objects for utility methods
```   

### Initialize needed API objects (api-fx)
Next you'll want to initialize API objects your runtime might want to use, like a RenderTarget or shaders.   
You can already start by creating a [`rosequartz.gfx.RenderTarget`](https://rosequartzdocs.netlify.app/rosequartz/gfx/rendertarget) the size of the screen, and targeting it.   

### Calling the project's main method (api-base)
The next step is to call the project's main method. For this, you will need to read the project   
configuration file and instanciate the main class defined in said file.   
After instantiating the class, cast it to a [`rosequartz.Project`](https://rosequartzdocs.netlify.app/rosequartz/project) call the [`main()`](https://rosequartzdocs.netlify.app/rosequartz/project#main())-method on said instance.  
I recommend to keep the object somewhere to stop it from being garbage collected.   

### Finalizing setup of engine API graphics (api-fx)
Again, if you want to implement graphics, you will also want to call these methods before ending the setup:
```java
import rosequartz.gfx.Graphics;

graphicsPipelineIdentifier = Graphics._requireGraphicsPipeline();
```
This will make the API require graphics calls to come from a [`rosequartz.gfx.GraphicsPipeline`](https://rosequartzdocs.netlify.app/rosequartz/gfx/graphicspipeline) from now on.   
We will come back to `graphicsPipelineIdentifier` later, just store it in a variable that you can use later for now.   

### Calling the gameloop (api-base)

To finally finish the setup, start calling your gameloop. You might want to consider calling [`rosequartz.ecb.ECB.getPipelineCount`](https://rosequartzdocs.netlify.app/rosequartz/ecb/ecb#getPipelineCount())   
and only starting the gameloop if it does not return 0, meaning there are actually pipelines to execute.   

# Gameloop

### Calling graphics-related API objects from outside of a GraphicsPipeline (api-fx)
There are 2 ways to call graphics-related methods   
without a [`rosequartz.gfx.NotOnGraphicsThreadException`](https://rosequartzdocs.netlify.app/rosequartz/gfx/notongraphicsthreadexception) being thrown.   
The first way is to call the method from a [`rosequartz.gfx.GraphicsPipeline`](https://rosequartzdocs.netlify.app/rosequartz/gfx/graphicspipeline).   
The second is to use the `graphicsPipelineIdentifier` we just got at the end of the setup.   
We can set the flag storing if we are in a GraphicsPipeline manually by calling   
[`rosequartz.gfx.Graphics._setInGraphicsPipeline`](https://rosequartzdocs.netlify.app/rosequartz/gfx/graphics#_setInGraphicsPipeline(java.lang.Object,boolean)). This allows us to make calls to API objects   
that would normally throw a NotOnGraphicsThreadException from outside of a GraphicsPipeline.

### Calling ECB pipelines (api-base)
You will need to call all pipelines using the [`rosequartz.ecb.ECB._runPipelines`](https://rosequartzdocs.netlify.app/rosequartz/ecb/ecb#_runPipelines())-method.   
Do this after calculating all values for that frame, like deltaTime (or user input, if not headless).

### Default RenderTarget (api-fx)
If you noticed, the [`rosequartz.NativesFX.RoseQuartzFX.getDefaultRenderTarget`](https://rosequartzdocs.netlify.app/rosequartz/nativesfx.rosequartzfx#getDefaultRenderTarget())-method wants you to return a 'default RenderTarget'.   
You will need to create a RenderTarget in the setup of your runtime and resize it when the size of the screen changes.   
Next you have to return this RenderTarget when that method is called.   
You will also have to render this RenderTarget to the final frame buffer, which can be done something like this:
```java
import rosequartz.gfx.Graphics;
import rosequartz.gfx.RenderTarget;

Graphics._setInGraphicsPipeline(graphicsPipelineIdentifier, true); // we want to call graphics API calls
RenderTarget userRenderTarget = RenderTarget.getCurrent(); // get the current render target
/* target the default frame buffer here (replace this comment)
   in OpenGL, this would mean:
       glBindFramebuffer(GL_FRAMEBUFFER, 0);
*/     glViewport(0, 0, windowWidth, windowHeight);
<default-render-target>.getTexture().blit(0, 0, 1, 1, 0, 0, 1, 1); // blit onto the current target RenderTarget
userRenderTarget.target(); // restore the old current render target
Graphics._setInGraphicsPipeline(graphicsPipelineIdentifier, false); // we are done
```
Resizing the default RenderTarget (replacing it with a new one, having the right size)   
should be done before calling all ECB pipelines,   
and rendering it the the final frame buffer should be done *after* calling the pipelines.  

# You are done!
Excluding a tool to generate a build of a project for this runtime, you are done!   
If you have a feature that you cannot implement, document it and throw a [`rosequartz.PlatformSpecificUnsupportedException`](https://rosequartzdocs.netlify.app/rosequartz/platformspecificunsupportedexception).   

You can test your runtime (if it implements `rosequartz-api-fx`) by using it to run [`Countryside`](https://github.com/devtaube/countryside).   
Remember that Countryside does not make use of the networking API.   

`rosequartz-api-base` and `rosequartz-api-fx` do not make use of `sun.misc.Unsafe` or any "big" java APIs (like `java.io` or `java.net`)   
(except slight use of reflection for the ECB), meaning you can also transpile it to other languages (like for example JavaScript,   
like the base build tools do).
