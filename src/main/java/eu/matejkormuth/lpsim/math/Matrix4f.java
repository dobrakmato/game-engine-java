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
package eu.matejkormuth.lpsim.math;

import eu.matejkormuth.math.quaternions.Quaternionf;
import eu.matejkormuth.math.vectors.Vector3f;

import java.util.Arrays;

public class Matrix4f {
    /**
     * Identity matrix.
     */
    public static final Matrix4f IDENTITY = new Matrix4f().initIdentity(); // stupid idea, because matrices are currently mutable. will fix later...

    public float[][] m;

    public Matrix4f() {
        m = new float[4][4];
    }

    public Matrix4f initShadowMapBias() {
        m[0][0] = 0.5f;
        m[0][1] = 0;
        m[0][2] = 0;
        m[0][3] = 0;

        m[1][0] = 0;
        m[1][1] = 0.5f;
        m[1][2] = 0;
        m[1][3] = 0;

        m[2][0] = 0;
        m[2][1] = 0;
        m[2][2] = 0.5f;
        m[2][3] = 0;

        m[3][0] = 0.5f;
        m[3][1] = 0.5f;
        m[3][2] = 0.5f;
        m[3][3] = 1;

        return this;
    }

    public Matrix4f initIdentity() {
        m[0][0] = 1;
        m[0][1] = 0;
        m[0][2] = 0;
        m[0][3] = 0;

        m[1][0] = 0;
        m[1][1] = 1;
        m[1][2] = 0;
        m[1][3] = 0;

        m[2][0] = 0;
        m[2][1] = 0;
        m[2][2] = 1;
        m[2][3] = 0;

        m[3][0] = 0;
        m[3][1] = 0;
        m[3][2] = 0;
        m[3][3] = 1;

        return this;
    }

    public Matrix4f initScale(float x, float y, float z) {
        m[0][0] = x;
        m[0][1] = 0;
        m[0][2] = 0;
        m[0][3] = 0;

        m[1][0] = 0;
        m[1][1] = y;
        m[1][2] = 0;
        m[1][3] = 0;

        m[2][0] = 0;
        m[2][1] = 0;
        m[2][2] = z;
        m[2][3] = 0;

        m[3][0] = 0;
        m[3][1] = 0;
        m[3][2] = 0;
        m[3][3] = 1;

        return this;
    }

    public static Matrix4f initScale(Matrix4f mat, float x, float y, float z) {
        mat.m[0][0] = x;
        mat.m[0][1] = 0;
        mat.m[0][2] = 0;
        mat.m[0][3] = 0;

        mat.m[1][0] = 0;
        mat.m[1][1] = y;
        mat.m[1][2] = 0;
        mat.m[1][3] = 0;

        mat.m[2][0] = 0;
        mat.m[2][1] = 0;
        mat.m[2][2] = z;
        mat.m[2][3] = 0;

        mat.m[3][0] = 0;
        mat.m[3][1] = 0;
        mat.m[3][2] = 0;
        mat.m[3][3] = 1;

        return mat;
    }

    public Matrix4f initTranslation(float x, float y, float z) {
        m[0][0] = 1;
        m[0][1] = 0;
        m[0][2] = 0;
        m[0][3] = x;

        m[1][0] = 0;
        m[1][1] = 1;
        m[1][2] = 0;
        m[1][3] = y;

        m[2][0] = 0;
        m[2][1] = 0;
        m[2][2] = 1;
        m[2][3] = z;

        m[3][0] = 0;
        m[3][1] = 0;
        m[3][2] = 0;
        m[3][3] = 1;

        return this;
    }

    public static Matrix4f initTranslation(Matrix4f mat, float x, float y, float z) {
        mat.m[0][0] = 1;
        mat.m[0][1] = 0;
        mat.m[0][2] = 0;
        mat.m[0][3] = x;

        mat.m[1][0] = 0;
        mat.m[1][1] = 1;
        mat.m[1][2] = 0;
        mat.m[1][3] = y;

        mat.m[2][0] = 0;
        mat.m[2][1] = 0;
        mat.m[2][2] = 1;
        mat.m[2][3] = z;

        mat.m[3][0] = 0;
        mat.m[3][1] = 0;
        mat.m[3][2] = 0;
        mat.m[3][3] = 1;

        return mat;
    }

