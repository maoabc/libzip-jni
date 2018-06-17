package mao.archive.libzip;

import java.io.IOException;

public class WrongPasswordException extends IOException{
    public WrongPasswordException() {
    }

    public WrongPasswordException(String message) {
        super(message);
    }
}
