import zlib
import sys

def extract_zlib(filename):
    with open(filename, 'rb') as f:
        data = f.read()

    # Search for zlib headers (0x78 0x01, 0x78 0x9c, 0x78 0xda)
    # Actually, let's just try decompressing every possible offset
    extracted = []
    for i in range(len(data) - 2):
        if data[i] == 0x78 and data[i+1] in [0x01, 0x9c, 0xda]:
            try:
                decompressed = zlib.decompress(data[i:])
                if b'fun ProfileScreen(' in decompressed and b'package com.example.neuv3.features.social.presentation.profile' in decompressed:
                    extracted.append(decompressed)
            except Exception:
                pass
    
    for idx, content in enumerate(extracted):
        with open(f"extracted_{idx}.kt", "wb") as out:
            out.write(content)
        print(f"Extracted {idx} with size {len(content)}")

if __name__ == "__main__":
    extract_zlib(sys.argv[1])
