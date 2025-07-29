#!/bin/bash

echo "🚀 PROCESSING ALL IMAGES THROUGH ALL MODES"
echo "==========================================="
echo ""

# Check if test images exist
if [ ! -d "test_images" ] || [ -z "$(ls -A test_images 2>/dev/null)" ]; then
    echo "❌ No images found in test_images/ directory!"
    echo "📂 Please add your 10 test images to: test_images/"
    echo "🖼️  Supported formats: .jpg, .jpeg, .png, .bmp"
    exit 1
fi

# Get timestamp for this test run
TIMESTAMP=$(date '+%Y-%m-%d_%H-%M-%S')
echo "📅 Test run timestamp: $TIMESTAMP"
echo ""

# Define operations and their corresponding kernels
declare -A OPERATIONS
OPERATIONS[edge_detection]="Edge Detection"
OPERATIONS[blur]="Blur" 
OPERATIONS[sharpen]="Sharpen"
OPERATIONS[mirror]="Mirror"

# Function to get image dimensions
get_image_dimensions() {
    local image_path="$1"
    identify -format "%wx%h" "$image_path" 2>/dev/null || echo "unknown"
}

# Function to extract timing from output
extract_timing() {
    local output="$1"
    echo "$output" | grep "Execution time:" | sed 's/.*Execution time: \([0-9.]*\) ms.*/\1/'
}

# Function to extract distributed timing breakdown
extract_distributed_timing() {
    local output="$1"
    local setup=$(echo "$output" | grep "Setup time" | sed 's/.*: *\([0-9.]*\) ms.*/\1/')
    local distribution=$(echo "$output" | grep "Distribution time" | sed 's/.*: *\([0-9.]*\) ms.*/\1/')
    local collection=$(echo "$output" | grep "Collection time" | sed 's/.*: *\([0-9.]*\) ms.*/\1/')
    local total=$(echo "$output" | grep "Total execution time" | sed 's/.*: *\([0-9.]*\) ms.*/\1/')
    echo "$setup,$distribution,$collection,$total"
}

# Function to process image with sequential/multithreaded modes
process_standard_mode() {
    local image_path="$1"
    local image_name="$2"
    local operation="$3"
    local mode="$4"
    local output_dir="$5"
    
    echo "  🔄 Processing: $mode mode..."
    
    # Copy image to resources for processing
    cp "$image_path" "src/main/resources/temp_input.jpg"
    
    # Get image dimensions
    local dimensions=$(get_image_dimensions "$image_path")
    local width=$(echo $dimensions | cut -d'x' -f1)
    local height=$(echo $dimensions | cut -d'x' -f2)
    
    # Process the image
    local output=$(java -cp src/main/java org.example.Main $mode "src/main/resources/temp_input.jpg" "src/main/resources/temp_output.jpg" $operation 2>&1)
    
    # Extract timing
    local timing=$(extract_timing "$output")
    
    # Create output filename
    local output_filename="${image_name%.*}_${operation}_${mode}.jpg"
    local final_output_path="$output_dir/$output_filename"
    
    # Copy result to organized location
    if [ -f "src/main/resources/temp_output.jpg" ]; then
        cp "src/main/resources/temp_output.jpg" "$final_output_path"
        echo "    ✅ Saved: $final_output_path"
        echo "    ⏱️  Time: ${timing} ms"
        
        # Log timing data
        csv_mode="$mode"
        if [ "$mode" = "parallel" ]; then
            csv_mode="multithreaded"
        fi
        echo "${image_name%.*},$operation,$csv_mode,$timing,$width,$height,$TIMESTAMP" >> results/timing_logs/processing_times.csv
    else
        echo "    ❌ Failed to process $mode mode"
    fi
    
    # Cleanup
    rm -f "src/main/resources/temp_input.jpg" "src/main/resources/temp_output.jpg"
}

