/*
 * Copyright (c) 2011, Novyon Events
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * @author Anthyon
 */
package com.jme3.terrain.noise.filter;

import java.nio.FloatBuffer;

public class ThermalErodeFilter extends AbstractFilter {

    private float talus;
    private float c;

    public ThermalErodeFilter setC(float c) {
        this.c = c;
        return this;
    }

    public ThermalErodeFilter setTalus(float talus) {
        this.talus = talus;
        return this;
    }

    @Override
    public int getMargin(int size, int margin) {
        return super.getMargin(size, margin) + 1;
    }

    @Override
    public FloatBuffer filter(float sx, float sy, float base, FloatBuffer buffer, int workSize) {
        float[] ga = buffer.array();
        float[] sa = new float[workSize * workSize];

        final int[] idxrel = { -workSize - 1, -workSize + 1, workSize - 1, workSize + 1 };
        final int nrel = idxrel.length;
        final float[] deltas = new float[nrel]; // reuse to avoid repeated allocations

        final int gaLen = ga.length;
        final float talusLocal = this.talus;
        final float cLocal = this.c;

        for (int y = 0; y < workSize; y++) {
            for (int x = 0; x < workSize; x++) {
                int idx = y * workSize + x;
                ga[idx] += sa[idx];
                sa[idx] = 0f;

                // clear small reused array (length is tiny, manual clear is optimal)
                for (int j = 0; j < nrel; j++) {
                    deltas[j] = 0f;
                }

                float deltaMax = talusLocal;
                float deltaTotal = 0f;

                for (int j = 0; j < nrel; j++) {
                    int nIdx = idx + idxrel[j];
                    if (nIdx > 0 && nIdx < gaLen) {
                        float dj = ga[idx] - ga[nIdx];
                        if (dj > talusLocal) {
                            deltas[j] = dj;
                            deltaTotal += dj;
                            if (dj > deltaMax) {
                                deltaMax = dj;
                            }
                        }
                    }
                }

                if (deltaTotal != 0f) {
                    for (int j = 0; j < nrel; j++) {
                        float dj = deltas[j];
                        if (dj != 0f) {
                            int nIdx = idx + idxrel[j];
                            float d = cLocal * (deltaMax - talusLocal) * dj / deltaTotal;
                            if (d > ga[idx] + sa[idx]) {
                                d = ga[idx] + sa[idx];
                            }
                            sa[idx] -= d;
                            sa[nIdx] += d;
                            // deltas[j] will be cleared at next iteration's start
                        }
                    }
                }
            }
        }

        return buffer;
    }

}
