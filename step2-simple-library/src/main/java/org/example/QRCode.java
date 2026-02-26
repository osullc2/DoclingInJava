package org.example;

/**
 * Java interface that mirrors the parts of the Python qrcode API we use.
 */
interface QRCode {
    PyPNGImage make(String data);

    interface PyPNGImage {
        void save(IO.BytesIO bio);
    }
}

