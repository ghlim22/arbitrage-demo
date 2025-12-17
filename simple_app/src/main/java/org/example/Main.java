package org.example;

import org.example.upbit.UpbitClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class Main {
    static void main(String[] args) {
        try {
            SpringApplication.run(Main.class, args);
            UpbitClient upbitClient = new UpbitClient();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }
}
