package org.hwyl.sexytopo.control.calibration;


/**
 * Calibration algorithm by Beat Heeb as written for PocketTopo.
 * Translated into Java from C#. Any mistakes are probably mine.
 * Kindly made available for use in SexyTopo by Beat Heeb.
 */
public class CalibAlgorithm {

    private static final float FV = 24000;
    private static final float FM = 16384;
    private static final float FN = 2796; // 2^26 / FV
    private static final float EPS = 1.0E-6F;
    private static final int MAX_IT = 200;

    //result
    public static Matrix aG, aM;
    public static Vector bG, bM;
    public static Vector nl; // nonlinearity coefficients


    public static void AddValues(int gx, int gy, int gz, int mx, int my, int mz,
                                 Vector[] g, Vector[] m, int idx) {
        g[idx] = new Vector(gx / FV, gy / FV, gz / FV);
        m[idx] = new Vector(mx / FV, my / FV, mz / FV);
    }


    // helpers
    public static Vector[] OptVectors(Vector gr, Vector mr, float alpha) {
        Vector no = Vector.Normalized(gr.crossProduct(mr));  // plane normal
        float s = (float)Math.sin(alpha);
        float c = (float)Math.cos(alpha);
        Vector gx = Vector.Normalized(mr.times(c).plus((mr.crossProduct(no)).times(s)).plus(gr));
        Vector mx = (gx.times(c)).plus((no.crossProduct(gx)).times(s));
        return new Vector[] {gx, mx};
    }

    public static Vector[] TurnVectors(Vector gxp, Vector mxp, Vector gr, Vector mr) {
        float s = gr.z * gxp.y - gr.y * gxp.z + mr.z * mxp.y - mr.y * mxp.z;
        float c = gr.y * gxp.y + gr.z * gxp.z + mr.y * mxp.y + mr.z * mxp.z;
        float a = (float)Math.atan2(s, c);
        Vector gx = gxp.TurnX(a);
        Vector mx = mxp.TurnX(a);
        return new Vector[]{gx, mx};
    }


    private static Object[] CheckOverflow(Matrix m, Vector v) {
        float max = Math.max(Matrix.MaxDiff(m, Matrix.getZero()) * FM,
                Vector.MaxDiff(v, Vector.getZero()) * FV);
        if (max > Short.MAX_VALUE) {
            m = m.times((Short.MAX_VALUE / max));
            v = v.times((Short.MAX_VALUE / max));
        }
        return new Object[] {m, v};
    }

    // saturation for non-linearity coefficients
    private static float Saturate(float x) {
        if (x > 127 / FN) x = 127 / FN;
        else if (x < -127 / FN) x = -127 / FN;
        return x;
    }

