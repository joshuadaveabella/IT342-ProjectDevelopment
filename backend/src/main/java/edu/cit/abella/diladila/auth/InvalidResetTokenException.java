package edu.cit.abella.diladila.auth;

public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException() {
        super("This reset link is invalid or has expired. Please request a new one.");
    }
}
