const fs = require('fs');
const zlib = require('zlib');

const data = fs.readFileSync(process.argv[2]);
let extractedCount = 0;

for (let i = 0; i < data.length - 2; i++) {
    if (data[i] === 0x78 && (data[i+1] === 0x01 || data[i+1] === 0x9c || data[i+1] === 0xda)) {
        try {
            const decompressed = zlib.inflateSync(data.slice(i));
            const str = decompressed.toString('utf8');
            if (str.includes('fun ProfileScreen(') && str.includes('package com.example.mindos.features.social.presentation.profile')) {
                fs.writeFileSync(`extracted_${extractedCount}.kt`, decompressed);
                console.log(`Extracted version ${extractedCount} with size ${decompressed.length}`);
                extractedCount++;
            }
        } catch (e) {
            // Ignore decompression errors
        }
    }
}
