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
package eu.matejkormuth.lpsim.timing;

import eu.matejkormuth.lpsim.Application;
import eu.matejkormuth.lpsim.Bootstrap;
import lombok.Getter;
import org.lwjgl.opengl.GL43;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GLProfiler implements Comparable<GLProfiler> {

    @Getter
    @Nullable
    private final GLProfiler parent;
    private final int level;
    private final List<GLProfiler> children = new ArrayList<>();

    @Getter
    private final String name;
    @Getter
    private long totalTime = 0L;
    @Getter
    private long invocations = 0L;

    private long lastStart = 0L;

    private GLProfiler(GLProfiler parent, String name, int level) {
        this.parent = parent;
        this.name = name;
        this.level = level;
    }

    public static GLProfiler createRoot() {
        return createRoot("root");
    }

    public static GLProfiler createRoot(String name) {
        return new GLProfiler(null, name, 0);
    }

    public GLProfiler createChild(String name) {
        GLProfiler p = new GLProfiler(this, name, level + 1);
        children.add(p);
        return p;
    }

    public void reset() {
        reset(false);
    }

    public void reset(boolean children) {
        this.invocations = 0;
        this.totalTime = 0;

        if (children) {
            for (GLProfiler profiler : this.children) {
                profiler.reset(true);
            }
        }
    }

    public void start() {
        if (Bootstrap.DEBUG) {
            invocations++;
            lastStart = System.nanoTime();
            //GL43.glDebugMessageInsert(GL43.GL_DEBUG_SOURCE_APPLICATION, GL43.GL_DEBUG_TYPE_MARKER,
            //        100, GL43.GL_DEBUG_SEVERITY_NOTIFICATION, this.name);
            GL43.glPushDebugGroup(GL43.GL_DEBUG_SOURCE_APPLICATION, 100, this.name);
        }
    }

    public long stop() {
        if (Bootstrap.DEBUG) {
            GL43.glPopDebugGroup();
            long timeSpent = (System.nanoTime() - lastStart);
            totalTime += timeSpent;
            return timeSpent;
        }
        return 0;
    }

    public void end() {
        if (Bootstrap.DEBUG) {
            GL43.glPopDebugGroup();
            //GL11.glFlush();
            //GL11.glFinish();
            //Util.checkGLError();
            totalTime += (System.nanoTime() - lastStart);
        }
    }

    @Override
    public String toString() {
        return toString(false);
    }

    private static final DecimalFormat df = new DecimalFormat("##.##");

    private String humanReadableTime(float ns) {
        if (ns < 1000) { // ns
            return ns + "ns";
        } else if (ns < 1_000_000 * 1000) { // ms
            return df.format(ns / 1_000_000f) + "ms";
        } else if (ns < 1_000_000 * 1000 * 60) { // s
            return df.format(ns / (1_000_000 * 1000f)) + "se";
        } else { // m
            return df.format(ns / (1_000_000 * 1000 * 60f)) + "mi";
        }
    }

    private String humanReadableNumber(long n) {
        if (n < 1000) {
            return n + "";
        } else if (n < 1000 * 1000) {
            return df.format(n / 1000) + "K";
        } else if (n < 1000 * 1000 * 1000) {
            return df.format(n / (1000 * 1000)) + "M";
        } else {
            return df.format(n / (1000 * 1000 * 1000)) + "G";
        }
    }

    public float safeDiv(long divisor, long divider) {
        if (divider == 0) {
            return 0f;
        } else {
            return divisor / (float) divider;
        }
    }

    @Override
    public int compareTo(GLProfiler o) {
        if (o.totalTime == totalTime)
            return 0;

        return o.totalTime > totalTime ? 1 : -1;
    }

    public String toString(boolean includeChildren) {
        StringBuilder sb = new StringBuilder();

        String TAB = "\t\t";

        if (level != 0) {
            //char[] ident = new char[level];
            //Arrays.fill(ident, ' ');
            sb.append(new String(new char[level]).replace('\0', ' '));
        } else {
            sb.append("Name").append(TAB).append("Frame").append(TAB).append("Avg").append(TAB).append("Inv").append(TAB).append("Total")
                    .append("\n");
        }

        sb.append(name).append(TAB)
                .append(df.format(safeDiv(totalTime, Application.get().getFrames()) / 1_000_000f) + "ms").append(TAB)
                .append(df.format(safeDiv(totalTime, invocations) / 1_000_000f) + "ms").append(TAB)
                .append(humanReadableNumber(invocations)).append(TAB)
                .append(df.format(totalTime / 1_000_000f) + "ms").append("\n");

        if (includeChildren && !children.isEmpty()) {
            List<GLProfiler> ordered = new ArrayList<>(children);
            Collections.sort(ordered);
            for (GLProfiler child : ordered) {
                sb.append(child.toString(true));
            }
        }

        return sb.toString();
    }
}