    public static Matrix4f createRotation(Quaternionf rot) {
        rot = rot.normalize();
        Matrix4f mat = new Matrix4f();
        mat.m[0][0] = 1 - 2 * rot.getY() * rot.getY() - 2 * rot.getZ() * rot.getZ();
        mat.m[0][1] = 2 * rot.getX() * rot.getY() - 2 * rot.getW() * rot.getZ();
        mat.m[0][2] = 2 * rot.getX() * rot.getZ() + 2 * rot.getW() * rot.getY();
        mat.m[0][3] = 0;

        mat.m[1][0] = 2 * rot.getX() * rot.getY() + 2 * rot.getW() * rot.getZ();
        mat.m[1][1] = 1 - 2 * rot.getX() * rot.getX() - 2 * rot.getZ() * rot.getZ();
        mat.m[1][2] = 2 * rot.getY() * rot.getZ() - 2 * rot.getW() * rot.getX();
        mat.m[1][3] = 0;

        mat.m[2][0] = 2 * rot.getX() * rot.getZ() - 2 * rot.getW() * rot.getY();
        mat.m[2][1] = 2 * rot.getY() * rot.getZ() + 2 * rot.getX() * rot.getW();
        mat.m[2][2] = 1 - 2 * rot.getX() * rot.getX() - 2 * rot.getY() * rot.getY();
        mat.m[2][3] = 0;

        mat.m[3][0] = 0;
        mat.m[3][1] = 0;
        mat.m[3][2] = 0;
        mat.m[3][3] = 1;

        return mat;
    }

    public static Matrix4f initRotation(Matrix4f mat, Quaternionf rot) {
        rot = rot.normalize();
        mat.m[0][0] = 1 - 2 * rot.getY() * rot.getY() - 2 * rot.getZ() * rot.getZ();
        mat.m[0][1] = 2 * rot.getX() * rot.getY() - 2 * rot.getW() * rot.getZ();
        mat.m[0][2] = 2 * rot.getX() * rot.getZ() + 2 * rot.getW() * rot.getY();
        mat.m[0][3] = 0;

        mat.m[1][0] = 2 * rot.getX() * rot.getY() + 2 * rot.getW() * rot.getZ();
        mat.m[1][1] = 1 - 2 * rot.getX() * rot.getX() - 2 * rot.getZ() * rot.getZ();
        mat.m[1][2] = 2 * rot.getY() * rot.getZ() - 2 * rot.getW() * rot.getX();
        mat.m[1][3] = 0;

        mat.m[2][0] = 2 * rot.getX() * rot.getZ() - 2 * rot.getW() * rot.getY();
        mat.m[2][1] = 2 * rot.getY() * rot.getZ() + 2 * rot.getX() * rot.getW();
        mat.m[2][2] = 1 - 2 * rot.getX() * rot.getX() - 2 * rot.getY() * rot.getY();
        mat.m[2][3] = 0;

        mat.m[3][0] = 0;
        mat.m[3][1] = 0;
        mat.m[3][2] = 0;
        mat.m[3][3] = 1;

        return mat;
    }


