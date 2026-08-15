package com.factshare.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class OcrService {

    private final ITesseract tesseract;

    public OcrService() {
        this.tesseract = new Tesseract();
        String tessDataPath = resolveTessDataPath();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("eng");
    }

    /**
     * Resolve tessdata directory. Checks env var first, then tries common system paths.
     */
    private String resolveTessDataPath() {
        // 1. Check env var
        String envPath = System.getenv("TESSDATA_PREFIX");
        if (envPath != null && !envPath.isBlank() && new File(envPath).isDirectory()) {
            return envPath;
        }
        // 2. Try common paths across OS versions
        String[] candidates = {
            "/usr/share/tesseract-ocr/5/tessdata",       // Debian Bookworm / Ubuntu 24.04
            "/usr/share/tesseract-ocr/4.00/tessdata",    // Debian Bullseye / Ubuntu 22.04
            "/usr/share/tesseract-ocr/tessdata",          // Generic
            "/usr/local/share/tessdata",                   // macOS Homebrew
            "/opt/homebrew/share/tessdata"                 // macOS Homebrew (Apple Silicon)
        };
        for (String path : candidates) {
            if (new File(path).isDirectory()) {
                return path;
            }
        }
        // 3. Fallback — let tess4j try its default
        return "/usr/share/tesseract-ocr/5/tessdata";
    }

    /**
     * Extract text from an uploaded image file using Tesseract OCR.
     */
    public String extractText(MultipartFile file) throws IOException, TesseractException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IOException("Unsupported image format. Please upload a PNG, JPG, or BMP image.");
        }
        return tesseract.doOCR(image);
    }
}