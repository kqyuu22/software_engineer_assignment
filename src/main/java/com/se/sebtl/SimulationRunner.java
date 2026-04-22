package com.se.sebtl;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.se.sebtl.service.iot.HardwareSimulatorService;
import com.se.sebtl.service.IoTManagerService;
import com.se.sebtl.model.Role;

import java.util.Scanner;

@Component
public class SimulationRunner implements CommandLineRunner {

    private final HardwareSimulatorService hardwareSimulator;
    private final IoTManagerService iotManager;

    public SimulationRunner(HardwareSimulatorService hardwareSimulator, IoTManagerService iotManager) {
        this.hardwareSimulator = hardwareSimulator;
        this.iotManager = iotManager;
    }

    @Override
    @SuppressWarnings("resource") // Safe to suppress: closing System.in crashes console applications
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=========================================");
        System.out.println("  PARKING LOT CLI SIMULATION ACTIVE");
        System.out.println("=========================================\n");

        while (true) {
            System.out.println("\n--- Select an Action ---");
            System.out.println("1. System: Assign a spot (Simulate Entry Kiosk)");
            System.out.println("2. Hardware: Trigger Sensor Occupied (Car arrives at spot)");
            System.out.println("3. Hardware: Trigger Sensor Available (Car leaves spot)");
            System.out.println("4. Hardware: Trigger Sensor Failure");
            System.out.println("5. Hardware: Fix Sensor Failure (Reboot)");
            System.out.println("6. Exit Simulation");
            System.out.print("> ");

            String input = scanner.nextLine();

            try {
                switch (input) {
                    case "1":
                        System.out.print("Enter Role (STAFF, LECTURER, STUDENT, OTHER): ");
                        Role role = Role.valueOf(scanner.nextLine().toUpperCase());
                        int assignedSpot = iotManager.assignSpot(role);
                        
                        if (assignedSpot != -1) {
                            System.out.println(">> Spot " + assignedSpot + " assigned and RESERVED. Timer started.");
                        } else {
                            System.out.println(">> Lot is full or no priority spots available for this role.");
                        }
                        break;
                    case "2":
                        System.out.print("Enter assigned slot ID (1-300): ");
                        hardwareSimulator.simulateCarArrival(Integer.parseInt(scanner.nextLine()));
                        break;
                    case "3":
                        System.out.print("Enter slot ID (1-300): ");
                        hardwareSimulator.simulateCarDeparture(Integer.parseInt(scanner.nextLine()));
                        break;
                    case "4":
                        System.out.print("Enter slot ID (1-300): ");
                        hardwareSimulator.simulateSensorFailure(Integer.parseInt(scanner.nextLine()));
                        break;
                    case "5": // NEW CASE
                        System.out.print("Enter broken slot ID to fix (1-300): ");
                        hardwareSimulator.simulateSensorFix(Integer.parseInt(scanner.nextLine()));
                        break;
                    case "6": // MOVED TO 6
                        System.out.println("Exiting simulation...");
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid option. Try again.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println(">> Error: Invalid Role. Please type STAFF, LECTURER, STUDENT, or OTHER.");
            } catch (Exception e) {
                System.out.println(">> Error processing input: " + e.getMessage());
            }
        }
    }
}