# Function to process image with distributed mode
process_distributed_mode() {
    local image_path="$1"
    local image_name="$2"
    local operation="$3"
    local output_dir="$4"
    
    echo "  🔄 Processing: distributed mode..."
    
    # Copy image to GUI temp location
    cp "$image_path" "src/main/resources/gui_input_temp.jpg"
    
    # Set operation for distributed processing
    echo "${OPERATIONS[$operation]}" > "src/main/resources/gui_operation.txt"
    
    # Get image dimensions
    local dimensions=$(get_image_dimensions "$image_path")
    local width=$(echo $dimensions | cut -d'x' -f1)
    local height=$(echo $dimensions | cut -d'x' -f2)
    
    # Process with distributed mode
    local output=$(mpjrun.sh -np 4 -cp src/main/java org.example.RealDistributedConvolution 2>&1)
    
    # Extract timing information
    local timing_breakdown=$(extract_distributed_timing "$output")
    local total_timing=$(echo "$timing_breakdown" | cut -d',' -f4)
    
    # Create output filename
    local output_filename="${image_name%.*}_${operation}_distributed.jpg"
    local final_output_path="$output_dir/$output_filename"
    
    # Copy result to organized location
    if [ -f "src/main/resources/output_real_distributed.jpg" ]; then
        cp "src/main/resources/output_real_distributed.jpg" "$final_output_path"
        echo "    ✅ Saved: $final_output_path"
        echo "    ⏱️  Time: ${total_timing} ms"
        
        # Log timing data
        echo "${image_name%.*},$operation,distributed,$total_timing,$width,$height,$TIMESTAMP" >> results/timing_logs/processing_times.csv
        echo "${image_name%.*},$operation,$timing_breakdown,$TIMESTAMP" >> results/timing_logs/distributed_breakdown.csv
    else
        echo "    ❌ Failed to process distributed mode"
    fi
    
    # Cleanup
    rm -f "src/main/resources/gui_input_temp.jpg" "src/main/resources/output_real_distributed.jpg"
}

# Function to process mirror operation (no convolution)
process_mirror_operation() {
    local image_path="$1"
    local image_name="$2"
    
    echo "🪞 Processing Mirror operation for: $image_name"
    
    # Mirror operation is handled differently - it's implemented in GUI
    # For now, we'll skip mirror in batch processing since it doesn't use convolution
    echo "  ⚠️  Mirror operation skipped in batch mode (use GUI for mirror)"
    echo ""
}

# Main processing loop
echo "📊 Found images:"
image_count=0
for image_file in test_images/*; do
    if [[ -f "$image_file" && "$image_file" =~ \.(jpg|jpeg|png|bmp)$ ]]; then
        echo "  📷 $(basename "$image_file")"
        ((image_count++))
    fi
done
echo ""
echo "🔢 Total images to process: $image_count"
echo "🔧 Operations per image: 3 (Edge Detection, Blur, Sharpen)"
echo "⚙️  Modes per operation: 3 (Sequential, Multithreaded, Distributed)"
echo "📈 Total processing tasks: $((image_count * 3 * 3))"
echo ""

# Process each image
processed_images=0
for image_file in test_images/*; do
    if [[ -f "$image_file" && "$image_file" =~ \.(jpg|jpeg|png|bmp)$ ]]; then
        image_name=$(basename "$image_file")
        ((processed_images++))
        
        echo "🖼️  Processing image $processed_images/$image_count: $image_name"
        echo "=================================================="
        
        # Process each operation (skip mirror for batch)
        for operation in edge_detection blur sharpen; do
            operation_name=${OPERATIONS[$operation]}
            echo ""
            echo "🔧 Operation: $operation_name"
            echo "----------------------------"
            
            # Process with each mode
            process_standard_mode "$image_file" "$image_name" "$operation" "sequential" "results/sequential/$operation"
            process_standard_mode "$image_file" "$image_name" "$operation" "parallel" "results/multithreaded/$operation"
            process_distributed_mode "$image_file" "$image_name" "$operation" "results/distributed/$operation"
        done
        
        echo ""
        echo "✅ Completed processing: $image_name"
        echo ""
    fi
done

echo "🎉 BATCH PROCESSING COMPLETE!"
echo "============================="
echo ""
echo "📊 Results Summary:"
echo "  🖼️  Images processed: $processed_images"
echo "  📁 Results saved in: results/"
echo "  📈 Timing data: results/timing_logs/"
echo ""
echo "📂 Generated files:"
echo "  📋 results/timing_logs/processing_times.csv"
echo "  📋 results/timing_logs/distributed_breakdown.csv"
echo ""
echo "📈 Use these CSV files to create graphs showing:"
echo "  • Performance comparison across modes"
echo "  • Execution time vs image size"
echo "  • Distributed processing breakdown"
echo ""
echo "🔍 To view results:"
echo "  ls -la results/*/"
echo "  head results/timing_logs/*.csv" 