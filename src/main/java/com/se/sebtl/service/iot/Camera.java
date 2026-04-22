package com.se.sebtl.service.iot;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class Camera {
    private Random random = new Random();

    private String generatePlate() {
        int part1 = 10 + random.nextInt(90);                    
        char letter = (char) ('A' + random.nextInt(26));        
        int part2 = 10000 + random.nextInt(90000);              
        return part1 + "" + letter + "-" + part2;
    }

    public String capture() { return generatePlate(); }

    public String captureCorrect(String expectedPlate) { return expectedPlate; }

    public String captureWrongPlate() { return generatePlate(); }

    public String captureFail() { return null; }
}