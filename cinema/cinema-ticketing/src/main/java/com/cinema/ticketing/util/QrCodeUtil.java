package com.cinema.ticketing.util;// cinema-ticketing/src/main/java/com/cinema/ticketing/service/QrCodeUtil.java


import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class QrCodeUtil {

    @Value("${file.static.path:D:/cinema/static}")
    private String staticPath;

    @Value("${qr.code.url.prefix:http://localhost:8080}")
    private String qrCodeUrlPrefix;

    public String generateQrCode(String content, String orderNo) {
        try {
            BitMatrix bitMatrix = encode(content, 300, 300);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            String fileName = orderNo + ".png";
            String relativePath = "/qrcode/" + fileName;
            String absolutePath = staticPath + relativePath;

            File file = new File(absolutePath);
            file.getParentFile().mkdirs();
            ImageIO.write(image, "PNG", file);
            log.info("二维码生成成功: {}", relativePath);

            return relativePath;

        } catch (Exception e) {
            log.error("二维码生成失败", e);
            throw new RuntimeException("二维码生成失败", e);
        }
    }

    private BitMatrix encode(String content, int width, int height) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        return new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
    }

    public String getQrCodeUrl(String relativePath) {
        return qrCodeUrlPrefix + relativePath;
    }

    public byte[] generateQrCodeBytes(String content, String orderNo) {
        try {
            int width = 300;
            int height = 300;

            // 生成二维码矩阵
            BitMatrix bitMatrix = new QRCodeWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    Map.of(EncodeHintType.MARGIN, 1)  // 白边宽度
            );

            // 直接写入内存 ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            log.info("二维码字节数组生成成功: orderNo={}", orderNo);
            return outputStream.toByteArray();

        } catch (WriterException | IOException e) {
            log.error("二维码生成失败: orderNo={}", orderNo, e);
            throw new RuntimeException("二维码生成失败: " + e.getMessage());
        }
    }

}