    /*
     * g: the gravity sensor values divided by FV
     * m: the magnetic sensor values divided by FV
     * delta: the average error in %
     * return value: the number of iterations used
     */
    public static int Optimize(
            Vector[] g, Vector[] m, MutableFloat delta, boolean useNonLinearity) {
        int num = g.length;
        Vector[] gr = new Vector[num];
        Vector[] mr = new Vector[num];
        Vector[] gx = new Vector[num];
        Vector[] mx = new Vector[num];
        Vector[] gl = new Vector[num];  // linearized g values
        Matrix[] gs = new Matrix[num];  // Diag(g^2 - 1/2)
        Matrix aG0, aM0;
        float invNum = 1.0F / num;
        Vector sumG = Vector.getZero();
        Vector sumM = Vector.getZero();
        Matrix sumG2 = Matrix.getZero();
        Matrix sumM2 = Matrix.getZero();
        float sa = 0, ca = 0;
        for (int i = 0; i < num; i++) {
            // sum up g x m for initial alpha
            sa += Vector.Abs(g[i].crossProduct(m[i])); // cross product
            ca += g[i].times(m[i]); // dot product
            // sum up g, m, g^2, & m^2
            sumG = sumG.plus(g[i]);
            sumM = sumM.plus(m[i]);
            sumG2 = sumG2.plus((g[i].outerProduct(g[i]))); // outer product
            sumM2 = sumM2.plus((m[i].outerProduct(m[i])));
            // initialize non-linearity data
            gl[i] = g[i];
            gs[i] = Matrix.getZero();
            gs[i].x.x = g[i].x * g[i].x - 0.5F;
            gs[i].y.y = g[i].y * g[i].y - 0.5F;
            gs[i].z.z = g[i].z * g[i].z - 0.5F;
        }
        float alpha = (float)Math.atan2(sa, ca);
        Vector avG = sumG.times(invNum); // average g
        Vector avM = sumM.times(invNum); // average m
        Matrix invG = Matrix.Inverse(sumG2.minus(sumG.outerProduct(avG)));
        Matrix invM = Matrix.Inverse(sumM2.minus(sumM.outerProduct(avM)));
        nl = Vector.getZero();
        aG = aM = Matrix.getOne();
        bG = Vector.getZero().minus(avG); // use negative average as initial offset
        bM = Vector.getZero().minus(avM);
        int it = 0; // number of iterations
        do {
            // get gr & mr from g, m, aG, aM, bG, & bM
            for (int i = 0; i < num; i++) {
                gr[i] = (aG.times(gl[i])).plus(bG);  // gl instead of g !
                mr[i] = (aM.times(m[i])).plus(bM);
            }
            sa = ca = 0;
            for (int i = 0; i < num; i++) {
                // get optimal gx & mx from gr & mr
                if (i < 16) { // equidirectional sample groups
                    Vector grp = Vector.getZero();
                    Vector mrp = Vector.getZero();
                    int first = i;
                    for (; i < first + 4; i++) {
                        // match gr & mr to first gr & mr
                        Vector[] vectors = TurnVectors(gr[i], mr[i], gr[first], mr[first]);
                        Vector gt = vectors[0]; Vector mt = vectors[1];
                        grp = grp.plus(gt);
                        mrp = mrp.plus(mt);
                    }
                    // get optimal matched gx & mx from sum of matched gr & mr
                    Vector[] optVectors = OptVectors(grp, mrp, alpha);
                    Vector gxp = optVectors[0];
                    Vector mxp = optVectors[1];
                    // alpha calculation
                    sa += Vector.Abs(mrp.crossProduct(gxp));
                    ca += mrp.times(gxp);
                    for (i = first; i < first + 4; i++) {
                        // get optimal gx & mx from matched gx & mx
                        Vector[] turnVectors = TurnVectors(gxp, mxp, gr[i], mr[i]);
                        gx[i] = turnVectors[0]; mx[i] = turnVectors[1];

                    }
                    i--;
                } else { // individual sample
                    Vector[] vectors = OptVectors(gr[i], mr[i], alpha);
                    gx[i] = vectors[0];
                    mx[i] = vectors[1];
                    // alpha calculation
                    sa += Vector.Abs(mr[i].crossProduct(gx[i]));
                    ca += mr[i].times(gx[i]);
                }
            }
            alpha = (float)Math.atan2(sa, ca);
            // get aG & aM from g, m, gx, & mx
            Vector avGx = Vector.getZero();
            Vector avMx = Vector.getZero();
            Matrix sumGxG = Matrix.getZero();
            Matrix sumMxM = Matrix.getZero();
            for (int i = 0; i < num; i++) {
                avGx = avGx.plus(gx[i]);
                avMx = avMx.plus(mx[i]);
                sumGxG = sumGxG.plus(gx[i].outerProduct(gl[i])); // outer product // gl instead of g !
                sumMxM = sumMxM.plus(mx[i].outerProduct(m[i]));
            }
            // get new aG & aM
            aG0 = aG; aM0 = aM;
            avGx = avGx.times(invNum); // average gx
            avMx = avMx.times(invNum);
            aG = (sumGxG.minus(avGx.outerProduct(sumG))).times(invG);
            aM = (sumMxM.minus(avMx.outerProduct(sumM))).times(invM);
            // enforce symmetric aG(y,z)
            aG.y.z = aG.z.y = (aG.y.z + aG.z.y) * 0.5F;
            // get new bG & bM
            bG = avGx.minus(aG.times(avG));
            bM = avMx.minus(aM.times(avM));
            if (useNonLinearity) {
                // get new non-linearity coefficients
                Matrix psum = Matrix.getZero();
                Vector qsum = Vector.getZero();
                for (int i = 0; i < num; i++) {
                    Matrix p = aG.times(gs[i]);
                    Vector q = gx[i].minus(aG.times(g[i])).minus(bG);
                    Matrix pt = Matrix.Transposed(p);
                    psum = psum.plus(pt.times(p));
                    qsum = qsum.plus(pt.times(q));
                }
                nl = Matrix.Inverse(psum).times(qsum);
                nl.x = Saturate(nl.x);
                nl.y = Saturate(nl.y);
                nl.z = Saturate(nl.z);
                // recalculate linearized g values
                sumG = Vector.getZero();
                sumG2 = Matrix.getZero();
                for (int i = 0; i < num; i++) {
                    gl[i] = g[i].plus(gs[i].times(nl));
                    // sum up g & g^2
                    sumG = sumG.plus(gl[i]);
                    sumG2 = sumG2.plus(gl[i].outerProduct(gl[i])); // outer product
                }
                avG = sumG.times(invNum); // average g
                invG = Matrix.Inverse(sumG2.minus(sumG.outerProduct(avG)));
            }
            it++;
        } while (it < MAX_IT && Math.max(Matrix.MaxDiff(aG, aG0), Matrix.MaxDiff(aM, aM0)) > EPS);
        Object[] overflow = CheckOverflow(aG, bG);
        aG = (Matrix)overflow[0]; bG = (Vector)overflow[1];

        Object[] overflow2 = CheckOverflow(aM, bM);
        aM = (Matrix)overflow2[0]; bM = (Vector)overflow2[1];

        delta.value = 0;
        for (int i = 0; i < num; i++) {
            Vector dg = gx[i].minus(gr[i]);
            Vector dm = mx[i].minus(mr[i]);
            delta.value += (dg.times(dg)) + (dm.times(dm));
        }
        delta.value = (float)Math.sqrt(delta.value / num) * 100;
        return it;
    }


