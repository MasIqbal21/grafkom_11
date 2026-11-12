# 3D Visual Realism Suite

Tugas Praktikum Grafika Komputer — Minggu ke-11: *Rendering Faces pada Visual Realism*
(Java 17+ OOP, LWJGL 3, JOML, Modern OpenGL 3.3 Core / GLSL 330).

Memperluas Java 3D Viewer (Minggu ke-10) dengan 5 indikator wajib:

| # | Indikator | Implementasi |
|---|-----------|---------------|
| 1 | Shading Models (Phong & Blinn-Phong) | `scene.frag` (uniform `useBlinnPhong`) |
| 2 | Flat vs Smooth Shading | `MeshFactory` (averaged vs per-face normals) |
| 3 | Hidden Surface Removal | `HSRManager` (Z-Buffer + Back-Face Culling) |
| 4 | Texture Mapping to Faces | `Texture2D` (STBImage, Nearest vs Bilinear+Mipmap) |
| 5 | Shadows of Objects | `ShadowMapFBO` + 2-pass shadow mapping, PCF, slope-scaled bias |

## Cara menjalankan

Butuh **JDK 17+** dan **Maven**, serta koneksi internet (untuk mengunduh LWJGL/JOML
dari Maven Central saat pertama kali build).

```bash
mvn compile exec:java
```

Atau build jar mandiri (semua dependency digabung) lalu jalankan:

```bash
mvn package
java -jar target/visual-realism-suite.jar
```

**macOS**: GLFW mewajibkan thread utama, tambahkan opsi JVM:
```bash
mvn package
java -XstartOnFirstThread -jar target/visual-realism-suite.jar
```
(Untuk `exec:java` di macOS, tambahkan konfigurasi `<argument>` exec-maven-plugin atau
jalankan via `java -XstartOnFirstThread -cp target/classes:... com.grafkom.visualrealism.Main`
setelah `mvn dependency:build-classpath`.)

## Kontrol

| Tombol | Fungsi | Indikator |
|---|---|---|
| `W A S D` | Gerak kamera maju/kiri/mundur/kanan | - |
| `Q / E` | Turun / naik | - |
| Mouse | Lihat sekeliling (mouse-look) | - |
| `Shift` (tahan) | Gerak lebih cepat | - |
| `TAB` | Lepas/kunci kursor mouse | - |
| **`B`** | Toggle **Phong <-> Blinn-Phong** | 1 |
| **`F`** | Toggle **Flat <-> Smooth Shading** | 2 |
| **`Z`** | Toggle **Z-Buffer / Depth Test** on/off | 3 |
| **`C`** | Toggle **Back-Face Culling** on/off | 3 |
| **`T`** | Toggle **Texture Filtering** Nearest <-> Bilinear+Mipmap | 4 |
| `L` | Pause/lanjutkan orbit cahaya (memudahkan screenshot bayangan) | 5 |
| `ESC` | Keluar | - |

Status toggle saat ini selalu ditampilkan pada **judul window** (title bar), sehingga
terlihat jelas pada setiap screenshot untuk laporan praktikum.

## Struktur kode (OOP)

```
com.grafkom.visualrealism
├── Main.java              entry point
├── VisualRealismSuite.java  aplikasi utama: render loop 2-pass, wiring semua komponen
├── Window.java             GLFW window + input (keyboard edge-trigger, mouse delta)
├── Camera.java             kamera fly (JOML) -> View & Projection matrix
├── Shader.java             compile/link GLSL + utilitas uniform
├── Mesh.java               VAO/VBO/EBO generik (indexed & non-indexed)
├── MeshFactory.java        generator UV-Sphere (smooth & flat) + ground plane,
│                           termasuk implementasi rumus averaging normal vertex
├── Texture2D.java          load tekstur (STBImage) + ganti filter mode runtime
├── HSRManager.java         toggle Z-Buffer Depth Test & Back-Face Culling
└── ShadowMapFBO.java       depth framebuffer untuk Shadow Mapping 2-pass

src/main/resources/shaders
├── scene.vert / scene.frag         Pass 2 (Lighting): Phong/Blinn-Phong,
│                                   texture, shadow sampling + PCF + slope bias
└── shadow_depth.vert / .frag       Pass 1 (Depth): render dari sudut pandang cahaya
```

## Catatan implementasi penting

- **Winding order** mesh (sphere & ground plane) sudah diverifikasi numerik agar
  CCW terhadap normal keluar — penting supaya Back-Face Culling (`GL_CCW` front face)
  tidak membuang permukaan yang seharusnya terlihat.
- **Flat shading** diimplementasikan dengan menduplikasi vertex per segitiga dan
  memberi ketiganya satu normal *face* yang sama (bukan trik qualifier `flat` GLSL),
  supaya efek "facet"-nya terlihat jelas dan sesuai rumus di modul teori.
- **Smooth shading** memakai rumus eksplisit `N_vertex = (Σ N_face) / ||Σ N_face||`
  (lihat `MeshFactory.computeAveragedVertexNormals`), bukan normal analitik bola,
  agar sesuai dengan algoritma yang diajarkan di modul.
- **Shadow mapping** memakai Percentage-Closer Filtering (PCF) 3x3 dan
  *slope-scaled bias* (`max(0.05*(1-N.L), 0.005)`) untuk mengurangi Shadow Acne
  tanpa menimbulkan Peter-Panning yang parah.

## Keterbatasan lingkungan pengembangan

Proyek ini ditulis & diperiksa di lingkungan cloud tanpa GPU/display fisik, sehingga
**tidak bisa dijalankan atau di-screenshot langsung di sini**. Yang sudah dilakukan
untuk memastikan kebenarannya:

1. Seluruh 11 file `.java` berhasil **di-compile bersih (javac, 0 error/warning)**
   terhadap stub API yang meniru signature asli LWJGL 3.3.3 / JOML 1.10.5.
2. **Winding order** geometri sphere & ground plane diverifikasi secara numerik
   (cross product vs arah normal keluar) — lihat komentar di `MeshFactory.java`.
3. Logika shading, HSR, texture filtering, dan shadow mapping ditulis mengikuti
   persis rumus-rumus pada modul teori (Bagian I).

Jalankan `mvn compile exec:java` di komputer sendiri (dengan GPU & driver OpenGL 3.3+)
untuk melihat hasil render dan mengambil screenshot sesuai format laporan.
