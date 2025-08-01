package org.example;

import mpi.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Scanner;

public class RealDistributedConvolution {

    public static void main(String[] args) throws Exception {
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        final int MASTER = 0;
        BufferedImage inputImage = null;

        // 🧠 Filter out MPJ prepended args (like "0", "4", "smpdev")
        int offset = 0;
        while (offset < args.length && (args[offset].matches("\\d+") || args[offset].equalsIgnoreCase("smpdev"))) {
            offset++;
        }
        String[] realArgs = new String[Math.max(0, args.length - offset)];
        System.arraycopy(args, offset, realArgs, 0, realArgs.length);

        if (rank == MASTER) {
            System.out.println("===== FILTERED USER ARGS =====");
            for (int i = 0; i < realArgs.length; i++) {
                System.out.println("realArgs[" + i + "] = " + realArgs[i]);
            }
        }

        // Default operation and kernel
        double[][] kernel = {
                {0, -1, 0},
                {-1, 4, -1},
                {0, -1, 0}
        };
        String operation = "Edge Detection";

        if (rank == MASTER) {
            String inputPath = (realArgs.length > 0) ? realArgs[0] : "src/main/resources/gui_input_temp.jpg";
            String outputPath = (realArgs.length > 1) ? realArgs[1] : "src/main/resources/output_real_distributed.jpg";
            String operationArg = (realArgs.length > 2) ? realArgs[2].toLowerCase() : null;

            System.out.println("Resolved input path: " + inputPath);
            System.out.println("Resolved output path: " + outputPath);

            if ("mirror".equals(operationArg)) {
                operation = "Mirror";
                System.out.println("Performing mirror operation...");

                File inputFile = new File(inputPath);
                if (!inputFile.exists()) {
                    System.out.println("Input file not found: " + inputPath);
                    MPI.Finalize();
                    return;
                }

                inputImage = ImageIO.read(inputFile);
                int width = inputImage.getWidth();
                int height = inputImage.getHeight();

                BufferedImage mirrored = new BufferedImage(width, height, inputImage.getType());
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pixel = inputImage.getRGB(x, y);
                        mirrored.setRGB(width - x - 1, y, pixel);
                    }
                }

                ImageIO.write(mirrored, "jpg", new File(outputPath));
                System.out.println("Mirror output saved to: " + outputPath);

                MPI.Finalize();
                return;
            }

            // Choose kernel based on operation
            if (operationArg != null) {
                switch (operationArg) {
                    case "blur":
                        operation = "Blur";
                        kernel = new double[][]{
                                {1 / 16.0, 2 / 16.0, 1 / 16.0},
                                {2 / 16.0, 4 / 16.0, 2 / 16.0},
                                {1 / 16.0, 2 / 16.0, 1 / 16.0}
                        };
                        break;
                    case "sharpen":
                        operation = "Sharpen";
                        kernel = new double[][]{
                                {0, -1, 0},
                                {-1, 5, -1},
                                {0, -1, 0}
                        };
                        break;
                    case "edge":
                    case "edge detection":
                    default:
                        operation = "Edge Detection";
                        break;
                }
            }

            System.out.println("Starting real distributed convolution with " + size + " processes");
            System.out.println("Operation: " + operation);

            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                System.out.println("Input file not found: " + inputPath);
                MPI.Finalize();
                return;
            }

            inputImage = ImageIO.read(inputFile);
            int height = inputImage.getHeight();
            int width = inputImage.getWidth();
            System.out.println("Image dimensions: " + width + "x" + height);

            // Send kernel to workers
            double[] flatKernel = new double[9];
            for (int i = 0, idx = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    flatKernel[idx++] = kernel[i][j];
            for (int i = 1; i < size; i++)
                MPI.COMM_WORLD.Send(flatKernel, 0, 9, MPI.DOUBLE, i, 99);

            // Distribute chunks
            int kernelRadius = 1;
            int chunkHeight = height / (size - 1);
            int remainder = height % (size - 1);

            for (int i = 1; i < size; i++) {
                int yStart = (i - 1) * chunkHeight;
                int yEnd = yStart + chunkHeight + (i == size - 1 ? remainder : 0);
                int yStartPad = Math.max(0, yStart - kernelRadius);
                int yEndPad = Math.min(height, yEnd + kernelRadius);

                int paddedHeight = yEndPad - yStartPad;
                int[] pixels = new int[width * paddedHeight];
                inputImage.getRGB(0, yStartPad, width, paddedHeight, pixels, 0, width);

                int paddingTop = yStart - yStartPad;
                int validHeight = yEnd - yStart;

                int[] meta = {width, paddedHeight, yStart, paddingTop, validHeight};
                MPI.COMM_WORLD.Send(meta, 0, 5, MPI.INT, i, 0);
                MPI.COMM_WORLD.Send(pixels, 0, pixels.length, MPI.INT, i, 1);
            }

            // Collect results
            BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());
            for (int i = 1; i < size; i++) {
                int[] meta = new int[3];
                MPI.COMM_WORLD.Recv(meta, 0, 3, MPI.INT, i, 2);
                int w = meta[0], h = meta[1], y = meta[2];

                int[] resultPixels = new int[w * h];
                MPI.COMM_WORLD.Recv(resultPixels, 0, resultPixels.length, MPI.INT, i, 3);
                outputImage.setRGB(0, y, w, h, resultPixels, 0, w);
            }

            ImageIO.write(outputImage, "jpg", new File(outputPath));
            System.out.println("Output saved to: " + outputPath);
        } else {
            try {
                double[] flatKernel = new double[9];
                MPI.COMM_WORLD.Recv(flatKernel, 0, 9, MPI.DOUBLE, MASTER, 99);

                double[][] kernelRecv = new double[3][3];
                for (int i = 0, idx = 0; i < 3; i++)
                    for (int j = 0; j < 3; j++)
                        kernelRecv[i][j] = flatKernel[idx++];

                int[] meta = new int[5];
                MPI.COMM_WORLD.Recv(meta, 0, 5, MPI.INT, MASTER, 0);
                int width = meta[0];
                int paddedHeight = meta[1];
                int yStart = meta[2];
                int paddingTop = meta[3];
                int validHeight = meta[4];

                int[] receivedPixels = new int[width * paddedHeight];
                MPI.COMM_WORLD.Recv(receivedPixels, 0, receivedPixels.length, MPI.INT, MASTER, 1);

                BufferedImage chunk = new BufferedImage(width, paddedHeight, BufferedImage.TYPE_INT_RGB);
                chunk.setRGB(0, 0, width, paddedHeight, receivedPixels, 0, width);

                BufferedImage processed = ConvolutionProcessor.applyConvolution(chunk, kernelRecv);
                BufferedImage validRegion = processed.getSubimage(0, paddingTop, width, validHeight);
                int[] resultPixels = new int[width * validHeight];
                validRegion.getRGB(0, 0, width, validHeight, resultPixels, 0, width);

                int[] resultMeta = {width, validHeight, yStart};
                MPI.COMM_WORLD.Send(resultMeta, 0, 3, MPI.INT, MASTER, 2);
                MPI.COMM_WORLD.Send(resultPixels, 0, resultPixels.length, MPI.INT, MASTER, 3);
            } catch (Exception e) {
                System.err.println("Worker " + rank + " error:");
                e.printStackTrace();
            }
        }

        MPI.Finalize();
    }
}
