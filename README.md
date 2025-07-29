# Distributed Image Processing System

## Overview

This project implements and compares three approaches to image convolution processing: sequential, multithreaded, and distributed computing using Java and MPJ Express. The system processes images through various convolution operations and provides detailed performance analysis across different image sizes.

## Features

- **Three Processing Modes**: Sequential (baseline), multithreaded (Java parallel streams), and distributed (MPI with MPJ Express)
- **Convolution Operations**: Edge detection, blur, and sharpen filters
- **Comprehensive Testing**: Performance analysis across 10 different image sizes (0.01 MP to 15.5 MP)
- **Automated Analysis**: Generates timing data, performance graphs, and speedup calculations
- **GUI Interface**: Drag-and-drop image processing with real-time results
- **Batch Processing**: Automated testing of multiple images and operations

## Test Image Coverage

The project includes 10 test images with varying resolutions to analyze performance characteristics:

| Resolution | Megapixels | Characteristics |
|------------|------------|-----------------|
| 100×100 | 0.01 MP | Very small - overhead dominates |
| 250×250 | 0.06 MP | Small image |
| 640×480 | 0.31 MP | VGA standard |
| 1920×1080 | 2.07 MP | Full HD |
| 3876×3999 | 15.50 MP | Very large - shows maximum speedup |

See [TEST_IMAGES_DOCUMENTATION.md](TEST_IMAGES_DOCUMENTATION.md) for complete specifications.

## Performance Results

The testing reveals clear patterns in when each processing method excels:

| Image Size | Sequential | Multithreaded | Distributed | Best Method |
|------------|------------|---------------|-------------|-------------|
| Small (< 1 MP) | 1.0× | 0.7× | 0.3× | Sequential |
| Medium (1-3 MP) | 1.0× | 1.6× | 1.2× | Multithreaded |
| Large (> 10 MP) | 1.0× | 1.9× | 2.0× | Distributed |

Key findings:
- Small images run faster sequentially due to thread and communication overhead
- Medium images benefit from CPU core utilization through multithreading
- Large images achieve best performance with distributed processing
- The choice of method should be based on typical image sizes in the application

## Quick Start

### Setup
```bash
git clone https://github.com/yourusername/distributed-image-processing.git
cd distributed-image-processing
./comprehensive_image_testing.sh
```

### Run Performance Analysis
```bash
# Process all test images through all modes
./process_all_images.sh

# View results summary
./view_results_improved.sh

# Generate performance graphs
python3 analyze_results.py
```

### Use the GUI
```bash
./run_gui.sh
```

## Architecture

### Processing Modes

**Sequential Processing**
- Single-threaded baseline implementation
- Direct convolution application
- Reference for speedup calculations

**Multithreaded Processing**
- Java parallel streams implementation
- Automatic CPU core utilization
- Optimal for medium-sized images

**Distributed Processing**
- MPJ Express MPI implementation
- Master-worker architecture with 4 processes
- Ghost cell handling for correct boundary processing
- Best performance for large images

### Technical Stack
- Language: Java 17
- Distributed Computing: MPJ Express (MPI)
- GUI: Java Swing
- Build System: Maven
- Analysis: Python with matplotlib, pandas, seaborn

## Generated Output

### Performance Graphs
The system generates professional-quality graphs suitable for academic reports:
- `edge_detection_performance.pdf` - Edge detection analysis
- `blur_performance.pdf` - Blur filter analysis
- `sharpen_performance.pdf` - Sharpening analysis
- `speedup_summary.pdf` - Overall speedup comparison

### Organized Results
```
results/
├── sequential/      # Sequential processing results
├── multithreaded/   # Multithreaded processing results
├── distributed/     # Distributed processing results
└── timing_logs/     # CSV data for analysis
```

### Performance Data
- `processing_times.csv` - Complete timing dataset
- `distributed_breakdown.csv` - MPI communication analysis

## Academic Applications

This project demonstrates key concepts in parallel and distributed computing:
- Amdahl's Law and practical speedup limits
- Overhead analysis in parallel systems
- Performance scaling across problem sizes
- Trade-offs between different parallelization approaches

The comprehensive testing methodology and professional documentation make it suitable for university coursework in distributed systems, parallel computing, and performance engineering.

## Dependencies

### Runtime Requirements
- Java 17+
- MPJ Express (setup included)
- ImageMagick (for image analysis)

### Analysis Requirements
- Python 3.8+
- pandas, matplotlib, seaborn, numpy

## Documentation

- [Test Images Documentation](TEST_IMAGES_DOCUMENTATION.md) - Complete test image specifications
- [MPJ Express Setup](MPJ_EXPRESS_SETUP.md) - Distributed computing setup guide
- [Graph Generation Guide](GRAPH_GENERATION.md) - Visualization creation
- [Academic Usage](ACADEMIC_USAGE.md) - University project integration

## License

MIT License - free to use for academic projects and research.

---

Comprehensive distributed computing analysis with performance testing across 10 different image sizes, demonstrating scalability principles and parallel computing trade-offs.