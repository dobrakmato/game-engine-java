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

import eu.matejkormuth.lpsim.math.Matrix4f;
import eu.matejkormuth.math.quaternions.Quaternionf;
import eu.matejkormuth.math.vectors.Vector3f;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@Slf4j
public class Camera {

    private static final Vector3f UP = new Vector3f(0, 1, 0);
    private static final Vector3f AXIS_Y = new Vector3f(0, 1, 0);

    @Getter
    @Setter
    private Vector3f position;
    @Getter
    @Setter
    private Vector3f forward;
    @Getter
    @Setter
    private Vector3f up;

    private Quaternionf orientation = new Quaternionf(1, 0, 0, 0);

    @Getter
    private Frustum frustum = new Frustum();
    private boolean frustumDirty = true;

    @Getter
    private float near = 0.1f;
    @Getter
    private float far = 6000f;
    private float fov = 60f;

    private Matrix4f rotationMatrix = new Matrix4f().initIdentity();
    private Matrix4f translationMatrix = new Matrix4f().initIdentity();
    private Matrix4f projectionMatrix = new Matrix4f().initIdentity();

    @Getter
    private float exposure;

    public Camera() {
        this.position = new Vector3f(490, 128, 490);
        this.up = UP.normalize();
        this.forward = new Vector3f(0, 0, 1).normalize();
        this.projectionMatrix.initPerspective(this.fov, World.CANVAS_WIDTH, World.CANVAS_HEIGHT, near, far);
        frustum.setCamInternals(this.fov, World.CANVAS_WIDTH / (float) World.CANVAS_HEIGHT, near, far);
        frustum.setCamDef(position, position.subtract(forward), up);
    }

    public Camera(Vector3f position, Vector3f forward, Vector3f up) {
        this.position = position;
        this.forward = forward.normalize();
        this.up = up.normalize();
        this.projectionMatrix.initPerspective(this.fov, World.CANVAS_WIDTH, World.CANVAS_HEIGHT, near, far);
        frustum.setCamInternals(this.fov, World.CANVAS_WIDTH / (float) World.CANVAS_HEIGHT, near, far);
        frustum.setCamDef(position, position.subtract(forward), up);
    }

    public Vector3f getForward() {
        return forward;
    }

    public Vector3f getRight() {
        return forward.cross(up);
    }

    public Vector3f getLeft() {
        return up.cross(forward);
    }

    public Vector3f getUp() {
        return up;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Matrix4f getViewMatrix() {
        //Matrix4f.initRotation(rotationMatrix, orientation);
        Matrix4f.initCamera(rotationMatrix, forward, up);
        //rotationMatrix = Matrix4f.createRotation(rotation);
        Matrix4f.initTranslation(translationMatrix, -position.getX(), -position.getY(), -position.getZ());

        return rotationMatrix.multiply(translationMatrix);
    }

    public Matrix4f getViewMatrix2() {
        Matrix4f.initCamera(rotationMatrix, forward, up);
        //rotationMatrix = Matrix4f.createRotation(rotation);
        Matrix4f.initTranslation(translationMatrix, -position.getX(), -position.getY(), -position.getZ());

        return translationMatrix;
    }

    public void move(Vector3f dir, float amount) {
        position = position.add(dir.multiply(amount));
    }


    public void update(float deltaTime) {
        float speed = 0.0125f * deltaTime;

        // Sprint modifier.
        if (Keyboard.isKeyDown(KeyBindings.KEY_SPRINT)) {
            speed *= 10;
        }
        if (Keyboard.isKeyDown(KeyBindings.KEY_CROUNCH)) {
            speed *= 0.01;
        }

        // Navigation (position)
        if (Keyboard.isKeyDown(KeyBindings.KEY_FORWARD)) {
            move(getForward(), speed);
            frustumDirty = true;
        }
        if (Keyboard.isKeyDown(KeyBindings.KEY_BACKWARD)) {
            move(getForward(), -speed);
            frustumDirty = true;
        }

        if (Keyboard.isKeyDown(KeyBindings.KEY_LEFT)) {
            move(getRight(), speed);
            frustumDirty = true;
        }
        if (Keyboard.isKeyDown(KeyBindings.KEY_RIGHT)) {
            move(getLeft(), speed);
            frustumDirty = true;
        }

        if (Keyboard.isKeyDown(KeyBindings.KEY_UP)) {
            move(getUp(), speed);
            frustumDirty = true;
        }
        if (Keyboard.isKeyDown(KeyBindings.KEY_DOWN)) {
            move(getUp(), -speed);
            frustumDirty = true;
        }

        if (Application.getInput().wasPressed(Keyboard.KEY_V)) {
            System.out.println("CAMPOS: new Vector3f(" + position.getX() + "f, " + position.getY() + "f, " + position.getZ() + "f)");
            System.out.println("CAMFORWARD: new Vector3f(" + forward.getX() + "f, " + forward.getY() + "f, " + forward.getZ() + "f)");
        }

        // Rotation (mouse).
        if (Mouse.isGrabbed()) {
            int dx = Mouse.getDX();
            int dy = Mouse.getDY();

            boolean rotY = dx != 0;
            boolean rotX = dy != 0;

            if (rotY) {
                rotateY(-dy * 0.03f);
                frustumDirty = true;
            }

            if (rotX) {
                rotateX(dx * 0.03f);
                frustumDirty = true;
            }
        }

        if (frustumDirty) {
            frustum.setCamDef(position, position.subtract(forward), up);
            frustumDirty = false;
        }
    }

    //private void rotateX(float x) {
    //    Quaternionf rot = Quaternionf.fromAngle(x, up);
    //    Quaternionf newForward = rot.multiply(forward);
    //    forward = new Vector3f(newForward.getX(), newForward.getY(), newForward.getZ()).normalize();
    //}
//
    //private void rotateY(float y) {
    //    Vector3f hAxis = up.cross(forward).normalize();
    //    Quaternionf rot = Quaternionf.fromAngle(y, hAxis);
    //    Quaternionf newUp = rot.multiply(up);
    //    up = new Vector3f(newUp.getX(), newUp.getY(), newUp.getZ()).normalize();
    //}

    private Vector3f quaternionRotate(Vector3f thiz, float degrees, Vector3f axis) {
        Quaternionf rotation = Quaternionf.fromAngle(degrees, axis).normalize();
        Quaternionf conjugate = rotation.conjugate().normalize();
        Quaternionf w = rotation.multiply(thiz).multiply(conjugate).normalize();
        return new Vector3f(w.getX(), w.getY(), w.getZ());
    }

    private void rotateY(float degrees) {
        Vector3f hAxis = AXIS_Y.cross(forward).normalize();

        Quaternionf rotation = Quaternionf.fromAngle(degrees, Vector3f.UNIT_X);
        orientation = orientation.multiply(rotation);

        forward = quaternionRotate(forward.normalize(), degrees * 3f, hAxis).normalize();
        up = forward.cross(hAxis).normalize();
    }

    private void rotateX(float degrees) {
        Vector3f hAxis = AXIS_Y.cross(forward).normalize();

        Quaternionf rotation = Quaternionf.fromAngle(degrees, AXIS_Y);
        orientation = orientation.multiply(rotation);

        forward = quaternionRotate(forward, degrees * 3f, AXIS_Y).normalize();
        up = forward.cross(hAxis).normalize();
    }

    public void setExposure(float exposure) {
        this.exposure = exposure;
    }
}
