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
import eu.matejkormuth.math.vectors.Vector3f;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class WorldObject implements Disposable {

    // Dynamic SLF4 logger for all subtypes.
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Deprecated
    protected Material material;

    private List<ObjectComponent> components = new ArrayList<>(2);

    @Getter
    private Vector3f position = new Vector3f();
    @Getter
    private Vector3f rotation = new Vector3f(); // replace by quaternion
    @Getter
    private Vector3f scale = new Vector3f(1, 1, 1);

    public enum RenderMode {
        OPAQUE,
        TRANSPARENT,
        TRANSLUCENT
    }

    @Getter
    @Setter
    private RenderMode renderMode = RenderMode.OPAQUE;

    @Getter
    @Setter
    private String tag;

    @Getter
    @Setter
    private boolean backfaceCullingEnabled = true;

    @Getter
    @Setter
    protected boolean castingShadows = true;

    protected Matrix4f transformMatrix = new Matrix4f().initIdentity();
    private boolean transformDirty = true;

    public Matrix4f getTransformMatrix() {
        if (transformDirty) {
            // recalculate.
            Matrix4f translationMatrix = new Matrix4f().initTranslation(position.getX(), position.getY(), position.getZ());
            Matrix4f rotationMatrix = new Matrix4f().initRotation(rotation.getX(), rotation.getY(), rotation.getZ());
            Matrix4f scaleMatrix = new Matrix4f().initScale(scale.getX(), scale.getY(), scale.getZ());

            transformMatrix = translationMatrix.multiply(rotationMatrix.multiply(scaleMatrix));
        }
        return transformMatrix;
    }

    public void setScale(Vector3f scale) {
        this.scale = scale;
        transformDirty = true;
    }

    public void setRotation(Vector3f rotation) {
        this.rotation = rotation;
        transformDirty = true;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
        transformDirty = true;
    }

    public abstract void render(@Nonnull Camera camera);

    @Deprecated
    public Material getMaterial() {
        return material;
    }

    /**
     * Returns current world object's program. Defaults to getMaterial().getProgram().
     *
     * @return program this object is rendered
     */
    @Deprecated
    public LightPrograms getLightPrograms() {
        return material.getLightPrograms();
    }

    public void update(float deltaTime) {
        for (int i = 0; i < components.size(); i++) {
            components.get(i).update(deltaTime);
        }
    }

    public void addComponent(ObjectComponent component) {
        if (component.parent != null) {
            throw new RuntimeException("Component " + component.toString() + " is already attached to " + component.parent.toString() + "!");
        }

        component.parent = this;
        this.components.add(component);
    }
}
