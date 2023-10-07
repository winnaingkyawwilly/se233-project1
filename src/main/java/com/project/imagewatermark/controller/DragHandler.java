package com.project.imagewatermark.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
public class DragHandler {

    @FXML
    private VBox vbox; // Reference to your VBox in FXML
    @FXML
    private Rectangle rectangle;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;

    @FXML
    private ImageView imageView = new ImageView();

    @FXML
    private TextField watermarkInput = new TextField();

    @FXML
    private ColorPicker watermarkColor = new ColorPicker();

    @FXML
    private Slider sizeSlider = new Slider();

    @FXML
    private Slider rotationSlider = new Slider();

    @FXML
    private Slider opacitySlider = new Slider();
    private List<Image> images = new ArrayList<>();
    private int currentImageIndex = -1;

    protected List<File> img_data;

    List<Image> toSave = new ArrayList<>();

    private ImageWatermarker watermarker = new ImageWatermarker();

    @FXML
    public void initialize() {
        // Set up drag-and-drop events for the VBox
        rectangle.setOnDragOver(this::handleDragOver);
        rectangle.setOnDragExited(this::handleDragExited);
        rectangle.setOnDragDropped(this::handleDragDrop);

        // Set up navigation button actions
        prevButton.setOnAction(event -> showPreviousImage());
        nextButton.setOnAction(event -> showNextImage());

        // Bind the slider value change event to the updateWatermarkPreview method
        sizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateWatermarkPreview());
        rotationSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateWatermarkPreview());
        opacitySlider.valueProperty().addListener((observable, oldValue, newValue) -> updateWatermarkPreview());
    }

    @FXML
    public void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.ANY);

            // Change the background color when a file is dragged over the VBox
            rectangle.setStyle("-fx-background-color: lightblue;");
        }
    }

    @FXML
    public void handleDragExited(DragEvent event) {
        // Reset the background color when the drag operation exits
        rectangle.setStyle("-fx-background-color: transparent;");
    }

    @FXML
    public void handleDragDrop(DragEvent event) {
        List<File> files = event.getDragboard().getFiles();
        img_data = event.getDragboard().getFiles();
        images.clear(); // Clear previous images
        currentImageIndex = -1;

        for (File file : files) {
            if (file.isFile()) {
                if (isSupportedImageFile(file)) {
                    Image img = null;
                    try {
                        img = new Image(new FileInputStream(file));
                        images.add(img);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                } else if (isZipFile(file)) {
                    // Handle ZIP files
                    List<Image> extractedImages = extractImagesFromZip(file);
                    if (!extractedImages.isEmpty()) {
                        images.addAll(extractedImages);
                    }
                }
            }
        }

        // Show the first image, if available
        showNextImage();

        // Reset the background color after dropping files
        vbox.setStyle("-fx-background-color: transparent;");

        // Update the watermark preview
        updateWatermarkPreview();
    }

    private boolean isZipFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".zip");
    }

    private List<Image> extractImagesFromZip(File zipFile) {
        List<Image> extractedImages = new ArrayList<>();
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(new FileInputStream(zipFile))) {
            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                if (isSupportedImageFile(new File(entry.getName()))) {
                    BufferedImage bufferedImage = ImageIO.read(zis);
                    Image img = SwingFXUtils.toFXImage(bufferedImage, null);
                    extractedImages.add(img);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return extractedImages;
    }


    private boolean isSupportedImageFile( File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png");
    }

    private void showPreviousImage() {
        if (!images.isEmpty()) {
            currentImageIndex = (currentImageIndex - 1 + images.size()) % images.size();
            updateImageView();
        }
    }

    private void showNextImage() {
        if (!images.isEmpty()) {
            currentImageIndex = (currentImageIndex + 1) % images.size();
            updateImageView();
        }
    }

    private void updateImageView() {
        if (currentImageIndex >= 0 && currentImageIndex < images.size()) {
            Image img = images.get(currentImageIndex);
            imageView.setImage(img);
            vbox.getChildren().clear();
            vbox.getChildren().add(imageView);
        }
    }

    public List<File> getImg_data() {
        return img_data;
    }

    @FXML
    private void updateWatermarkPreview() {
        Color newColor = watermarkColor.getValue();
        int newSize = (int) sizeSlider.getValue();
        String watermarkText = watermarkInput.getText();
        double newOpacity = opacitySlider.getValue();
        double rotationAngle = rotationSlider.getValue();

        if (!watermarkText.isEmpty() && !images.isEmpty()) {
            List<Image> watermarkedImages = new ArrayList<>();

            for (Image img : images) {
                Image watermarkedImage = watermarker.addWatermark(img, watermarkText, newColor, newSize, newOpacity, rotationAngle);
                watermarkedImages.add(watermarkedImage);
            }

            // Clear the existing images in the toSave list
            toSave.clear();

            // Add the new watermarked images to the toSave list
            toSave.addAll(watermarkedImages);

            // Display the watermarked images
            imageView.setImage(watermarkedImages.get(currentImageIndex));
            vbox.getChildren().clear();
            vbox.getChildren().add(imageView);
        }
    }

    @FXML
    private void saveImagesAsPngJpg() {
        if (!toSave.isEmpty()) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PNG Images", "*.png"),
                    new FileChooser.ExtensionFilter("JPEG Images", "*.jpg", "*.jpeg")
            );
            fileChooser.setTitle("Save Watermarked Images");

            // Show the file save dialog
            File selectedFile = fileChooser.showSaveDialog(new Stage());

            if (selectedFile != null) {
                // Save as individual PNG or JPG files
                saveImagesAsIndividualFiles(selectedFile);
            }
        }
    }

    @FXML
    private void saveImagesAsZip() {
        if (!toSave.isEmpty()) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Files", "*.zip"));
            fileChooser.setTitle("Save Watermarked Images as ZIP");

            // Show the file save dialog
            File selectedFile = fileChooser.showSaveDialog(new Stage());

            if (selectedFile != null) {
                // Save as a ZIP file
                saveImagesAsZip(selectedFile);
            }
        }
    }


    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return null;
    }

    private void saveImagesAsZip(File zipFile) {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipArchiveOutputStream zos = new ZipArchiveOutputStream(fos)) {

            // Iterate through watermarked images
            for (int i = 0; i < toSave.size(); i++) {
                Image imgToSave = toSave.get(i);
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(imgToSave, null);

                // Determine the file name based on the image format
                String imageFormat = getFileExtension(zipFile.getName());
                String fileName = "watermarked_image_" + i + "." + imageFormat;

                // Create a new ZIP entry for the image
                ZipArchiveEntry zipEntry = new ZipArchiveEntry(fileName);
                zos.putArchiveEntry(zipEntry);

                // Write the image data to the ZIP file
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, imageFormat, byteArrayOutputStream);
                zos.write(byteArrayOutputStream.toByteArray());

                zos.closeArchiveEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveImagesAsIndividualFiles(File saveDirectory) {
        for (int i = 0; i < toSave.size(); i++) {
            Image imgToSave = toSave.get(i);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(imgToSave, null);

            // Determine the file name based on the image format
            String imageFormat = getFileExtension(saveDirectory.getName());
            String fileName = "watermarked_image_" + i + "." + imageFormat;

            File file = new File(saveDirectory.getParentFile(), fileName);

            try {
                ImageIO.write(bufferedImage, imageFormat, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
