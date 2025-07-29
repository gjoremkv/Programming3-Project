# Distributed Image Convolution Project

A Java implementation of image convolution with multiple processing approaches: sequential, parallel, and distributed computing using MPJ Express.

## 🚀 Quick Start

### Launch GUI (Recommended)
```bash
./run_gui.sh
```

### Command Line
```bash
# Sequential/Parallel processing
java -cp src/main/java org.example.Main sequential
java -cp src/main/java org.example.Main parallel

# Distributed processing (requires MPJ Express)
mpjrun.sh -np 4 -cp src/main/java org.example.RealDistributedConvolution
```

## 🧪 Comprehensive Testing Suite

### Set Up Testing Environment
```bash
./comprehensive_image_testing.sh
```

### Add Your Test Images
1. Place your 10 test images in: `test_images/`
2. Supported formats: JPG, PNG, BMP
3. Images will be processed through all modes and operations

### Run Complete Testing
```bash
./process_all_images.sh
```

### View Results
```bash
./view_results.sh
```

## 📊 Generated Data Structure

### Organized Output Images
```
results/
├── sequential/
│   ├── edge_detection/  → imagename_edge_sequential.jpg
│   ├── blur/           → imagename_blur_sequential.jpg
│   └── sharpen/        → imagename_sharpen_sequential.jpg
├── parallel/
│   ├── edge_detection/  → imagename_edge_parallel.jpg
│   ├── blur/           → imagename_blur_parallel.jpg
│   └── sharpen/        → imagename_sharpen_parallel.jpg
└── distributed/
    ├── edge_detection/  → imagename_edge_distributed.jpg
    ├── blur/           → imagename_blur_distributed.jpg
    └── sharpen/        → imagename_sharpen_distributed.jpg
```

### CSV Data for Graphs
- **`results/timing_logs/processing_times.csv`** - Complete timing data
- **`results/timing_logs/distributed_breakdown.csv`** - Detailed distributed timings

## 📈 Performance Analysis

### Timing Precision
All timing measurements display **3 decimal places** for microsecond precision:
- **Sequential**: ~96.836 ms (single-threaded baseline)
- **Parallel**: ~149.141 ms (Java streams with some overhead)
- **Distributed**: ~230.000 ms (true distributed with 3 workers)

### Detailed Distributed Timing Breakdown
```
Setup time (image loading):      ~72.000 ms
Distribution time (send chunks):  ~55.000 ms  
Collection time (receive results): ~100.000 ms
Total execution time:            ~230.000 ms
```

### Features
- ✅ **3 Workers** - True distributed processing with 1 master + 3 worker processes
- ✅ **Ghost Cells** - Eliminates boundary artifacts with 1-pixel overlap
- ✅ **Precise Timing** - 3 decimal place accuracy for all operations
- ✅ **All Operations** - Edge Detection, Blur, Sharpen, Mirror
- ✅ **Drag & Drop** - Interactive image selection in GUI
- ✅ **Batch Processing** - Automated testing of multiple images
- ✅ **CSV Export** - Ready-to-graph timing data