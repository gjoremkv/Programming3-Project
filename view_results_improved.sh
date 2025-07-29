#!/bin/bash

echo "📊 COMPREHENSIVE RESULTS ANALYSIS"
echo "=================================="
echo ""

# Check if results exist
if [ ! -f "results/timing_logs/processing_times.csv" ]; then
    echo "❌ No timing data found!"
    echo "📋 Run ./process_all_images.sh first to generate data"
    exit 1
fi

# Function to format time nicely
format_time() {
    local time_ms="$1"
    if (( $(echo "$time_ms >= 1000" | bc -l) )); then
        local seconds=$(echo "scale=2; $time_ms / 1000" | bc)
        echo "${seconds}s"
    else
        echo "${time_ms}ms"
    fi
}

echo "🏆 OVERALL PERFORMANCE SUMMARY"
echo "==============================="
echo ""

# Calculate overall averages
echo "📈 Average processing times across all images and operations:"
echo ""

sequential_avg=$(tail -n +2 results/timing_logs/processing_times.csv | awk -F',' '$3=="sequential" {sum+=$4; count++} END {if(count>0) print sum/count; else print 0}')
multithreaded_avg=$(tail -n +2 results/timing_logs/processing_times.csv | awk -F',' '$3=="multithreaded" {sum+=$4; count++} END {if(count>0) print sum/count; else print 0}')
distributed_avg=$(tail -n +2 results/timing_logs/processing_times.csv | awk -F',' '$3=="distributed" {sum+=$4; count++} END {if(count>0) print sum/count; else print 0}')

echo "🔵 SEQUENTIAL:  $(format_time $sequential_avg) (baseline)"
echo "🟢 MULTITHREADED: $(format_time $multithreaded_avg)"
echo "🟡 DISTRIBUTED: $(format_time $distributed_avg)"
echo ""

# Calculate speedups
if (( $(echo "$sequential_avg > 0" | bc -l) )); then
    multithreaded_speedup=$(echo "scale=2; $sequential_avg / $multithreaded_avg" | bc)
    distributed_speedup=$(echo "scale=2; $sequential_avg / $distributed_avg" | bc)
    
    multithreaded_improvement=$(echo "scale=1; (1 - $multithreaded_avg / $sequential_avg) * 100" | bc)
    distributed_improvement=$(echo "scale=1; (1 - $distributed_avg / $sequential_avg) * 100" | bc)
    
    echo "⚡ SPEED IMPROVEMENTS:"
    echo "  Multithreaded: ${multithreaded_speedup}x faster (${multithreaded_improvement}% improvement)"
    echo "  Distributed: ${distributed_speedup}x faster (${distributed_improvement}% improvement)"
fi

echo ""
echo "📏 PERFORMANCE BY IMAGE SIZE"
echo "============================="
echo ""

# Create a temporary file for processing
temp_file=$(mktemp)
tail -n +2 results/timing_logs/processing_times.csv | sort -t',' -k5,5n > "$temp_file"

# Group by image size and show performance
current_size=""
while IFS=',' read -r image_name operation mode time_ms width height timestamp; do
    size="${width}×${height}"
    pixels=$((width * height))
    megapixels=$(echo "scale=1; $pixels / 1000000" | bc)
    
    if [ "$current_size" != "$size" ]; then
        if [ -n "$current_size" ]; then
            echo ""
        fi
        echo "📐 $size ($megapixels MP):"
        current_size="$size"
        
        # Get all times for this size
        seq_time=$(grep ",$width,$height," "$temp_file" | grep ",sequential," | head -1 | cut -d',' -f4)
        multi_time=$(grep ",$width,$height," "$temp_file" | grep ",multithreaded," | head -1 | cut -d',' -f4)
        dist_time=$(grep ",$width,$height," "$temp_file" | grep ",distributed," | head -1 | cut -d',' -f4)
        
        printf "  %-12s: %8s" "Sequential" "$(format_time $seq_time)"
        printf "  %-12s: %8s" "Multithreaded" "$(format_time $multi_time)"
        printf "  %-12s: %8s\n" "Distributed" "$(format_time $dist_time)"
        
        # Calculate speedups for this size
        if (( $(echo "$seq_time > 0" | bc -l) )); then
            multi_speedup=$(echo "scale=2; $seq_time / $multi_time" | bc)
            dist_speedup=$(echo "scale=2; $seq_time / $dist_time" | bc)
            echo "    💨 Speedups: Multithreaded ${multi_speedup}x, Distributed ${dist_speedup}x"
        fi
    fi
done < "$temp_file"

rm "$temp_file"

echo ""
echo ""
echo "🔧 PERFORMANCE BY OPERATION TYPE"
echo "================================="
echo ""

for operation in edge_detection blur sharpen; do
    case $operation in
        edge_detection) op_name="Edge Detection" ;;
        blur) op_name="Blur Filter" ;;
        sharpen) op_name="Sharpen Filter" ;;
    esac
    
    echo "🔸 $op_name:"
    
    seq_op=$(tail -n +2 results/timing_logs/processing_times.csv | awk -F',' -v op="$operation" '$2==op && $3=="sequential" {sum+=$4; count++} END {if(count>0) print sum/count; else print 0}')
    multi_op=$(tail -n +2 results/timing_logs/processing_times.csv | awk -F',' -v op="$operation" '$2==op && $3=="multithreaded" {sum+=$4; count++} END {if(count>0) print sum/count; else print 0}')
    dist_op=$(tail -n +2 results/timing_logs/processing_times.csv | awk -F',' -v op="$operation" '$2==op && $3=="distributed" {sum+=$4; count++} END {if(count>0) print sum/count; else print 0}')
    
    printf "  %-12s: %8s" "Sequential" "$(format_time $seq_op)"
    printf "  %-12s: %8s" "Multithreaded" "$(format_time $multi_op)"
    printf "  %-12s: %8s\n" "Distributed" "$(format_time $dist_op)"
    echo ""
done

echo "💡 KEY INSIGHTS"
echo "==============="
echo ""

# Calculate some insights
total_images=$(tail -n +2 results/timing_logs/processing_times.csv | cut -d',' -f1 | sort -u | wc -l)
total_tasks=$(tail -n +2 results/timing_logs/processing_times.csv | wc -l)

echo "📊 Data Summary:"
echo "  • $total_images different images tested"
echo "  • $total_tasks total processing tasks completed"
echo "  • 3 operations tested (Edge Detection, Blur, Sharpen)"
echo "  • 3 processing modes compared"
echo ""

# Find best performer for different scenarios
smallest_image=$(tail -n +2 results/timing_logs/processing_times.csv | sort -t',' -k5,5n | head -1)
largest_image=$(tail -n +2 results/timing_logs/processing_times.csv | sort -t',' -k5,5nr | head -1)

echo "🎯 Performance Insights:"
echo "  • For small images: Multithreaded mode often has overhead"
echo "  • For large images: Multithreaded and Distributed show significant gains"
echo "  • Distributed mode includes MPI communication overhead"
echo "  • Best choice depends on image size and system resources"
echo ""

echo "📈 NEXT STEPS"
echo "============="
echo ""
echo "🐍 Generate professional graphs for your LaTeX report:"
echo "   python3 analyze_results.py"
echo ""
echo "📁 Your results are organized in:"
echo "   results/sequential/    → Sequential processing results"
echo "   results/multithreaded/ → Multithreaded processing results" 
echo "   results/distributed/   → Distributed processing results"
echo ""
echo "📊 CSV files for further analysis:"
echo "   results/timing_logs/processing_times.csv"
echo "   results/timing_logs/distributed_breakdown.csv" 