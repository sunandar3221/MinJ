# MinJ - Game 3D Voxel Ala Minecraft Berbasis Java

**MinJ** adalah game sandbox voxel 3D ala Minecraft klasik yang dibuat dari nol (*from scratch*) menggunakan bahasa pemrograman **Java murni (Pure Java)** tanpa ketergantungan library eksternal (tanpa OpenGL/LWJGL berat, bebas DLL issues), berjalan langsung menggunakan Java standard library (AWT/Swing + Software 3D Rasterizer dengan Z-Buffer).

---

## Fitur Utama

1. **Dunia Voxel 3D & Infinite Chunks**:
   - Terrain generation prosedural menggunakan algoritma **Multi-Octave Perlin Noise** (perbukitan, lembah, pantai berpasir, dan perairan).
   - Berbagai jenis balok voxel:
     - Rumput (*Grass Block*)
     - Tanah (*Dirt*)
     - Batu (*Stone*)
     - Batu Bulat (*Cobblestone*)
     - Kayu Papan (*Oak Planks*)
     - Batang Kayu (*Oak Log*)
     - Dedaunan (*Leaves*)
     - Kaca (*Glass*)
     - Pasir (*Sand*)
     - Batu Bata (*Bricks*)
     - Bedrock
     - Air (*Water*)
     - TNT
     - Bijih Tambang (*Coal Ore*, *Iron Ore*, *Gold Ore*)
     - Rak Buku (*Bookshelf*)
   - Pepohonan alami (pohon kayu dengan dedaunan rimbun).
   - Generasi bijih tambang di bawah tanah.

2. **Mesin Grafis 3D Retro**:
   - Software 3D Perspective Rasterizer dengan Z-Buffering.
   - Texturing prosedural 16x16 piksel khas Minecraft.
   - **Smooth Ambient Occlusion (AO)**: bayangan sudut realistis di tepian balok.
   - **Directional Shading**: pencahayaan dinamis pada tiap sisi balok.
   - **Distance Fog**: efek kabut cakrawala yang membaur halus.
   - Langit dinamis dengan matahari dan garis cakrawala.
   - Hand model 3D di sudut layar kanan bawah dengan animasi ayunan pukulan (*swing animation*).

3. **Fisika & Kontrol Karakter First-Person**:
   - Kamera First-Person halus dengan penguncian mouse (*Mouse Lock*).
   - Deteksi tabrakan AABB (*Axis-Aligned Bounding Box Collision*).
   - Gravitasi, lompatan, berjalan, dan efek *head bobbing*.
   - **Creative Flight Mode**: aktifkan/nonaktifkan mode terbang bebas dengan tombol `F`.

4. **Interaksi Balok & UI**:
   - **Raycasting Amanatides-Woo DDA**: pendeteksian target balok dan sisi balok dengan presisi tinggi.
   - Garis kawat pembatas (*wireframe outline*) pada balok yang sedang dibidik.
   - **Hancurkan Balok** dengan Klik Kiri.
   - **Taruh Balok** dengan Klik Kanan (tidak bisa menaruh balok di dalam tubuh pemain).
   - **Hotbar 9 Slot**: pilih balok dengan tombol angka `1` - `9` atau *scroll wheel* mouse.
   - **Efek Suara Sintetis (Procedural Audio)**: efek suara hancur balok, pasang balok, langkah kaki, dan lompatan langsung disintesis melalui Java Sound API tanpa memerlukan file audio eksternal.
   - **HUD / F3 Debug Info**: menampilkan FPS, koordinat X/Y/Z, arah mata angin, dan status mode terbang.

---

## Kontrol Permainan

| Tombol | Aksi |
|---|---|
| **Klik Layar** | Mulai bermain & kunci kursor mouse ke dalam game |
| **W, A, S, D** | Berjalan maju, mundur, kiri, kanan |
| **Mouse** | Menoleh / melihat ke sekeliling (FPS Look) |
| **Spasi** | Melompat (atau terbang naik jika mode terbang aktif) |
| **Shift** | Jongkok (atau terbang turun jika mode terbang aktif) |
| **Klik Kiri** | Menghancurkan balok (*Break Block*) |
| **Klik Kanan** | Menaruh balok yang sedang dipilih (*Place Block*) |
| **1 - 9** | Memilih balok dari hotbar |
| **Scroll Mouse** | Mengganti pilihan balok pada hotbar |
| **F** | Mengaktifkan/menonaktifkan mode terbang (*Creative Flight*) |
| **ESC** | Melepaskan kursor mouse / jeda |

---

## Cara Menjalankan

### Cara 1: Menggunakan Script Batch (Paling Praktis di Windows)
Cukup klik dua kali file:
```
run.bat
```

### Cara 2: Menjalankan File JAR yang Sudah Jadi
Buka Command Prompt atau PowerShell di folder ini, lalu jalankan:
```bash
java -jar MinJ.jar
```

### Cara 3: Kompilasi dan Jalankan Manual via Terminal
```bash
# Kompilasi
javac -d bin src\com\minj\*.java

# Jalankan
java -cp bin com.minj.Main
```
