#!/bin/bash

echo "🧪 COMPREHENSIVE IMAGE PROCESSING TEST SUITE"
echo "============================================="
echo ""

# Create organized output directories
echo "📁 Setting up organized output directories..."
mkdir -p results/{sequential,multithreaded,distributed}/{edge_detection,blur,sharpen,mirror}
mkdir -p results/timing_logs
mkdir -p test_images

echo "✅ Directory structure created:"
echo "   📂 results/"
echo "     ├── 📂 sequential/ (edge_detection, blur, sharpen, mirror)"
echo "     ├── 📂 multithreaded/   (edge_detection, blur, sharpen, mirror)"
echo "     ├── 📂 distributed/ (edge_detection, blur, sharpen, mirror)"
echo "     ├── 📂 timing_logs/ (CSV files for graph creation)"
echo "     └── 📂 test_images/ (your input images)"
echo ""

# Clean up old test images
echo "🧹 Cleaning up old test images..."
cd src/main/resources
rm -f input*.jpg output*.jpg seq_*.png par_*.png *.png gui_output.jpg output_real_distributed.jpg
rm -f output_distributed*.jpg
echo "✅ Old test images removed"
cd ../../..
echo ""

# Create CSV headers for timing data
echo "📊 Creating timing log files..."
echo "image_name,operation,mode,execution_time_ms,image_width,image_height,timestamp" > results/timing_logs/processing_times.csv
echo "image_name,operation,setup_time_ms,distribution_time_ms,collection_time_ms,total_time_ms,timestamp" > results/timing_logs/distributed_breakdown.csv

echo "✅ Timing log files created with headers"
echo ""

# Compile everything
echo "📦 Compiling all components..."
javac -cp .:$MPJ_HOME/lib/mpj.jar src/main/java/org/example/*.java
if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi
echo "✅ Compilation successful!"
echo ""

echo "🎯 TEST SUITE READY!"
echo "==================="
echo ""
echo "📋 Instructions:"
echo "1. 📂 Place your 10 test images in: results/test_images/"
echo "2. 🖼️  Supported formats: JPG, PNG, BMP"
echo "3. ▶️  Run: ./process_all_images.sh"
echo ""
echo "📊 What will be generated:"
echo "   • 🖼️  Processed images organized by mode and operation"
echo "   • 📈 CSV files with timing data for graph creation"
echo "   • 🏷️  Clear labeling: imagename_operation_mode.jpg"
echo ""
echo "📁 Example output structure:"
echo "   results/sequential/edge_detection/myimage_edge_sequential.jpg"
echo "   results/multithreaded/blur/myimage_blur_multithreaded.jpg"
echo "   results/distributed/sharpen/myimage_sharpen_distributed.jpg"
echo ""
echo "Ready to process your images! 🚀" 