    public Matrix4f initRotation(float degreesX, float degreesY, float degreesZ) {

        Matrix4f rx = new Matrix4f();
        Matrix4f ry = new Matrix4f();
        Matrix4f rz = new Matrix4f();

        rz.m[0][0] = (float) Math.cos(degreesZ);
        rz.m[0][1] = (float) -Math.sin(degreesZ);
        rz.m[0][2] = 0;
        rz.m[0][3] = 0;

        rz.m[1][0] = (float) Math.sin(degreesZ);
        rz.m[1][1] = (float) Math.cos(degreesZ);
        rz.m[1][2] = 0;
        rz.m[1][3] = 0;

        rz.m[2][0] = 0;
        rz.m[2][1] = 0;
        rz.m[2][2] = 1;
        rz.m[2][3] = 0;

        rz.m[3][0] = 0;
        rz.m[3][1] = 0;
        rz.m[3][2] = 0;
        rz.m[3][3] = 1;

        // ----------------------

        rx.m[0][0] = 1;
        rx.m[0][1] = 0;
        rx.m[0][2] = 0;
        rx.m[0][3] = 0;

        rx.m[1][0] = 0;
        rx.m[1][1] = (float) Math.cos(degreesX);
        rx.m[1][2] = (float) -Math.sin(degreesX);
        rx.m[1][3] = 0;

        rx.m[2][0] = 0;
        rx.m[2][1] = (float) Math.sin(degreesX);
        rx.m[2][2] = (float) Math.cos(degreesX);
        rx.m[2][3] = 0;

        rx.m[3][0] = 0;
        rx.m[3][1] = 0;
        rx.m[3][2] = 0;
        rx.m[3][3] = 1;

        // ---------------------

        ry.m[0][0] = (float) Math.cos(degreesY);
        ry.m[0][1] = 0;
        ry.m[0][2] = -(float) Math.sin(degreesY);
        ry.m[0][3] = 0;

        ry.m[1][0] = 0;
        ry.m[1][1] = 1;
        ry.m[1][2] = 0;
        ry.m[1][3] = 0;

        ry.m[2][0] = (float) Math.sin(degreesY);
        ry.m[2][1] = 0;
        ry.m[2][2] = (float) Math.cos(degreesY);
        ry.m[2][3] = 0;

        ry.m[3][0] = 0;
        ry.m[3][1] = 0;
        ry.m[3][2] = 0;
        ry.m[3][3] = 1;

        m = rz.multiply(ry.multiply(rx)).m;
        return this;
    }

    public static Matrix4f initRotation(Matrix4f mat, float degreesX, float degreesY, float degreesZ) {

        Matrix4f rx = new Matrix4f();
        Matrix4f ry = new Matrix4f();
        Matrix4f rz = new Matrix4f();

        rz.m[0][0] = (float) Math.cos(degreesZ);
        rz.m[0][1] = -(float) Math.sin(degreesZ);
        rz.m[0][2] = 0;
        rz.m[0][3] = 0;

        rz.m[1][0] = (float) Math.sin(degreesZ);
        rz.m[1][1] = (float) Math.cos(degreesZ);
        rz.m[1][2] = 0;
        rz.m[1][3] = 0;

        rz.m[2][0] = 0;
        rz.m[2][1] = 0;
        rz.m[2][2] = 1;
        rz.m[2][3] = 0;

        rz.m[3][0] = 0;
        rz.m[3][1] = 0;
        rz.m[3][2] = 0;
        rz.m[3][3] = 1;

        // ----------------------

        rx.m[0][0] = 1;
        rx.m[0][1] = 0;
        rx.m[0][2] = 0;
        rx.m[0][3] = 0;

        rx.m[1][0] = 0;
        rx.m[1][1] = (float) Math.cos(degreesX);
        rx.m[1][2] = -(float) Math.sin(degreesX);
        rx.m[1][3] = 0;

        rx.m[2][0] = 0;
        rx.m[2][1] = (float) Math.sin(degreesX);
        rx.m[2][2] = (float) Math.cos(degreesX);
        rx.m[2][3] = 0;

        rx.m[3][0] = 0;
        rx.m[3][1] = 0;
        rx.m[3][2] = 0;
        rx.m[3][3] = 1;

        // ---------------------

        ry.m[0][0] = (float) Math.cos(degreesY);
        ry.m[0][1] = 0;
        ry.m[0][2] = -(float) Math.sin(degreesY);
        ry.m[0][3] = 0;

        ry.m[1][0] = 0;
        ry.m[1][1] = 1;
        ry.m[1][2] = 0;
        ry.m[1][3] = 0;

        ry.m[2][0] = (float) Math.sin(degreesY);
        ry.m[2][1] = 0;
        ry.m[2][2] = (float) Math.cos(degreesY);
        ry.m[2][3] = 0;

        ry.m[3][0] = 0;
        ry.m[3][1] = 0;
        ry.m[3][2] = 0;
        ry.m[3][3] = 1;

        mat.m = rz.multiply(ry.multiply(rx)).m;
        return mat;
    }

