package com.se.sebtl.service.iot;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class Camera {
    private Random random = new Random();
    private String lastCapturedPlate;

    private String generatePlate() {
        int part1 = 10 + random.nextInt(90);                    
        char letter = (char) ('A' + random.nextInt(26));        
        int part2 = 10000 + random.nextInt(90000);              
        return part1 + "" + letter + "-" + part2;
    }

    public String capture() { 
        lastCapturedPlate = generatePlate(); 
        return lastCapturedPlate; 
    }

    public String captureCorrect(String expectedPlate) { 
        lastCapturedPlate = expectedPlate; 
        return lastCapturedPlate; 
    }

    public String captureWrongPlate() { 
        lastCapturedPlate = generatePlate(); 
        return lastCapturedPlate; 
    }

    public String captureFail() { 
        lastCapturedPlate = null; 
        return null; 
    }

    public String getLastCapturedPlate() {
        return lastCapturedPlate;
    }
}