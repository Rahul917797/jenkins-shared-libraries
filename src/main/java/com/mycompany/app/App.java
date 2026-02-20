
package com.mycompany.app;

/**
 * Hello world!
 */
public class App {

    private static final String MESSAGE = "Hello World!";

    public App() {}

    public static void main(String[] args) {
        System.out.println(MESSAGE);

        // Keep the application running to avoid CrashLoopBackOff
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            System.out.println("Application interrupted");
            Thread.currentThread().interrupt();
        }
    }

    public String getMessage() {
        return MESSAGE;
    }
}
