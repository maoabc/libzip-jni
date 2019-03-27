package mao.archive.libzip;

import java.io.IOException;

import androidx.annotation.Keep;

@Keep
public class PasswordException extends IOException{
    public PasswordException() {
    }

    public PasswordException(String message) {
        super(message);
    }
}
