    
   Type       |     R     |     G     |     B      |     A     
    -----     |   -----   |   -----   |   -----    |   -----      |
  **D24_S8**  |         Depth  (24 bits)       | | |   Stencil    | 32 bits
  **RGBA8**   |  Albedo.R |  Albedo.G |  Albedo.B  |  Occlusion   | 32 bits
  **RGB10A2** |  Normal.X |  Normal.Y |  Normal.Z  | Light. Model | 32 bits
  **RGBA8**   |  Emissive | Roughness |  Metallic  | Translucency | 32 bits
  **RGBA8**   |  Motion X |  Motion Y | Distortion |              | 32 bits
  
   Type       |     R     |     G     |     B     |     A     
    -----     |   -----   |   -----   |   -----   |   -----   |
  **RGB16**   |   HDR.R   |   HDR.G   |   HDR.B   |           | 48 bits
  
```
render (scene) {
    var culler = scene.culler;
    var cameras = scene.cameras;
    
    foreach (camera in cameras) {
    
        // Selects shadow casters which shadows will be visible on screen and which should be 
        // updated in this frame (based on their refresh rate)
        var casters = culler.cullShadowCasters(scene, camera);
        
        foreach (caster in casters) {
        
            // Selects objects that will contribute to the creation of shadow map, that are also
            // big enough to make a visible difference and/or forced as shadow casters. Also
            // orders the object front to back to improve efficiency of early depth test.
            var renderables = culler.cullObjects(scene, caster);
            
            // Binds and clears shadow map FBO, bind shadow map shader, prepares uniforms 
            // and opengl state.
            caster.setup();
            
            // todo: create batch?
            
            foreach (renderable in renderables) {
            
                // Each renderable object has render() method used for rendering. Concrete rendering
                // method as gl function used to draw, used shader, uniform binding, material 
                // and/or other buffers is considered as an implementaion detail. 
                object.render();
            }
        }
        
         // Cull all cubemaps which reflections will contribute to the final scene
         // by checking visibility of their bounding boxes.
         var cubemaps = culler.cullObjects(scene, camera, CubeMap.class)
         
         foreach (cubemap in cubemaps) {
             // Captures the state of cubemap by rendering all 6 faces of cubemap.
             cubemap.capute();
         }
    
        // Selects opaque (as this is deffered part of render loop) renderable world objects
        // like characters, vehicles, buildings, terrin and foliage which will be visible
        // in this frame. Also orders the object front to back to improve efficiency of 
        // early depth test.
        var renderables = culler.cullObjects(scene, camera, CullOption.OPAQUE);
        var instanced = instacncing.createInstances(renderables);
        
        // Binds and clears camera frame buffer, binds shader, recomputes 
        // camera matrices if needed and updates opengl state.
        camera.setup();
        
        foreach (renderable in renderables) {
            object.render();
        }
        
        foreach (renderable in instanced) {
            object.renderInstanced();
        }
        
        
        
        // [todo: decals]
        
        
        
        // Cull all analytical lights which contribution of light will be visible in this
        // frame. Also order lights by their screen space position (this should help improve
        // cache efficiency).
        var lights = culler.cullLights(scene, camera);
        
        foreach (light in lights) {
        
            // Renders the light's contribution to the scene by reading gbuffers
            // which will be additively blended. The concrete method of rendring the light
            // (for example stancil tested light volumes) and it's shader(s), uniforms and/or
            // opengl state changes are considered as an implementation detail.
            
            // Directional ligths are rendered as full screen quads. Point and spot lights
            // are rendered by stencil tested light volumes. For point light sphere is used.
            // For the spot lights cone is used. 
            
            // [todo: area lights]
            light.render();
        }
        
        // [todo: GI]
        
        // [todo: Transparent objects using forward rendering (currently unspported)]
        
        // [todo: volumetric, height and local fog]
        
        
        // Render reflections from previously captured cubemaps.
        
        foreach (cubemap in cubemaps) {
            // Renderes paralax corrected contribution of IBL to final image from
            // this cubemap.
            cubemap.render();  
        }
        
        // [todo: SSR]
        
        // [todo: skybox rendering]
        var sky = scene.sky;
        sky.render();
        
        // [todo: particles]
        
        // Get linked list of camera screen-space post processing effects.
        var effect = camera.effects;
        
        while ((effect = effect.next) != null) {
        
            // Passes the reference to last effect to provide texture to used for
            // this effect. Also binds FBO of this object to render to.
            effect.prepare(effect.last);
            
            // Applies the effect.
            effect.render();
        }
    }
}
```