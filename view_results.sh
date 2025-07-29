#!/bin/bash

echo "📊 RESULTS VIEWER"
echo "================="
echo ""

# Check if results exist
if [ ! -d "results" ]; then
    echo "❌ No results directory found!"
    echo "📋 Run ./comprehensive_image_testing.sh first to set up"
    echo "📋 Then run ./process_all_images.sh to process your images"
    exit 1
fi

echo "📂 Results Directory Structure:"
echo "==============================="
tree results/ 2>/dev/null || ls -la results/

echo ""
echo "📈 Timing Data Summary:"
echo "======================"

if [ -f "results/timing_logs/processing_times.csv" ]; then
    echo ""
    echo "🔍 Processing Times (First 10 rows):"
    echo "------------------------------------"
    head -10 results/timing_logs/processing_times.csv
    
    echo ""
    echo "📊 Statistics:"
    echo "-------------"
    echo -n "Total processed images: "
    tail -n +2 results/timing_logs/processing_times.csv | cut -d',' -f1 | sort -u | wc -l
    
    echo -n "Total processing tasks: "
    tail -n +2 results/timing_logs/processing_times.csv | wc -l
    
    echo ""
    echo "⚡ Performance Summary by Mode:"
    echo "------------------------------"
    echo "Sequential average:"
    tail -n +2 results/timing_logs/processing_times.csv | awk -F',' '$3=="sequential" {sum+=$4; count++} END {if(count>0) printf "  %.3f ms (n=%d)\n", sum/count, count; else print "  No data"}'
    
    echo "Multithreaded average:"
    tail -n +2 results/timing_logs/processing_times.csv | awk -F',' '$3=="multithreaded" {sum+=$4; count++} END {if(count>0) printf "  %.3f ms (n=%d)\n", sum/count, count; else print "  No data"}'
    
    echo "Distributed average:"
    tail -n +2 results/timing_logs/processing_times.csv | awk -F',' '$3=="distributed" {sum+=$4; count++} END {if(count>0) printf "  %.3f ms (n=%d)\n", sum/count, count; else print "  No data"}'
else
    echo "❌ No timing data found. Run ./process_all_images.sh first."
fi

echo ""
if [ -f "results/timing_logs/distributed_breakdown.csv" ]; then
    echo "🔍 Distributed Timing Breakdown (First 5 rows):"
    echo "-----------------------------------------------"
    head -5 results/timing_logs/distributed_breakdown.csv
fi

echo ""
echo "📁 Generated Images Summary:"
echo "============================"
for mode_dir in results/sequential results/multithreaded results/distributed; do
    if [ -d "$mode_dir" ]; then
        mode_name=$(basename "$mode_dir")
        echo "📂 $mode_name:"
        for op_dir in "$mode_dir"/*; do
            if [ -d "$op_dir" ]; then
                op_name=$(basename "$op_dir")
                count=$(ls -1 "$op_dir"/*.jpg 2>/dev/null | wc -l)
                echo "  🔧 $op_name: $count images"
            fi
        done
    fi
done

echo ""
echo "💡 Tips for Creating Graphs:"
echo "============================"
echo "📈 Excel/Google Sheets:"
echo "  • Import: results/timing_logs/processing_times.csv"
echo "  • Create pivot table with Mode vs Operation"
echo "  • Chart execution times by image size"
echo ""
echo "📈 Python/R:"
echo "  • Use pandas.read_csv() for processing_times.csv"
echo "  • Plot: execution_time vs (image_width * image_height)"
echo "  • Group by: mode, operation"
echo ""
echo "📈 Suggested Graphs:"
echo "  1. Bar chart: Average time by mode"
echo "  2. Scatter plot: Time vs image size (colored by mode)"
echo "  3. Breakdown chart: Distributed timing components"
echo "  4. Performance ratio: Multithreaded/Sequential, Distributed/Sequential" 