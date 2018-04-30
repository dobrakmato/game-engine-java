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
package eu.matejkormuth.lpsim.gl;

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.KHRDebugCallback;

@Slf4j
public class GLDebugHandler implements KHRDebugCallback.Handler {
    public void handleMessage(int source, int type, int id, int severity, String message) {

        if(type == GL43.GL_DEBUG_TYPE_PUSH_GROUP || type == GL43.GL_DEBUG_TYPE_POP_GROUP) {
            return;
        }

        String source_s;
        switch (source) {
            case 33350:
                source_s = "API";
                break;
            case 33351:
                source_s = "WINDOW_SYSTEM";
                break;
            case 33352:
                source_s = "SHADER_COMPILER";
                break;
            case 33353:
                source_s = "THIRD_PARTY";
                break;
            case 33354:
                source_s = "APPLICATION";
                break;
            case 33355:
                source_s = "OTHER_SOURCE";
                break;
            default:
                source_s = this.printUnknownToken(source);
        }

        String severity_s;
        switch (severity) {
            case 33387:
                severity_s = "NOTIFICATION";
                break;
            case 37190:
                severity_s = "HIGH";
                break;
            case 37191:
                severity_s = "MEDIUM";
                break;
            case 37192:
                severity_s = "LOW";
                break;
            default:
                severity_s = this.printUnknownToken(severity);
        }

        String type_s;
        switch (type) {
            case 33356:
                type_s = "ERROR";
                log.error("[GL] [{}/{}] [{}] {}", type_s, source_s, severity_s, message.trim());
                return;
            case 33357:
                type_s = "DEPRECATED_BEHAVIOR";
                break;
            case 33358:
                type_s = "UNDEFINED_BEHAVIOR";
                break;
            case 33359:
                type_s = "PORTABILITY";
                break;
            case 33360:
                type_s = "PERFORMANCE";
                break;
            case 33361:
                type_s = "OTHER_TYPE";
                break;
            case 33384:
                type_s = "MARKER";
                break;
            default:
                type_s = this.printUnknownToken(type);
        }

        log.info("[GL] [{} {} {}] {}", type_s, source_s, severity_s, message.trim());

    }

    private String printUnknownToken(int token) {
        return "Unknown (0x" + Integer.toHexString(token).toUpperCase() + ")";
    }
}
