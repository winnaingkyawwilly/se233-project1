package com.project.imagewatermark.jobs;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

public class WatermarkTask implements Callable<Void> {
    private final Image imgToSave;
    private final File saveDirectory;
    private final int i;
    private String outputFormat;

    public WatermarkTask(Image image, File saveDirectory, String outputFormat, int i) {
        this.imgToSave = image;
        this.saveDirectory = saveDirectory;
        this.i = i;
        this.outputFormat = outputFormat;
    }
    @Override
    public Void call() throws Exception {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(imgToSave, null);

        BufferedImage newBufferedImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        newBufferedImage.createGraphics().drawImage(bufferedImage, null, 0, 0);

        // Determine the file name based on the image format
        String fileName = "watermarked_image_" + i + "." + this.outputFormat.toLowerCase();

        File file = Paths.get(saveDirectory.getAbsolutePath(), fileName).toFile();

        ImageIO.write(newBufferedImage, this.outputFormat.toLowerCase(), file);
        return null;
    }
}
