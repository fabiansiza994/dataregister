package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.service.impl.LambdaInvoker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lambda")
public class LambdaController {

    private final LambdaInvoker lambdaInvoker;

    public LambdaController(LambdaInvoker lambdaInvoker) {
        this.lambdaInvoker = lambdaInvoker;
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopEC2() {
        String response = lambdaInvoker.invokeFunction("stop-ec2-lambda", "{}");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start")
    public ResponseEntity<String> startEC2() {
        String response = lambdaInvoker.invokeFunction("start-ec2-lambda", "{}");
        return ResponseEntity.ok(response);
    }
}
