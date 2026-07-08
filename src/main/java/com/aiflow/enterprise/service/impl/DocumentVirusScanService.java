package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.entity.embedded.VirusScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class DocumentVirusScanService {

    private static final Logger log = LoggerFactory.getLogger(DocumentVirusScanService.class);

    private static final List<String> EXECUTABLE_EXTENSIONS = Arrays.asList(
            ".exe", ".bat", ".cmd", ".com", ".msi", ".scr", ".pif", ".vbs",
            ".js", ".jar", ".class", ".wsf", ".ps1", ".sh", ".bin");

    private final boolean enabled;
    private final String clamHost;
    private final int clamPort;
    private final int clamTimeout;
    private final int maxScanSize;

    public DocumentVirusScanService(
            @Value("${app.document.virus-scan.enabled:false}") boolean enabled,
            @Value("${app.document.virus-scan.clam-host:localhost}") String clamHost,
            @Value("${app.document.virus-scan.clam-port:3310}") int clamPort,
            @Value("${app.document.virus-scan.timeout:30000}") int clamTimeout,
            @Value("${app.document.virus-scan.max-size:104857600}") int maxScanSize) {
        this.enabled = enabled;
        this.clamHost = clamHost;
        this.clamPort = clamPort;
        this.clamTimeout = clamTimeout;
        this.maxScanSize = maxScanSize;
    }

    public VirusScanResult scan(byte[] fileData, String fileName, String mimeType) {
        if (!enabled) {
            return VirusScanResult.builder()
                    .status(VirusScanResult.VirusScanStatus.SKIPPED)
                    .scannerName("disabled")
                    .scannedAt(Instant.now())
                    .details("Virus scanning is disabled")
                    .build();
        }

        if (fileData == null || fileData.length == 0) {
            return VirusScanResult.builder()
                    .status(VirusScanResult.VirusScanStatus.SKIPPED)
                    .scannerName("heuristic")
                    .scannedAt(Instant.now())
                    .details("Empty file - skipping scan")
                    .build();
        }

        if (fileData.length > maxScanSize) {
            log.warn("File too large for virus scan: {} bytes, max is {}", fileData.length, maxScanSize);
            return heuristicScanFallback(fileName, mimeType, fileData);
        }

        long start = System.currentTimeMillis();
        try {
            return scanWithClamAv(fileData, start);
        } catch (Exception e) {
            log.warn("ClamAV scan failed ({}), falling back to heuristic scan: {}", e.getMessage(),
                    fileName);
            return heuristicScanFallback(fileName, mimeType, fileData);
        }
    }

    private VirusScanResult scanWithClamAv(byte[] fileData, long start) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(clamHost, clamPort), clamTimeout);
            socket.setSoTimeout(clamTimeout);

            try (OutputStream out = socket.getOutputStream();
                 InputStream in = socket.getInputStream()) {

                byte[] command = "zINSTREAM\0".getBytes();
                out.write(command);
                out.flush();

                byte[] chunk = new byte[8192];
                try (ByteArrayInputStream bis = new ByteArrayInputStream(fileData)) {
                    int read;
                    while ((read = bis.read(chunk)) != -1) {
                        byte[] length = java.nio.ByteBuffer.allocate(4)
                                .putInt(read).array();
                        out.write(length);
                        out.write(chunk, 0, read);
                    }
                }

                byte[] zeroLength = java.nio.ByteBuffer.allocate(4).putInt(0).array();
                out.write(zeroLength);
                out.flush();

                byte[] response = in.readAllBytes();
                String result = new String(response);
                long elapsed = System.currentTimeMillis() - start;

                if (result.contains("OK")) {
                    return VirusScanResult.builder()
                            .status(VirusScanResult.VirusScanStatus.CLEAN)
                            .scannerName("ClamAV")
                            .scannerVersion("clamd")
                            .scannedAt(Instant.now())
                            .scanDurationMs(elapsed)
                            .details("File is clean")
                            .build();
                } else if (result.contains("FOUND")) {
                    String threat = extractThreatName(result);
                    return VirusScanResult.builder()
                            .status(VirusScanResult.VirusScanStatus.INFECTED)
                            .scannerName("ClamAV")
                            .scannerVersion("clamd")
                            .scannedAt(Instant.now())
                            .scanDurationMs(elapsed)
                            .threatName(threat)
                            .details("Threat detected: " + threat)
                            .build();
                } else {
                    return VirusScanResult.builder()
                            .status(VirusScanResult.VirusScanStatus.ERROR)
                            .scannerName("ClamAV")
                            .scannedAt(Instant.now())
                            .scanDurationMs(elapsed)
                            .details("Unexpected response: " + result.trim())
                            .build();
                }
            }
        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - start;
            return VirusScanResult.builder()
                    .status(VirusScanResult.VirusScanStatus.ERROR)
                    .scannerName("ClamAV")
                    .scannedAt(Instant.now())
                    .scanDurationMs(elapsed)
                    .details("Connection failed: " + e.getMessage())
                    .build();
        }
    }

    private VirusScanResult heuristicScanFallback(String fileName, String mimeType, byte[] fileData) {
        long start = System.currentTimeMillis();
        String ext = fileName != null && fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : "";

        if (EXECUTABLE_EXTENSIONS.contains(ext)) {
            long elapsed = System.currentTimeMillis() - start;
            return VirusScanResult.builder()
                    .status(VirusScanResult.VirusScanStatus.SUSPICIOUS)
                    .scannerName("heuristic")
                    .scannedAt(Instant.now())
                    .scanDurationMs(elapsed)
                    .threatName("EXECUTABLE_EXTENSION")
                    .details("Executable file extension blocked: " + ext)
                    .build();
        }

        if (fileData.length > 0 && fileData[0] == 0x7f
                && fileData.length > 1 && fileData[1] == 0x45
                && fileData.length > 2 && fileData[2] == 0x4c
                && fileData.length > 3 && fileData[3] == 0x46) {
            long elapsed = System.currentTimeMillis() - start;
            return VirusScanResult.builder()
                    .status(VirusScanResult.VirusScanStatus.SUSPICIOUS)
                    .scannerName("heuristic")
                    .scannedAt(Instant.now())
                    .scanDurationMs(elapsed)
                    .threatName("ELF_BINARY")
                    .details("ELF binary detected")
                    .build();
        }

        long elapsed = System.currentTimeMillis() - start;
        return VirusScanResult.builder()
                .status(VirusScanResult.VirusScanStatus.CLEAN)
                .scannerName("heuristic")
                .scannedAt(Instant.now())
                .scanDurationMs(elapsed)
                .details("Heuristic scan passed (ClamAV unavailable)")
                .build();
    }

    private String extractThreatName(String response) {
        int colonIdx = response.indexOf(':');
        if (colonIdx >= 0) {
            String afterColon = response.substring(colonIdx + 1).trim();
            int spaceIdx = afterColon.indexOf(' ');
            return spaceIdx >= 0 ? afterColon.substring(0, spaceIdx) : afterColon;
        }
        return response.trim();
    }
}