    private static void PutCoeff(byte[] data, int index, float value) {
        int coeff = (int)Math.round(value);
        data[index] = (byte)coeff;
        data[index + 1] = (byte)(coeff >> 8);
    }

    /*
     * returns byte sequence to be written to address 0x8010 - 0x803F/0x8043
     */
    public static byte[] GetCoeff(boolean useNonLinearity) {
        byte[] data = new byte[useNonLinearity ? 52 : 48];
        PutCoeff(data, 0, bG.x * FV);
        PutCoeff(data, 2, aG.x.x * FM);
        PutCoeff(data, 4, aG.x.y * FM);
        PutCoeff(data, 6, aG.x.z * FM);
        PutCoeff(data, 8, bG.y * FV);
        PutCoeff(data, 10, aG.y.x * FM);
        PutCoeff(data, 12, aG.y.y * FM);
        PutCoeff(data, 14, aG.y.z * FM);
        PutCoeff(data, 16, bG.z * FV);
        PutCoeff(data, 18, aG.z.x * FM);
        PutCoeff(data, 20, aG.z.y * FM);
        PutCoeff(data, 22, aG.z.z * FM);
        PutCoeff(data, 24, bM.x * FV);
        PutCoeff(data, 26, aM.x.x * FM);
        PutCoeff(data, 28, aM.x.y * FM);
        PutCoeff(data, 30, aM.x.z * FM);
        PutCoeff(data, 32, bM.y * FV);
        PutCoeff(data, 34, aM.y.x * FM);
        PutCoeff(data, 36, aM.y.y * FM);
        PutCoeff(data, 38, aM.y.z * FM);
        PutCoeff(data, 40, bM.z * FV);
        PutCoeff(data, 42, aM.z.x * FM);
        PutCoeff(data, 44, aM.z.y * FM);
        PutCoeff(data, 46, aM.z.z * FM);
        if (useNonLinearity) {
            int nlx = Math.round(nl.x * FN);
            int nly = Math.round(nl.y * FN);
            int nlz = Math.round(nl.z * FN);
            data[48] = (byte)(nlx - 1);
            data[49] = (byte)(nly - 1);
            data[50] = (byte)(nlz - 1);
            data[51] = (byte)0xFF;
        }
        return data;
    }


}
