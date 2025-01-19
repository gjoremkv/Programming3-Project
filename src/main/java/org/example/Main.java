package org.example;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // image
        String inputImagePath = "src/main/resources/input4.jpg";
        String outputImagePath = "src/main/resources/output4.jpg";

        // Defining kernel (edge Detection)
        double[][] kernel = {
                {0, -1, 0},
                {-1, 4, -1},
                {0, -1, 0}
        };

        try {
            // read the input image
            BufferedImage inputImage = ImageIO.read(new File(inputImagePath));

            // apply convolution
            BufferedImage outputImage = ConvolutionProcessor.applyConvolution(inputImage, kernel);

            //write the output image
            ImageIO.write(outputImage, "jpg", new File(outputImagePath));

            System.out.println("Image processed successfully! Check " + outputImagePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
