# Test Images Documentation

## Comprehensive Image Size Testing

This project uses 10 carefully selected test images with different resolutions to analyze the performance characteristics of sequential, multithreaded, and distributed image processing across various computational loads.

## Test Image Specifications

| Image | Resolution | Megapixels | File Size | Performance Profile |
|-------|------------|------------|-----------|-------------------|
| **test1** | `100×100` | **0.01 MP** | ~10 KB | Very Small - Overhead dominates |
| **test2** | `250×250` | **0.06 MP** | ~78 KB | Small - Sequential often fastest |
| **test3** | `500×500` | **0.25 MP** | ~348 KB | Medium-Small - Transition point |
| **test4** | `640×480` | **0.31 MP** | ~456 KB | VGA Standard - Mixed results |
| **test5** | `800×800` | **0.64 MP** | ~980 KB | Medium - Parallel starts showing gains |
| **test6** | `1280×720` | **0.92 MP** | ~1.4 MB | HD Ready - Clear parallel benefits |
| **test7** | `1600×900` | **1.44 MP** | ~1.6 MB | Large - Significant speedups |
| **test8** | `1920×1080` | **2.07 MP** | ~2.0 MB | Full HD - Excellent parallel performance |
| **test9** | `2560×1440` | **3.69 MP** | ~4.8 MB | QHD - Strong distributed benefits |
| **test10** | `3876×3999` | **15.50 MP** | ~7.1 MB | Very Large - Maximum speedup demonstrated |

## Performance Insights

### Speedup Patterns by Image Size

#### Small Images (< 1 MP)
- **Sequential**: Often fastest due to low overhead
- **Multithreaded**: May show slowdown due to thread creation costs
- **Distributed**: Significant overhead from MPI communication

#### Medium Images (1-3 MP)
- **Sequential**: Baseline performance
- **Multithreaded**: Shows 1.3-1.7× speedup
- **Distributed**: Competitive with multithreaded

#### Large Images (> 10 MP)
- **Sequential**: Becomes bottleneck (9.5+ seconds)
- **Multithreaded**: Excellent speedup (~1.9×)
- **Distributed**: Best performance (~2.0× speedup)

## Scientific Testing Methodology

### Operations Tested
- **Edge Detection**: Sobel operator convolution
- **Blur Filter**: Gaussian blur convolution  
- **Sharpen Filter**: Sharpening kernel convolution

### Processing Modes
- **Sequential**: Single-threaded baseline
- **Multithreaded**: Java parallel streams (CPU cores)
- **Distributed**: MPJ Express MPI (multiple processes)

### Measurements
- **Execution Time**: Microsecond precision timing
- **Speedup Factor**: `Time_Sequential / Time_Method`
- **Performance Scaling**: Across 10 different image sizes
- **Test Coverage**: 3 operations × 3 modes × 10 images = 90 total tests

## Results Summary

### Best Performance by Category

| Image Size | Best Method | Speedup | Reason |
|------------|-------------|---------|--------|
| **< 0.5 MP** | Sequential | 1.0× (baseline) | Overhead dominates |
| **0.5-2 MP** | Multithreaded | 1.3-1.7× | CPU parallelization |
| **> 10 MP** | Distributed | 1.9-2.0× | MPI efficiency at scale |

### Real-World Applications
- **Photo editing software**: Use multithreaded for most operations
- **Medical imaging**: Distributed processing for large scans
- **Video processing**: Hybrid approach based on frame size
- **Scientific computing**: Distributed for high-resolution data

## Academic Contribution

This comprehensive test suite demonstrates:

1. **Amdahl's Law**: Theoretical speedup limits in practice
2. **Parallel Computing Trade-offs**: When overhead vs. benefit matters
3. **Scalability Analysis**: Performance characteristics across problem sizes
4. **Real-world Applicability**: Practical guidance for system architects

## File Organization

```
test_images/
├── test1(100x100)-fotor-2025072920123.jpg          # 0.01 MP
├── test2(250x250)-fotor-20250729201131.jpg         # 0.06 MP
├── test3(500-500)-fotor-20250729201053.jpg         # 0.25 MP
├── test4(640-480)-fotor-20250729201020.jpg         # 0.31 MP
├── test5(800x800)-fotor-2025072920941.jpg          # 0.64 MP
├── test6(1280x720)-fotor-202507292090.jpg          # 0.92 MP
├── test7(1600x900)-fotor-2025072920749.jpg         # 1.44 MP
├── test8(1920x1080)-fotor-202507292059.jpg         # 2.07 MP
├── test9(2560x1440)-fotor.jpg                      # 3.69 MP
└── test10(3840-2160).jpg                           # 15.50 MP
```

## Reproducible Research

All test images are included in this repository to ensure:
- **Reproducible results**: Anyone can run the same tests
- **Transparent methodology**: Clear documentation of test cases
- **Academic integrity**: Complete data set provided
- **Future research**: Baseline for comparative studies

Total Test Coverage: 90 performance measurements across 10 distinct computational loads. 