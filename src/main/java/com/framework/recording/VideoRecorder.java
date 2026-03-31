package com.framework.recording;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Selenium-based video recorder that captures periodic browser screenshots
 * and encodes them into an MP4 (H.264) file using JCodec.
 * <p>
 * The resulting MP4 plays inline in Allure reports and any modern browser.
 * Works in both headless and non-headless modes since it uses WebDriver's
 * screenshot capability instead of OS-level screen capture.
 */
public class VideoRecorder {

    private static final Logger LOG = LogManager.getLogger(VideoRecorder.class);
    private static final int CAPTURE_INTERVAL_MS = 500;
    private static final int FRAME_RATE = 2;

    private final File outputDir;
    private final List<BufferedImage> frames = new ArrayList<>();
    private Thread captureThread;
    private final AtomicBoolean recording = new AtomicBoolean(false);

    public VideoRecorder(File outputDir) {
        this.outputDir = outputDir;
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    /**
     * Start recording by capturing screenshots from the given WebDriver at regular intervals.
     */
    public void start(WebDriver driver) {
        if (!(driver instanceof TakesScreenshot)) {
            LOG.warn("WebDriver does not support screenshots — video recording disabled");
            return;
        }

        recording.set(true);
        frames.clear();

        captureThread = new Thread(() -> {
            while (recording.get()) {
                try {
                    byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshotBytes));
                    if (image != null) {
                        synchronized (frames) {
                            frames.add(image);
                        }
                    }
                } catch (Exception e) {
                    if (!recording.get()) break;
                    LOG.warn("Screenshot capture failed: {}", e.getMessage());
                }
                try {
                    Thread.sleep(CAPTURE_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "video-recorder");
        captureThread.setDaemon(true);
        captureThread.start();
        LOG.info("Video recording started (screenshot-based, {}ms interval)", CAPTURE_INTERVAL_MS);
    }

    /**
     * Stop recording and write captured frames to an MP4 file.
     *
     * @return the generated MP4 video file, or null if recording failed
     */
    public File stop() {
        recording.set(false);
        if (captureThread != null) {
            try {
                captureThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        List<BufferedImage> capturedFrames;
        synchronized (frames) {
            capturedFrames = new ArrayList<>(frames);
            frames.clear();
        }

        if (capturedFrames.isEmpty()) {
            LOG.warn("No frames captured — no video file generated");
            return null;
        }

        LOG.info("Encoding {} frames to MP4...", capturedFrames.size());
        File videoFile = new File(outputDir, "recording-" + System.currentTimeMillis() + ".mp4");
        try {
            encodeMp4(videoFile, capturedFrames);
            LOG.info("Video recording saved: {} ({} frames, {} bytes)",
                    videoFile.getName(), capturedFrames.size(), videoFile.length());
            return videoFile;
        } catch (Exception e) {
            LOG.error("Failed to write video file: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Encode a list of BufferedImages into an MP4 file using JCodec.
     * JCodec H.264 requires even width and height, so frames are padded if needed.
     */
    private void encodeMp4(File file, List<BufferedImage> images) throws Exception {
        AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(file, FRAME_RATE);
        for (BufferedImage image : images) {
            BufferedImage safeImage = ensureEvenDimensions(image);
            encoder.encodeImage(safeImage);
        }
        encoder.finish();
    }

    /**
     * H.264 requires width and height to be even numbers.
     * If the screenshot has odd dimensions, create a new image with 1px padding.
     */
    private BufferedImage ensureEvenDimensions(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int newW = (w % 2 != 0) ? w + 1 : w;
        int newH = (h % 2 != 0) ? h + 1 : h;

        if (newW == w && newH == h) {
            return img;
        }

        BufferedImage padded = new BufferedImage(newW, newH, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = padded.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, newW, newH);
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return padded;
    }
}
