#!/bin/bash

echo "============================================="
echo "Starting Image Processor GUI"
echo "============================================="
echo ""

# Compile the GUI if needed
echo "Compiling GUI application..."
javac -cp .:$MPJ_HOME/lib/mpj.jar src/main/java/org/example/*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo ""
    echo "Launching GUI..."
    echo ""
    echo "📋 Available Features:"
    echo "  📁 File Selection - Choose any image file OR drag & drop images!"
    echo "  🔧 Operations - Edge Detection, Blur, Sharpen, Mirror"
    echo "  ⚙️ Processing Modes:"
    echo "    • Sequential - Single-threaded baseline"
    echo "    • Parallel - Multi-core Java Streams"
    echo "    • Distributed - True distributed computing with MPJ Express"
    echo ""
    
    # Run the GUI with correct classpath
    java -cp src/main/java:$MPJ_HOME/lib/mpj.jar org.example.ImageProcessorGUI
else
    echo "❌ Compilation failed!"
    exit 1
fi 