    public Matrix4f initPerspective(float fov, float width, float height, float zNear, float zFar) {
        float ar = width / height;
        float tanHalfFOV = (float) Math.tan(Math.toRadians(fov / 2));
        float zRange = zNear - zFar;

        m[0][0] = 1f / (tanHalfFOV * ar);
        m[0][1] = 0;
        m[0][2] = 0;
        m[0][3] = 0;

        m[1][0] = 0;
        m[1][1] = 1f / tanHalfFOV;
        m[1][2] = 0;
        m[1][3] = 0;

        m[2][0] = 0;
        m[2][1] = 0;
        m[2][2] = (-zNear - zFar) / zRange;
        m[2][3] = 2 * zFar * zNear / zRange;

        m[3][0] = 0;
        m[3][1] = 0;
        m[3][2] = 1;
        m[3][3] = 0;

        return this;
    }

    public static Matrix4f initPerspective(Matrix4f mat, float fov, float width, float height, float zNear, float zFar) {
        float ar = width / height;
        float tanHalfFOV = (float) Math.tan(Math.toRadians(fov / 2));
        float zRange = zNear - zFar;

        mat.m[0][0] = 1f / (tanHalfFOV * ar);
        mat.m[0][1] = 0;
        mat.m[0][2] = 0;
        mat.m[0][3] = 0;

        mat.m[1][0] = 0;
        mat.m[1][1] = 1f / tanHalfFOV;
        mat.m[1][2] = 0;
        mat.m[1][3] = 0;

        mat.m[2][0] = 0;
        mat.m[2][1] = 0;
        mat.m[2][2] = (-zNear - zFar) / zRange;
        mat.m[2][3] = 2 * zFar * zNear / zRange;

        mat.m[3][0] = 0;
        mat.m[3][1] = 0;
        mat.m[3][2] = 1;
        mat.m[3][3] = 0;

        return mat;
    }

    public Matrix4f initLookAt(Vector3f eye, Vector3f center, Vector3f up) {
        Vector3f f = center.subtract(eye).normalize();
        Vector3f s = f.cross(up.normalize()).normalize();
        Vector3f u = s.cross(f);

        m[0][0] = s.getX();
        m[0][1] = u.getX();
        m[0][2] = -f.getX();
        m[0][3] = 0;

        m[1][0] = s.getY();
        m[1][1] = u.getY();
        m[1][2] = -f.getY();
        m[1][3] = 0;

        m[2][0] = s.getZ();
        m[2][1] = u.getZ();
        m[2][2] = -f.getZ();
        m[2][3] = 0;

        m[3][0] = -s.dot(eye);
        m[3][1] = -u.dot(eye);
        m[3][2] = -f.dot(eye);
        m[3][3] = 1;

        return this;
    }

    public static Matrix4f initLookAt(Matrix4f mat, Vector3f eye, Vector3f center, Vector3f up) {
        Vector3f f = center.subtract(eye).normalize();
        Vector3f s = f.cross(up.normalize()).normalize();
        Vector3f u = s.cross(f);

        mat.m[0][0] = s.getX();
        mat.m[0][1] = u.getX();
        mat.m[0][2] = -f.getX();
        mat.m[0][3] = 0;

        mat.m[1][0] = s.getY();
        mat.m[1][1] = u.getY();
        mat.m[1][2] = -f.getY();
        mat.m[1][3] = 0;

        mat.m[2][0] = s.getZ();
        mat.m[2][1] = u.getZ();
        mat.m[2][2] = -f.getZ();
        mat.m[2][3] = 0;

        mat.m[3][0] = -s.dot(eye);
        mat.m[3][1] = -u.dot(eye);
        mat.m[3][2] = -f.dot(eye);
        mat.m[3][3] = 1;

        return mat;
    }

