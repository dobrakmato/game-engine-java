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

import eu.matejkormuth.math.vectors.Vector3f;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Frustum {

    // Based on Radar Approach: http://www.lighthouse3d.com/tutorials/view-frustum-culling.

    private Vector3f cc;
    private Vector3f x, y, z;

    private float nearD;
    private float farD;
    private float width;
    private float height;
    private float ratio;
    private float tang;

    private float sphereFactorX;
    private float sphereFactorY;

    private static final double HALF_ANG2RAD = Math.PI / 360.0;

    // call when projection changes
    public void setCamInternals(float angdeg, float ratio, float nearD, float farD) {
        this.ratio = ratio;
        this.nearD = nearD;
        this.farD = farD;

        angdeg *= HALF_ANG2RAD; // convert to radians and divide by two
        this.tang = (float) Math.tan(angdeg);

        sphereFactorY = (float) (1.0 / Math.cos(angdeg));
        float anglex = (float) Math.atan(tang * ratio);
        sphereFactorX = (float) (1.0 / Math.cos(anglex));

        this.height = nearD * tang;
        this.width = height * ratio;
    }

    // call when camera position or rotation changes
    public void setCamDef(Vector3f point, Vector3f lookAt, Vector3f up) {
        this.cc = point;

        z = lookAt.subtract(point).normalize();
        x = z.cross(up).normalize();
        y = x.cross(z).normalize();
    }

    public Intersection testPoint(Vector3f point) {
        Vector3f v = point.subtract(cc);
        float pcz = v.dot(z.negate());
        if (pcz > farD || pcz < nearD) {
            return Intersection.OUTSIDE;
        }

        float pcy = v.dot(y);
        float aux = pcz * tang;
        if (pcy > aux || pcy < -aux) {
            return Intersection.OUTSIDE;
        }

        float pcx = v.dot(x);
        aux = aux * ratio;
        if (pcx > aux || pcx < -aux) {
            return Intersection.OUTSIDE;
        }

        return Intersection.INSIDE;
    }

    public Intersection testSphere(Vector3f center, float radius) {
        Vector3f v = center.subtract(cc);
        Intersection result = Intersection.INSIDE;

        float az = v.dot(z.negate());
        if (az > farD + radius || az < nearD - radius) {
            return Intersection.OUTSIDE;
        }
        if (az > farD - radius || az < nearD + radius) {
            result = Intersection.INTERSECT;
        }

        float ay = v.dot(y);
        float d = sphereFactorY * radius;
        az *= tang;
        if (ay > az + d || ay < -az - d) {
            return Intersection.OUTSIDE;
        }
        if (ay > az - d || ay < -az + d) {
            result = Intersection.INTERSECT;
        }

        float ax = v.dot(x);
        az *= ratio;
        d = sphereFactorX * radius;
        if (ax > az + d || ax < -az - d) {
            return Intersection.OUTSIDE;
        }
        if (ax > az - d || ax < -ax + d) {
            result = Intersection.INTERSECT;
        }

        return result;
    }
}
