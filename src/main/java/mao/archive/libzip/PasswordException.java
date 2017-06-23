package mao.archive.libzip;

import java.io.IOException;

/**
 * Created by mao on 17-6-22.
 */

public class PasswordException extends IOException{
    public PasswordException() {
    }

    public PasswordException(String message) {
        super(message);
    }
}