    public Matrix4f initOrtho(float l, float r, float b, float t, float n, float f) {
        m[0][0] = 2.0f / (r - l);
        m[0][1] = 0.0f;
        m[0][2] = 0.0f;
        m[0][3] = -(r + l) / (r - l);
        m[1][0] = 0.0f;
        m[1][1] = 2.0f / (t - b);
        m[1][2] = 0.0f;
        m[1][3] = -(t + b) / (t - b);
        m[2][0] = 0.0f;
        m[2][1] = 0.0f;
        m[2][2] = 2.0f / (f - n);
        m[2][3] = -(f + n) / (f - n);
        m[3][0] = 0.0f;
        m[3][1] = 0.0f;
        m[3][2] = 0.0f;
        m[3][3] = 1.0f;

        return this;
    }

    public static Matrix4f initOrtho(Matrix4f mat, float l, float r, float b, float t, float n, float f) {
        mat.m[0][0] = 2.0f / (r - l);
        mat.m[0][1] = 0.0f;
        mat.m[0][2] = 0.0f;
        mat.m[0][3] = -(r + l) / (r - l);
        mat.m[1][0] = 0.0f;
        mat.m[1][1] = 2.0f / (t - b);
        mat.m[1][2] = 0.0f;
        mat.m[1][3] = -(t + b) / (t - b);
        mat.m[2][0] = 0.0f;
        mat.m[2][1] = 0.0f;
        mat.m[2][2] = 2.0f / (f - n);
        mat.m[2][3] = -(f + n) / (f - n);
        mat.m[3][0] = 0.0f;
        mat.m[3][1] = 0.0f;
        mat.m[3][2] = 0.0f;
        mat.m[3][3] = 1.0f;

        return mat;
    }

    public Matrix4f initCamera(Vector3f forward, Vector3f up) {
        Vector3f r = up.cross(forward).normalize();
        Vector3f u = forward.cross(r).normalize();

        m[0][0] = r.getX();
        m[0][1] = r.getY();
        m[0][2] = r.getZ();
        m[0][3] = 0;

        m[1][0] = u.getX();
        m[1][1] = u.getY();
        m[1][2] = u.getZ();
        m[1][3] = 0;

        m[2][0] = forward.getX();
        m[2][1] = forward.getY();
        m[2][2] = forward.getZ();
        m[2][3] = 0;

        m[3][0] = 0;
        m[3][1] = 0;
        m[3][2] = 0;
        m[3][3] = 1;

        return this;
    }

    public static Matrix4f initCamera(Matrix4f mat, Vector3f forward, Vector3f up) {
        Vector3f r = up.cross(forward);
        Vector3f u = forward.cross(r);

        mat.m[0][0] = r.getX();
        mat.m[0][1] = r.getY();
        mat.m[0][2] = r.getZ();
        mat.m[0][3] = 0;

        mat.m[1][0] = u.getX();
        mat.m[1][1] = u.getY();
        mat.m[1][2] = u.getZ();
        mat.m[1][3] = 0;

        mat.m[2][0] = forward.getX();
        mat.m[2][1] = forward.getY();
        mat.m[2][2] = forward.getZ();
        mat.m[2][3] = 0;

        mat.m[3][0] = 0;
        mat.m[3][1] = 0;
        mat.m[3][2] = 0;
        mat.m[3][3] = 1;

        return mat;
    }

