#!/bin/bash

echo "🎬 DEMO: Testing System Demonstration"
echo "====================================="
echo ""

# Check if we have an existing image to use for demo
demo_image=""
if [ -f "src/main/resources/gui_input_temp.jpg" ]; then
    demo_image="src/main/resources/gui_input_temp.jpg"
    echo "📷 Using existing image for demo: gui_input_temp.jpg"
elif [ -n "$(find . -name '*.jpg' -o -name '*.png' 2>/dev/null | head -1)" ]; then
    demo_image=$(find . -name '*.jpg' -o -name '*.png' 2>/dev/null | head -1)
    echo "📷 Found image for demo: $(basename $demo_image)"
else
    echo "❌ No images found for demo!"
    echo "💡 Add an image to test_images/ and run ./process_all_images.sh"
    exit 1
fi

# Copy demo image to test_images
echo "📂 Setting up demo image..."
cp "$demo_image" "test_images/demo_image.jpg"

echo "✅ Demo image ready: test_images/demo_image.jpg"
echo ""

echo "🚀 Running processing demo (1 image × 3 operations × 3 modes = 9 tasks)..."
echo ""

# Run the processing
./process_all_images.sh

echo ""
echo "📊 Demo Results:"
echo "==============="
./view_results.sh

echo ""
echo "🎉 Demo Complete!"
echo "================="
echo ""
echo "💡 To test with your 10 images:"
echo "1. Add your images to test_images/"
echo "2. Run: ./process_all_images.sh"
echo "3. View results: ./view_results.sh"
echo ""
echo "📈 The CSV files are ready for creating graphs in Excel/Python/R!" 