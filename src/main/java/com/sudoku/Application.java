package com.sudoku;

import com.webforj.App;
import com.webforj.annotation.AppTitle;
import com.webforj.annotation.Routify;
import com.webforj.annotation.StyleSheet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Routify(packages = "com.sudoku.views")
@AppTitle("Sudoku")
@StyleSheet("ws://sudoku.css")
public class Application extends App {

    /**
     * Standard Spring Boot main method.
     * Run this class directly from your IDE — no Maven required at runtime.
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