    public Matrix4f multiply(Matrix4f mat) {
        Matrix4f result = new Matrix4f();

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                result.m[x][y] = this.m[x][0] * mat.m[0][y] + //
                        this.m[x][1] * mat.m[1][y] + //
                        this.m[x][2] * mat.m[2][y] + //
                        this.m[x][3] * mat.m[3][y]; //
            }
        }

        return result;
    }

    public Matrix4f transpose() {
        Matrix4f mat = new Matrix4f();

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                mat.m[y][x] = mat.m[x][y];
            }
        }

        return mat;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(m);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Matrix4f other = (Matrix4f) obj;
        return Arrays.deepEquals(m, other.m);
    }

    @Override
    public String toString() {
        return Arrays.toString(m[0]) + "\n" + Arrays.toString(m[1]) + "\n" + Arrays.toString(m[2]) + "\n"
                + Arrays.toString(m[3]);
    }

    public Matrix4f invert() {
        float a = m[0][0] * m[1][1] - m[0][1] * m[1][0];
        float b = m[0][0] * m[1][2] - m[0][2] * m[1][0];
        float c = m[0][0] * m[1][3] - m[0][3] * m[1][0];
        float d = m[0][1] * m[1][2] - m[0][2] * m[1][1];
        float e = m[0][1] * m[1][3] - m[0][3] * m[1][1];
        float f = m[0][2] * m[1][3] - m[0][3] * m[1][2];
        float g = m[2][0] * m[3][1] - m[2][1] * m[3][0];
        float h = m[2][0] * m[3][2] - m[2][2] * m[3][0];
        float i = m[2][0] * m[3][3] - m[2][3] * m[3][0];
        float j = m[2][1] * m[3][2] - m[2][2] * m[3][1];
        float k = m[2][1] * m[3][3] - m[2][3] * m[3][1];
        float l = m[2][2] * m[3][3] - m[2][3] * m[3][2];
        float det = a * l - b * k + c * j + d * i - e * h + f * g;
        det = 1.0f / det;
        float nm00 = (m[1][1] * l - m[1][2] * k + m[1][3] * j) * det;
        float nm01 = (-m[0][1] * l + m[0][2] * k - m[0][3] * j) * det;
        float nm02 = (m[3][1] * f - m[3][2] * e + m[3][3] * d) * det;
        float nm03 = (-m[2][1] * f + m[2][2] * e - m[2][3] * d) * det;
        float nm10 = (-m[1][0] * l + m[1][2] * i - m[1][3] * h) * det;
        float nm11 = (m[0][0] * l - m[0][2] * i + m[0][3] * h) * det;
        float nm12 = (-m[3][0] * f + m[3][2] * c - m[3][3] * b) * det;
        float nm13 = (m[2][0] * f - m[2][2] * c + m[2][3] * b) * det;
        float nm20 = (m[1][0] * k - m[1][1] * i + m[1][3] * g) * det;
        float nm21 = (-m[0][0] * k + m[0][1] * i - m[0][3] * g) * det;
        float nm22 = (m[3][0] * e - m[3][1] * c + m[3][3] * a) * det;
        float nm23 = (-m[2][0] * e + m[2][1] * c - m[2][3] * a) * det;
        float nm30 = (-m[1][0] * j + m[1][1] * h - m[1][2] * g) * det;
        float nm31 = (m[0][0] * j - m[0][1] * h + m[0][2] * g) * det;
        float nm32 = (-m[3][0] * d + m[3][1] * b - m[3][2] * a) * det;
        float nm33 = (m[2][0] * d - m[2][1] * b + m[2][2] * a) * det;
        this.m[0][0] = (nm00);
        this.m[0][1] = (nm01);
        this.m[0][2] = (nm02);
        this.m[0][3] = (nm03);
        this.m[1][0] = (nm10);
        this.m[1][1] = (nm11);
        this.m[1][2] = (nm12);
        this.m[1][3] = (nm13);
        this.m[2][0] = (nm20);
        this.m[2][1] = (nm21);
        this.m[2][2] = (nm22);
        this.m[2][3] = (nm23);
        this.m[3][0] = (nm30);
        this.m[3][1] = (nm31);
        this.m[3][2] = (nm32);
        this.m[3][3] = (nm33);
        return this;
    }
}