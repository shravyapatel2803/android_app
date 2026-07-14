package com.example.billgenerator.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class QrCodeGenerator {

    public static Bitmap generateUpiQrCode(String upiId, String name, String amount, String note) {
        // UPI URL format: upi://pay?pa=upiId&pn=name&am=amount&tn=note&cu=INR
        StringBuilder uri = new StringBuilder("upi://pay?pa=").append(upiId);
        if (name != null && !name.isEmpty()) {
            uri.append("&pn=").append(UriEncoder.encode(name));
        }
        if (amount != null && !amount.isEmpty()) {
            uri.append("&am=").append(amount);
        }
        if (note != null && !note.isEmpty()) {
            uri.append("&tn=").append(UriEncoder.encode(note));
        }
        uri.append("&cu=INR");

        return generateQrCode(uri.toString());
    }

    public static Bitmap generateQrCode(String data) {
        int width = 500;
        int height = 500;
        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class UriEncoder {
        public static String encode(String text) {
            if (text == null) return "";
            return text.replace(" ", "%20");
        }
    }
}
