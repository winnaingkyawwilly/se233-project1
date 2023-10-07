package com.project.imagewatermark.jobs;

import com.project.imagewatermark.enums.ResizeType;
import javafx.scene.paint.Color;
import org.apache.commons.compress.compressors.FileNameUtil;
import org.apache.commons.compress.utils.FileNameUtils;
import org.imgscalr.Scalr;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

public class ResizeTask implements Callable<Void> {
    private File imageFile, outputDir;
    private ResizeType resizeType;
    private int resizeValue;
    private double imgQuality;
    private Color backgroundColor;
    private String outputFormat;


    public ResizeTask(File imageFile, ResizeType resizeType, int resizeValue, File outputDir, Color backgroundColor, String outputFormat, double imgQuality) {
        this.imageFile = imageFile;
        this.resizeType = resizeType;
        this.resizeValue = resizeValue;
        this.outputDir = outputDir;
        this.backgroundColor = backgroundColor;
        this.outputFormat = outputFormat;
        this.imgQuality = imgQuality;
    }

    @Override
    public Void call() throws Exception {
        BufferedImage bufferedImage = ImageIO.read(imageFile);
        if (resizeType == ResizeType.WIDTH) {
            bufferedImage = Scalr.resize(bufferedImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH, resizeValue);
        } else if (resizeType == ResizeType.HEIGHT) {
            bufferedImage = Scalr.resize(bufferedImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_HEIGHT, resizeValue);
        } else if (resizeType == ResizeType.PERCENTAGE) {
            bufferedImage = Scalr.resize(bufferedImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, (int) (bufferedImage.getWidth() * resizeValue / 100.0), (int) (bufferedImage.getHeight() * resizeValue / 100.0));
        }
        BufferedImage newBufferedImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = newBufferedImage.createGraphics();
        graphics2D.setColor(new java.awt.Color((float) backgroundColor.getRed(), (float) backgroundColor.getGreen(), (float) backgroundColor.getBlue()));
        graphics2D.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());

        graphics2D.drawImage(bufferedImage, 0, 0, null);
        Path path = Paths.get(outputDir.getAbsolutePath(), FileNameUtils.getBaseName(imageFile.getName()) + "-resized." + outputFormat.toLowerCase());
        File outputFile = path.toFile();

        if (outputFile.exists()) {
            outputFile.delete();
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName(outputFormat.toLowerCase()).next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality((float) imgQuality);
        writer.setOutput(ImageIO.createImageOutputStream(outputFile));

        writer.write(null, new IIOImage(newBufferedImage, null, null), param);

        return null;
    }
}
