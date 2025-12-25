# Blackhole EH (Event Horizon)
**Blackhole EH** is a research-oriented project exploring **lossless image storage and deduplication** 
based on block-level processing and reversible transforms.

The system converts images into fixed-size blocks, separates luminance and chrominance components, 
and stores images locally in a custom binary format (`.blho`) while uploading unique blocks 
to a server for deduplicated storage.

> ⚠️ **Project status**
> This project is in an **active R&D phase**.
> It is not yet optimized for compression efficiency and should be considered a research prototype rather than a production-ready archiver.

## Motivation
Traditional image formats (JPEG, PNG, etc.) are optimized for **single-file compression**, but they do not address:
* Cross-image redundancy
* Block-level deduplication across datasets
* Research into alternative lossless representations beyond entropy coding

Blackhole EH investigates whether **content-addressable, block-based storage** can be used 
as a foundation for future lossless image archival systems.

## Core Concepts
### 1. Lossless Block Decomposition
Images are split into **8×8 pixel blocks**.
Each pixel is converted from RGB into **Y/U/V components** using a **reversible integer transform (RCT)**.

This guarantees:
* Exact reversibility
* No rounding
* No loss of information

### 2. Block Components
For each 8×8 block:
* **Y (luma)** is stored as 8-bit values
* **U and V (chroma)** are stored as signed 16-bit values (packed as bytes)

Each component is processed **independently**.

## BLHO File Format (v2)
`.blho` files store the **structural description** of an image, not its pixel data.

### What `.blho` contains
* File header (`BLHO`, version 2)
* JSON metadata
* Lists of **unique SHA-256 hashes** for:
  * Y blocks
  * U blocks
  * V blocks
* Position maps referencing these hashes

### What `.blho` does NOT contain
* Raw block data
* Pixel values
* Compressed image bytes

This design allows `.blho` files to act as **manifests** that can reconstruct an image once the corresponding blocks are available.

## Processing Pipeline
1. **Image loading**
2. **Padding** to multiples of 8×8 (edge pixels replicated)
3. **Block splitting**
4. **RGB → RCT transform**
5. **SHA-256 hashing** of Y / U / V blocks
6. **Deduplication** within the image
7. **`.blho` file generation**
8. **Server check** for missing blocks
9. **Upload only missing blocks**

## Server-Side Deduplication
The server stores blocks indexed by:
* SHA-256 hash
* Block type (Y/U/V)

For each processed image:
* The client checks which hashes already exist
* Only missing blocks are uploaded
* Duplicate blocks across images are stored once

## Lossless Guarantee
All operations in Blackhole EH are **bit-exact**:
* Reversible integer color transform
* No quantization
* No floating-point arithmetic
* No entropy coding (yet)

Reconstruction is deterministic and fully lossless as long as:
* All referenced blocks are available
* Position maps are preserved

## Current Limitations
* `.blho` files are often **larger than the original JPEG**
* No entropy reduction beyond deduplication
* Position maps are not optimized
* No spatial prediction or delta coding (future research)

These limitations are **intentional** at this stage and are the subject of ongoing research.

## Research Directions (Planned)
The following topics are under investigation but **not implemented yet**:
* Spatial correlation analysis
* Delta (residual) representations
* Entropy estimation
* Hybrid block encoding (absolute vs delta)
* BLHO v3 format design

See project issues for detailed research tasks.

## Project Structure
Key components:
* `BlockSplitter`
  Splits images into padded 8×8 RCT blocks
* `BlhoWriter`
  Generates `.blho` files (v2 format)
* `FileProcessor`
  Orchestrates image processing and server interaction
* `BlockClient`
  Communicates with the block storage server

## Requirements
* Java **21+** (tested with Java 25)
* Maven
* Spring Boot 3.x

## Running the Project
Configure the image directory in `application.yml`, then run:

```bash
mvn spring-boot:run
```

The application will:
* Process all JPG/JPEG images in the configured directory
* Generate `.blho` files
* Upload missing blocks to the server

## Intended Audience
This project is intended for:
* Researchers in lossless compression
* Engineers exploring content-addressable storage
* Developers interested in alternative image representations

It is **not** intended as a drop-in replacement for existing image codecs.