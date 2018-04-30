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

import eu.matejkormuth.math.vectors.Vector2f;
import eu.matejkormuth.math.vectors.Vector3f;
import lombok.experimental.UtilityClass;

import java.util.HashMap;

@UtilityClass
public class Syntax {

    public final static class Cfg {
        private final HashMap<String, Object> hashMap;

        private Cfg(HashMap<String, Object> hashMap) {
            this.hashMap = hashMap;
        }

        public boolean has(String key) {
            return hashMap.containsKey(key);
        }

        public <T> T get(String key) {
            return cast(hashMap.get(key));
        }

        public <T> T get(String key, T def) {
            return hashMap.get(key) == null ? def : cast(hashMap.get(key));
        }

        public float getFloat(String key) {
            return getFloat(key, 0);
        }

        public float getFloat(String key, float def) {
            return hashMap.get(key) == null ? def : (float) hashMap.get(key);
        }

        @SuppressWarnings({"unchecked"})
        private static <T> T cast(Object o) {
            return (T) o;
        }
    }

    public static Cfg cfg(String key1, Object value1) {
        HashMap<String, Object> hm = new HashMap<>(1);
        hm.put(key1, value1);
        return new Cfg(hm);
    }

    public static Cfg cfg(String key1, Object value1,
                          String key2, Object value2) {
        HashMap<String, Object> hm = new HashMap<>(2);
        hm.put(key1, value1);
        hm.put(key2, value2);
        return new Cfg(hm);
    }

    public static Cfg cfg(String key1, Object value1,
                          String key2, Object value2,
                          String key3, Object value3) {
        HashMap<String, Object> hm = new HashMap<>(3);
        hm.put(key1, value1);
        hm.put(key2, value2);
        hm.put(key3, value3);
        return new Cfg(hm);
    }

    public static Cfg cfg(String key1, Object value1,
                          String key2, Object value2,
                          String key3, Object value3,
                          String key4, Object value4) {
        HashMap<String, Object> hm = new HashMap<>(4);
        hm.put(key1, value1);
        hm.put(key2, value2);
        hm.put(key3, value3);
        hm.put(key4, value4);
        return new Cfg(hm);
    }

    public static Cfg cfg(String key1, Object value1,
                          String key2, Object value2,
                          String key3, Object value3,
                          String key4, Object value4,
                          String key5, Object value5) {
        HashMap<String, Object> hm = new HashMap<>(5);
        hm.put(key1, value1);
        hm.put(key2, value2);
        hm.put(key3, value3);
        hm.put(key4, value4);
        hm.put(key5, value5);
        return new Cfg(hm);
    }

    public static Cfg cfg(Object... keysValues) {
        if ((keysValues.length % 2) != 0) {
            throw new IllegalArgumentException("Invalid amount of arguments! Value missing for a key!");
        }

        HashMap<String, Object> hm = new HashMap<>(keysValues.length / 2);
        for (int i = 0; i < keysValues.length; i += 2) {
            hm.put(keysValues[i].toString(), keysValues[i + 1]);
        }
        return new Cfg(hm);
    }

    public static Vector3f vec3(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    public static Vector3f vec3(float x) {
        return new Vector3f(x, x, x);
    }

    public static Vector2f vec2(float x, float y) {
        return new Vector2f(x, y);
    }

    public static <T> T or(T actual, T defaultValue) {
        return actual != null ? actual : defaultValue;
    }

}
