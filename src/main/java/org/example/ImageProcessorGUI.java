package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class ImageProcessorGUI extends JFrame {

    private BufferedImage inputImage;
    private JLabel imageLabel;
    private JLabel statusLabel;
    private JComboBox<String> operationBox;
    private JComboBox<String> modeBox;
    private File selectedFile;
    private JProgressBar progressBar;

    public ImageProcessorGUI() {
        setTitle("Kernel Image Processor - Distributed Computing Demo");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Create control panel
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

        // Create image display area with drag & drop
        imageLabel = new JLabel("Drag & drop an image here or click 'Select Image'", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(400, 400));
        imageLabel.setBorder(BorderFactory.createTitledBorder("Image Preview"));
        
        // Enable drag & drop
        setupDragAndDrop();
        
        add(new JScrollPane(imageLabel), BorderLayout.CENTER);

        // Create status panel
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.SOUTH);
    }

    private void setupDragAndDrop() {
        new DropTarget(imageLabel, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();
                    
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        
                        if (!files.isEmpty()) {
                            File file = files.get(0);
                            if (isImageFile(file)) {
                                loadImage(file);
                                dtde.dropComplete(true);
                            } else {
                                JOptionPane.showMessageDialog(ImageProcessorGUI.this, 
                                    "Please drop an image file (JPG, PNG, BMP)", 
                                    "Invalid File", JOptionPane.WARNING_MESSAGE);
                                dtde.dropComplete(false);
                            }
                        }
                    } else {
                        dtde.dropComplete(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(ImageProcessorGUI.this, 
                        "Error loading dropped file: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                    dtde.dropComplete(false);
                }
            }
        });
    }

    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".bmp");
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        controlPanel.setBorder(BorderFactory.createTitledBorder("Image Processing Controls"));

        // Select Image Button
        JButton selectImageButton = new JButton("📁 Select Image");
        selectImageButton.setPreferredSize(new Dimension(140, 30));
        selectImageButton.addActionListener(e -> chooseImage());

        // Operation Selection
        operationBox = new JComboBox<>(new String[]{"Edge Detection", "Blur", "Sharpen", "Mirror"});
        operationBox.setPreferredSize(new Dimension(140, 30));

        // Mode Selection - Changed "Real MPJ Express" to "Distributed"
        modeBox = new JComboBox<>(new String[]{"Sequential", "Parallel", "Distributed"});
        modeBox.setPreferredSize(new Dimension(160, 30));

        // Process Button
        JButton processButton = new JButton("▶️ Run Processing");
        processButton.setPreferredSize(new Dimension(140, 30));
        processButton.addActionListener(e -> processImage());

        // Layout components
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0;
        controlPanel.add(selectImageButton, gbc);
        
        gbc.gridx = 1;
        controlPanel.add(new JLabel("Operation:"), gbc);
        
        gbc.gridx = 2;
        controlPanel.add(operationBox, gbc);
        
        gbc.gridx = 3;
        controlPanel.add(new JLabel("Mode:"), gbc);
        
        gbc.gridx = 4;
        controlPanel.add(modeBox, gbc);
        
        gbc.gridx = 5;
        controlPanel.add(processButton, gbc);

        return controlPanel;
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));

        statusLabel = new JLabel("Ready - Select an image and processing mode");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");

        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(progressBar, BorderLayout.SOUTH);

        return statusPanel;
    }

    private void chooseImage() {
        JFileChooser chooser = new JFileChooser("src/main/resources");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || isImageFile(f);
            }

            @Override
            public String getDescription() {
                return "Image files (*.jpg, *.jpeg, *.png, *.bmp)";
            }
        });

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            loadImage(file);
        }
    }

    private void loadImage(File file) {
        try {
            selectedFile = file;
            inputImage = ImageIO.read(file);
            if (inputImage == null) {
                throw new IOException("Unable to read image file - unsupported format or corrupted file");
            }
            
            // Always save a temp copy for distributed processing
            File tempInput = new File("src/main/resources/gui_input_temp.jpg");
            ImageIO.write(inputImage, "jpg", tempInput);
            
            displayImage(inputImage, "Input: " + file.getName());
            statusLabel.setText("Image loaded: " + file.getName() + " (" + inputImage.getWidth() + "x" + inputImage.getHeight() + ")");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error loading image: " + ex.getMessage() + "\nPlease try a different image file.", 
                "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void displayImage(BufferedImage image, String title) {
        if (image != null) {
            // Scale image to fit display while maintaining aspect ratio
            int maxWidth = 400;
            int maxHeight = 400;
            
            double scale = Math.min(
                (double) maxWidth / image.getWidth(),
                (double) maxHeight / image.getHeight()
            );
            
            int scaledWidth = (int) (image.getWidth() * scale);
            int scaledHeight = (int) (image.getHeight() * scale);
            
            Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImage));
            imageLabel.setText("");
            imageLabel.setBorder(BorderFactory.createTitledBorder(title));
        }
    }

    private void processImage() {
        if (inputImage == null) {
            JOptionPane.showMessageDialog(this, "Please select an image first!", "No Image", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String operation = (String) operationBox.getSelectedItem();
        String mode = (String) modeBox.getSelectedItem();

        progressBar.setIndeterminate(true);
        statusLabel.setText("Processing image with " + mode + " mode...");

        // Process in background thread to keep GUI responsive
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private BufferedImage result;
            private double executionTime;
            private String outputPath;

            @Override
            protected Void doInBackground() throws Exception {
                if ("Mirror".equals(operation)) {
                    result = mirrorImage();
                    outputPath = "src/main/resources/gui_output.jpg";
                } else {
                    double[][] kernel = getKernel(operation);
                    long startTime = System.nanoTime();
                    
                    switch (mode) {
                        case "Parallel":
                            result = ConvolutionProcessor.applyConvolutionParallel(inputImage, kernel);
                            outputPath = "src/main/resources/gui_output.jpg";
                            break;
                        case "Distributed":
                            if ("Mirror".equals(operation)) {
                                // Handle mirror separately since it doesn't use convolution
                                result = mirrorImage();
                                outputPath = "src/main/resources/gui_output.jpg";
                                long endTime = System.nanoTime();
                                executionTime = (endTime - startTime) / 1_000_000.0;
                            } else {
                                runDistributed(operation);
                                return null;
                            }
                            break;
                        case "Sequential":
                        default:
                            result = ConvolutionProcessor.applyConvolution(inputImage, kernel);
                            outputPath = "src/main/resources/gui_output.jpg";
                            break;
                    }
                    
                    long endTime = System.nanoTime();
                    executionTime = (endTime - startTime) / 1_000_000.0;
                }

                // Save result
                if (result != null) {
                    ImageIO.write(result, "jpg", new File(outputPath));
                }

                return null;
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                
                try {
                    if (result != null) {
                        displayImage(result, "Output: " + operation + " (" + mode + ")");
                        String message = String.format("Processing completed!\nOperation: %s\nMode: %s\nTime: %.3f ms\nSaved: %s",
                            operation, mode, executionTime, outputPath);
                        statusLabel.setText("Completed: " + operation + " in " + String.format("%.3f", executionTime) + " ms");
                        JOptionPane.showMessageDialog(ImageProcessorGUI.this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ImageProcessorGUI.this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Error occurred during processing");
                }
                
                progressBar.setValue(0);
                progressBar.setString("Ready");
            }
        };

        worker.execute();
    }

    private double[][] getKernel(String operation) {
        switch (operation) {
            case "Blur":
                return new double[][]{
                    {1/9.0, 1/9.0, 1/9.0},
                    {1/9.0, 1/9.0, 1/9.0},
                    {1/9.0, 1/9.0, 1/9.0}
                };
            case "Sharpen":
                return new double[][]{
                    {0, -1, 0},
                    {-1, 5, -1},
                    {0, -1, 0}
                };
            case "Edge Detection":
            default:
                return new double[][]{
                    {0, -1, 0},
                    {-1, 4, -1},
                    {0, -1, 0}
                };
        }
    }

    private BufferedImage mirrorImage() {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage mirrored = new BufferedImage(width, height, inputImage.getType());
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                mirrored.setRGB(width - 1 - x, y, inputImage.getRGB(x, y));
            }
        }
        
        return mirrored;
    }

    private void runDistributed(String operation) throws Exception {
        // Capture start time for distributed processing
        long distributedStartTime = System.nanoTime();
        
        // Write operation type to a temporary file for the distributed process to read
        try (PrintWriter writer = new PrintWriter(new FileWriter("src/main/resources/gui_operation.txt"))) {
            writer.println(operation);
        }
        
        // Check if MPJ Express is available
        ProcessBuilder checkPb = new ProcessBuilder("which", "mpjrun.sh");
        Process checkProcess = checkPb.start();
        int checkCode = checkProcess.waitFor();
        
        if (checkCode != 0) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "MPJ Express not found. Please ensure mpjrun.sh is in your PATH.", "MPJ Express Not Found", JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("MPJ Express not available");
            });
            return;
        }

        // Run distributed processing with 4 processes (1 master + 3 workers) with proper ghost cells
        ProcessBuilder pb = new ProcessBuilder("mpjrun.sh", "-np", "4", "-cp", "src/main/java", "org.example.RealDistributedConvolution");
        pb.directory(new File("."));
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        // Calculate total execution time including process overhead
        long distributedEndTime = System.nanoTime();
        double totalExecutionTime = (distributedEndTime - distributedStartTime) / 1_000_000.0;
        
        if (exitCode == 0) {
            SwingUtilities.invokeLater(() -> {
                try {
                    BufferedImage output = ImageIO.read(new File("src/main/resources/output_real_distributed.jpg"));
                    displayImage(output, "Output: Distributed");
                    String message = String.format("Distributed processing completed!\nOperation: %s\nMode: Distributed (3 workers)\nTotal Time: %.3f ms\nSaved: src/main/resources/output_real_distributed.jpg", 
                        operation, totalExecutionTime);
                    statusLabel.setText("Completed: " + operation + " (Distributed) in " + String.format("%.3f", totalExecutionTime) + " ms");
                    JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error loading distributed output: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Error running distributed processing. Please check your MPJ Express installation.", "Error", JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("Error in distributed processing");
            });
        }
    }

    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }

        SwingUtilities.invokeLater(() -> {
            new ImageProcessorGUI().setVisible(true);
        });
    }
} 