/**
 * lpsim - 
 * Copyright (c) 2015, Matej Kormuth <http://www.github.com/dobrakmato>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package eu.matejkormuth.lpsim;

public class TerrainGenerator {

    private float frequency =  1 / 4f;
    private float amplitude = 512f;

    public double get(int x, int z) {
        double result = 0.0D;

        //result += layer(x, z, 1 / 192f, 1 / 1.5f);
        result += layer(x, z, frequency / 128f, 1 / 1.5f);
        result += layer(x, z, frequency / 64f, 1 / 4.5f);
        result += layer(x, z, frequency / 32f, 1 / 10f);
        result += layer(x, z, frequency / 16f, 1 / 20f);
        result += layer(x, z, frequency / 8f, 1 / 40f);
        result += layer(x, z, frequency / 4f, 1 / 70f);
        result += layer(x, z, frequency  / 2f, 1 / 128f);
        result += layer(x, z, frequency / 1f, 1 / 256f);
        //result += layer(x, z, frequency / 0.5f, 1 / 512f);
        //result += layer(x, z, frequency / 0.25f, 1 / 1024f);

        boolean sign = result < 0;
        result = Math.pow(Math.abs(result), 6D);
        if (sign)
            result *= -1;

        return result * amplitude;
    }

    // forceinline
    public double layer(int x, int z, float f, float i) {
        return ((SimplexNoise.noise(x * f, z * f) + 1) / 2) * i;
    }
}
