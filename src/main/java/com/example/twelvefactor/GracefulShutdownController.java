package com.example.twelvefactor;

import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GracefulShutdownController {

    @GetMapping("/graceful-wait")
    @SneakyThrows
    public String waitGraceful(){
        Thread.sleep(20000);
        return "I waited for 20 seconds";
    }
}
