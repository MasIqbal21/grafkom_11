package com.grafkom.visualrealism;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * MeshFactory
 * -----------
 * Membangkitkan geometri UV-Sphere (mesh bermateri dengan Normal Vertex & Koordinat UV,
 * lihat Tugas 1.a) dan bidang lantai (ground plane, untuk menerima bayangan pada Indikator 5).
 *
 * Untuk Indikator 2 (Flat vs Smooth Shading), kelas ini secara eksplisit mengimplementasikan
 * rumus "vertex normal averaging":
 *
 *      N_vertex = ( Σ N_face_i ) / || Σ N_face_i ||
 *
 * sehingga mesh SMOOTH memakai normal vertex hasil rata-rata, sedangkan mesh FLAT
 * memakai satu normal FACE yang sama untuk ketiga vertex penyusun tiap segitiga
 * (facet shading, tanpa interpolasi antar wajah).
 */
public final class MeshFactory {

    private MeshFactory() {}

    /** Data geometri mentah sebelum normal dihitung: posisi, UV, dan daftar index segitiga. */
    private record RawGeometry(float[] positions, float[] uvs, int[] indices) {}

    // ---------------------------------------------------------------------
    // 1) UV-Sphere
    // ---------------------------------------------------------------------

    private static RawGeometry buildSphereRaw(float radius, int sectors, int stacks) {
        List<Float> positions = new ArrayList<>();
        List<Float> uvs = new ArrayList<>();

        for (int i = 0; i <= stacks; i++) {
            // phi: dari +90 derajat (kutub atas) ke -90 derajat (kutub bawah)
            double phi = Math.PI / 2.0 - Math.PI * i / stacks;
            float y = (float) (radius * Math.sin(phi));
            double xyPlaneRadius = radius * Math.cos(phi);

            for (int j = 0; j <= sectors; j++) {
                double theta = 2.0 * Math.PI * j / sectors;
                float x = (float) (xyPlaneRadius * Math.cos(theta));
                float z = (float) (xyPlaneRadius * Math.sin(theta));

                positions.add(x);
                positions.add(y);
                positions.add(z);

                uvs.add((float) j / sectors);          // u in [0,1]
                uvs.add((float) i / stacks);            // v in [0,1]
            }
        }

        List<Integer> indices = new ArrayList<>();
        int rowStride = sectors + 1;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < sectors; j++) {
                int k1 = i * rowStride + j;
                int k2 = k1 + rowStride;

                // Urutan vertex dipilih agar winding CCW saat dilihat dari LUAR bola
                // (diverifikasi numerik: (b-a) x (c-a) searah dengan normal keluar).
                // Ini wajib benar agar Back-Face Culling (GL_CCW front face) tidak
                // membuang seluruh permukaan bola yang terlihat.
                if (i != 0) {
                    indices.add(k1);
                    indices.add(k1 + 1);
                    indices.add(k2);
                }
                if (i != (stacks - 1)) {
                    indices.add(k1 + 1);
                    indices.add(k2 + 1);
                    indices.add(k2);
                }
            }
        }

        return new RawGeometry(toFloatArray(positions), toFloatArray(uvs), toIntArray(indices));
    }

    /** Sphere dengan normal vertex = rata-rata normal face terhubung (Smooth Shading). */
    public static Mesh createSmoothSphere(float radius, int sectors, int stacks) {
        RawGeometry raw = buildSphereRaw(radius, sectors, stacks);
        float[] vertexNormals = computeAveragedVertexNormals(raw.positions(), raw.indices());
        float[] interleaved = interleave(raw.positions(), vertexNormals, raw.uvs());
        return new Mesh(interleaved, raw.indices());
    }

    /** Sphere dengan satu normal FACE per segitiga, vertex diduplikasi (Flat Shading). */
    public static Mesh createFlatSphere(float radius, int sectors, int stacks) {
        RawGeometry raw = buildSphereRaw(radius, sectors, stacks);
        float[] interleaved = duplicateWithFaceNormals(raw.positions(), raw.uvs(), raw.indices());
        return new Mesh(interleaved);
    }

    // ---------------------------------------------------------------------
    // 2) Ground plane (bidang lantai, penerima bayangan)
    // ---------------------------------------------------------------------

    public static Mesh createGroundPlane(float halfSize, float uvRepeat) {
        // Normal menghadap +Y, UV di-repeat agar tekstur ubin berulang (GL_REPEAT, lihat Texture2D).
        float ny = 1f;
        float[] vertices = {
                -halfSize, 0, -halfSize,   0, ny, 0,   0f, 0f,
                 halfSize, 0, -halfSize,   0, ny, 0,   uvRepeat, 0f,
                 halfSize, 0,  halfSize,   0, ny, 0,   uvRepeat, uvRepeat,
                -halfSize, 0,  halfSize,   0, ny, 0,   0f, uvRepeat,
        };
        // Winding CCW saat dilihat dari atas (+Y, arah normal lantai) - diverifikasi numerik,
        // supaya lantai tidak ikut ter-cull saat Back-Face Culling aktif.
        int[] indices = { 0, 2, 1, 0, 3, 2 };
        return new Mesh(vertices, indices);
    }

    // ---------------------------------------------------------------------
    // Helper: N_vertex = (Σ N_face) / ||Σ N_face||  (rata-rata normal face pada tiap vertex)
    // ---------------------------------------------------------------------

    private static float[] computeAveragedVertexNormals(float[] positions, int[] indices) {
        int vertexCount = positions.length / 3;
        Vector3f[] accum = new Vector3f[vertexCount];
        for (int i = 0; i < vertexCount; i++) accum[i] = new Vector3f();

        for (int t = 0; t < indices.length; t += 3) {
            int ia = indices[t], ib = indices[t + 1], ic = indices[t + 2];
            Vector3f a = vertexAt(positions, ia);
            Vector3f b = vertexAt(positions, ib);
            Vector3f c = vertexAt(positions, ic);

            Vector3f edge1 = new Vector3f(b).sub(a);
            Vector3f edge2 = new Vector3f(c).sub(a);
            Vector3f faceNormal = new Vector3f(edge1).cross(edge2); // belum dinormalisasi -> bobot luas segitiga

            accum[ia].add(faceNormal);
            accum[ib].add(faceNormal);
            accum[ic].add(faceNormal);
        }

        float[] normals = new float[positions.length];
        for (int i = 0; i < vertexCount; i++) {
            Vector3f n = accum[i];
            if (n.lengthSquared() > 1e-12f) n.normalize();
            else n.set(0, 1, 0);
            normals[i * 3] = n.x;
            normals[i * 3 + 1] = n.y;
            normals[i * 3 + 2] = n.z;
        }
        return normals;
    }

    private static float[] duplicateWithFaceNormals(float[] positions, float[] uvs, int[] indices) {
        // 8 float per vertex (pos3 + normal3 + uv2), 3 vertex per segitiga
        float[] out = new float[(indices.length) * 8];
        int w = 0;
        for (int t = 0; t < indices.length; t += 3) {
            int ia = indices[t], ib = indices[t + 1], ic = indices[t + 2];
            Vector3f a = vertexAt(positions, ia);
            Vector3f b = vertexAt(positions, ib);
            Vector3f c = vertexAt(positions, ic);

            Vector3f faceNormal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
            if (faceNormal.lengthSquared() > 1e-12f) faceNormal.normalize();

            int[] idx = { ia, ib, ic };
            for (int corner : idx) {
                out[w++] = positions[corner * 3];
                out[w++] = positions[corner * 3 + 1];
                out[w++] = positions[corner * 3 + 2];

                out[w++] = faceNormal.x;
                out[w++] = faceNormal.y;
                out[w++] = faceNormal.z;

                out[w++] = uvs[corner * 2];
                out[w++] = uvs[corner * 2 + 1];
            }
        }
        return out;
    }

    private static Vector3f vertexAt(float[] positions, int index) {
        return new Vector3f(positions[index * 3], positions[index * 3 + 1], positions[index * 3 + 2]);
    }

    private static float[] interleave(float[] positions, float[] normals, float[] uvs) {
        int vertexCount = positions.length / 3;
        float[] out = new float[vertexCount * 8];
        for (int i = 0; i < vertexCount; i++) {
            out[i * 8] = positions[i * 3];
            out[i * 8 + 1] = positions[i * 3 + 1];
            out[i * 8 + 2] = positions[i * 3 + 2];

            out[i * 8 + 3] = normals[i * 3];
            out[i * 8 + 4] = normals[i * 3 + 1];
            out[i * 8 + 5] = normals[i * 3 + 2];

            out[i * 8 + 6] = uvs[i * 2];
            out[i * 8 + 7] = uvs[i * 2 + 1];
        }
        return out;
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }
}
