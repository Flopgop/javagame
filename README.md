# game1

note: this will crash without statue.glb

download the statue model from [here](https://www.myminifactory.com/object/3d-print-discobolus-271434) <br/>
open the stl in blender and scale the model to 0.005, translate it by -2.5 on the x-axis, -1.25 on the y-axis <br/>
apply all transforms <br/>
give it a material and assign a texture to color, roughness, and metallic <br/>
export it as glb (make sure to pack textures n such, that's on by default in blender 4.3.2, haven't checked other versions) <br/>
put the newly created glb file in src/main/resources and rename it "statue.glb" <br/>
now the game shouldn't crash when you start it.

If you need to recreate statue_collision.obj, I just put a convex hull over the statue.glb model, decimated it until it was under 256 vertices (!!IMPORTANT), triangulated it, and repeated the previous two steps until I got a collision shape I liked.
It needs to be exported as an obj, don't export materials, triangulate it, and leave the other settings at defaults.