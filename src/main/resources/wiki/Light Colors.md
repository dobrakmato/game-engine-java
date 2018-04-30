I found a listing of light energies for various types of lights, intended for setting up lighting in CG scenes. I converted the data over to RGB, and I've found the results to be handy for configuring realistic-looking lights in the DX11 renderer.
I recommend you set the intensity of lights (spot and interior) to 1.0, and falloff to 2.0 (interior, maybe spot) - then playing with the radius and adjusting intensity as desired. I recommend you leave falloff at 2.0, and only adjust it if the other settings can't get you what you want... as this is quadratic falloff which is what light actually does, and given that DX11 is doing PBR, it seems to work quite well. That said, when the HDR thing starts changing the dynamic range on you, the edge does begin to look sharp.
Anyway... here's the list!

|name	|	 R 0-255	|	 G 0-255	|	 B 0-255        |
|:-----:|:-------------:|:-------------:|:-----------------:|
|Candle	|	 255	|	 147	|	 41     |
|40W Tungsten	|	 255	|	 197	|	 143        |
|100W Tungsten	|	 255	|	 214	|	 170        |
|Halogen	|	 255	|	 241	|	 224        |
|Carbon Arc	|	 255	|	 250	|	 244        |
|High Noon Sun	|	 255	|	 255	|	 251        |
|Direct Sunlight	|	 255	|	 255	|	 255        |
|Overcast Sky	|	201	|	 226	|	 255        |
|Clear Blue Sky	|	 64	|	 156	|	 255        |
|Warm Fluorescent	|	 255	|	 244	|	 229        |
|Standard Fluorescent	|	 244	|	 255	|	 250        |
|Cool White Fluorescent	|	 212	|	 235	|	 255        |
|Full Spectrum Fluorescent	|	 255	|	 244	|	 242        |
|Grow Light Fluorescent	|	 255	|	 239	|	 247        |
|Black Light Fluorescent	|	 167	|	 0	|	 255        |
|Mercury Vapor	|	 216	|	 247	|	 255        |
|Sodium Vapor	|	 255	|	 209	|	 178        |
|Metal Halide	|	 242	|	 252	|	 255        |
|High Pressure Sodium	|	 255	|	 183	|	 76     |