import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Console-based Vehicle Rental Management System
 * Features:
 *  - Add / Remove / List vehicles
 *  - Register / List customers
 *  - Book vehicle for date range (per-day tariff)
 *  - Generate invoice & view bookings
 *  - Return vehicle (mark available)
 *
 * Single-file program for easy compilation & testing.
 */
public class VehicleRentalSystem {
    private static Scanner sc = new Scanner(System.in);
    private static Map<Integer, Vehicle> vehicles = new LinkedHashMap<>();
    private static Map<Integer, Customer> customers = new LinkedHashMap<>();
    private static Map<Integer, Booking> bookings = new LinkedHashMap<>();

    private static int nextVehicleId = 1;
    private static int nextCustomerId = 1;
    private static int nextBookingId = 1;

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n=== Vehicle Rental Management ===");
            System.out.println("1. Vehicle management");
            System.out.println("2. Customer management");
            System.out.println("3. Book vehicle");
            System.out.println("4. View bookings / Invoice");
            System.out.println("5. Return vehicle");
            System.out.println("0. Exit");
            System.out.print("Choose: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> vehicleMenu();
                case "2" -> customerMenu();
                case "3" -> bookVehicle();
                case "4" -> viewBookingsMenu();
                case "5" -> returnVehicle();
                case "0" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    /* ---------------- Vehicle ---------------- */
    private static void vehicleMenu() {
        while (true) {
            System.out.println("\n--- Vehicle Management ---");
            System.out.println("1. Add vehicle");
            System.out.println("2. Remove vehicle");
            System.out.println("3. List vehicles");
            System.out.println("0. Back");
            System.out.print("Choose: ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1" -> addVehicle();
                case "2" -> removeVehicle();
                case "3" -> listVehicles();
                case "0" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void addVehicle() {
        System.out.print("Enter vehicle type (Car/Bike/...): ");
        String type = sc.nextLine().trim();
        System.out.print("Enter model/name: ");
        String model = sc.nextLine().trim();
        double rate;
        while (true) {
            System.out.print("Enter rate per day (numeric): ");
            String r = sc.nextLine().trim();
            try {
                rate = Double.parseDouble(r);
                if (rate < 0) throw new NumberFormatException();
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid rate. Try again.");
            }
        }
        Vehicle v = new Vehicle(nextVehicleId++, type, model, rate, true);
        vehicles.put(v.id, v);
        System.out.println("Added: " + v);
    }

    private static void removeVehicle() {
        listVehicles();
        System.out.print("Enter vehicle ID to remove: ");
        String s = sc.nextLine().trim();
        try {
            int id = Integer.parseInt(s);
            Vehicle v = vehicles.get(id);
            if (v == null) System.out.println("No such vehicle.");
            else {
                // prevent removing if booked & active
                boolean activeBooking = bookings.values().stream()
                        .anyMatch(b -> b.vehicleId == id && !b.isReturned());
                if (activeBooking) {
                    System.out.println("Cannot remove: vehicle has an active booking.");
                    return;
                }
                vehicles.remove(id);
                System.out.println("Removed vehicle ID " + id);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
        }
    }

    private static void listVehicles() {
        System.out.println("\nVehicles:");
        if (vehicles.isEmpty()) {
            System.out.println("  (no vehicles)");
            return;
        }
        System.out.printf("%-5s %-10s %-18s %-10s %-10s\n", "ID", "Type", "Model", "Rate/day", "Available");
        for (Vehicle v : vehicles.values()) {
            System.out.printf("%-5d %-10s %-18s %-10.2f %-10s\n",
                    v.id, v.type, v.model, v.ratePerDay, v.available ? "Yes" : "No");
        }
    }

    /* ---------------- Customer ---------------- */
    private static void customerMenu() {
        while (true) {
            System.out.println("\n--- Customer Management ---");
            System.out.println("1. Register customer");
            System.out.println("2. List customers");
            System.out.println("0. Back");
            System.out.print("Choose: ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1" -> registerCustomer();
                case "2" -> listCustomers();
                case "0" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void registerCustomer() {
        System.out.print("Enter name: ");
        String name = sc.nextLine().trim();
        System.out.print("Enter phone: ");
        String phone = sc.nextLine().trim();
        Customer c = new Customer(nextCustomerId++, name, phone);
        customers.put(c.id, c);
        System.out.println("Registered: " + c);
    }

    private static void listCustomers() {
        System.out.println("\nCustomers:");
        if (customers.isEmpty()) {
            System.out.println("  (no customers)");
            return;
        }
        System.out.printf("%-5s %-20s %-15s\n", "ID", "Name", "Phone");
        for (Customer c : customers.values()) {
            System.out.printf("%-5d %-20s %-15s\n", c.id, c.name, c.phone);
        }
    }

    /* ---------------- Booking ---------------- */
    private static void bookVehicle() {
        if (vehicles.isEmpty()) {
            System.out.println("No vehicles available. Add vehicles first.");
            return;
        }
        if (customers.isEmpty()) {
            System.out.println("No customers registered. Register a customer first.");
            return;
        }
        listVehicles();
        System.out.print("Enter vehicle ID to book: ");
        int vehicleId;
        try {
            vehicleId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
            return;
        }
        Vehicle v = vehicles.get(vehicleId);
        if (v == null) {
            System.out.println("Vehicle not found.");
            return;
        }
        if (!v.available) {
            System.out.println("Vehicle not available currently.");
            return;
        }

        listCustomers();
        System.out.print("Enter customer ID: ");
        int customerId;
        try {
            customerId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
            return;
        }
        Customer cust = customers.get(customerId);
        if (cust == null) {
            System.out.println("Customer not found.");
            return;
        }

        LocalDate start, end;
        try {
            System.out.print("Enter start date (YYYY-MM-DD): ");
            start = LocalDate.parse(sc.nextLine().trim());
            System.out.print("Enter end date (YYYY-MM-DD): ");
            end = LocalDate.parse(sc.nextLine().trim());
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format. Use YYYY-MM-DD.");
            return;
        }
        if (end.isBefore(start)) {
            System.out.println("End date cannot be before start date.");
            return;
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1; // inclusive
        double total = days * v.ratePerDay;

        Booking b = new Booking(nextBookingId++, vehicleId, customerId, start, end, total, false);
        bookings.put(b.id, b);
        v.available = false; // mark unavailable until returned
        System.out.println("Booking successful. Booking ID: " + b.id);
        printInvoice(b);
    }

    private static void viewBookingsMenu() {
        System.out.println("\n--- Bookings ---");
        if (bookings.isEmpty()) {
            System.out.println("No bookings yet.");
            return;
        }
        System.out.printf("%-5s %-8s %-8s %-12s %-12s %-8s\n", "BID", "VehID", "CustID", "Start", "End", "Cost");
        for (Booking b : bookings.values()) {
            System.out.printf("%-5d %-8d %-8d %-12s %-12s %-8.2f%s\n",
                    b.id, b.vehicleId, b.customerId, b.startDate, b.endDate, b.totalCost, b.isReturned() ? " (Returned)" : "");
        }
        System.out.print("Enter booking ID to view invoice (or press Enter to go back): ");
        String s = sc.nextLine().trim();
        if (s.isEmpty()) return;
        try {
            int bid = Integer.parseInt(s);
            Booking b = bookings.get(bid);
            if (b == null) System.out.println("No such booking.");
            else printInvoice(b);
        } catch (NumberFormatException e) {
            System.out.println("Invalid booking ID.");
        }
    }

    private static void printInvoice(Booking b) {
        Vehicle v = vehicles.get(b.vehicleId);
        Customer c = customers.get(b.customerId);
        System.out.println("\n--- Invoice ---");
        System.out.println("Booking ID: " + b.id);
        System.out.println("Customer: " + c.name + " (ID " + c.id + ")");
        System.out.println("Phone: " + c.phone);
        System.out.println("Vehicle: " + v.type + " - " + v.model + " (ID " + v.id + ")");
        System.out.println("Period: " + b.startDate + " to " + b.endDate);
        long days = ChronoUnit.DAYS.between(b.startDate, b.endDate) + 1;
        System.out.println("Days: " + days);
        System.out.printf("Rate per day: %.2f\n", v.ratePerDay);
        System.out.printf("Total: %.2f\n", b.totalCost);
        System.out.println("Returned: " + (b.isReturned() ? "Yes" : "No"));
    }

    private static void returnVehicle() {
        System.out.println("\n--- Return Vehicle ---");
        System.out.print("Enter booking ID: ");
        String s = sc.nextLine().trim();
        int bid;
        try {
            bid = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
            return;
        }
        Booking b = bookings.get(bid);
        if (b == null) {
            System.out.println("Booking not found.");
            return;
        }
        if (b.isReturned()) {
            System.out.println("Already returned.");
            return;
        }
        // Allow user to optionally provide actual return date (to adjust cost)
        System.out.print("Enter actual return date (YYYY-MM-DD) or press Enter to use planned end date: ");
        String d = sc.nextLine().trim();
        LocalDate actual;
        if (d.isEmpty()) {
            actual = b.endDate;
        } else {
            try {
                actual = LocalDate.parse(d);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Canceling return.");
                return;
            }
        }
        if (actual.isBefore(b.startDate)) {
            System.out.println("Return date cannot be before start date.");
            return;
        }

        // Recalculate cost if returned later/earlier (still charge at least 1 day)
        long days = ChronoUnit.DAYS.between(b.startDate, actual) + 1;
        Vehicle v = vehicles.get(b.vehicleId);
        double newTotal = days * v.ratePerDay;
        b.endDate = actual;
        b.totalCost = newTotal;
        b.setReturned(true);
        // mark vehicle available
        v.available = true;
        System.out.println("Vehicle returned. Updated booking:");
        printInvoice(b);
    }

    /* ---------------- Data classes ---------------- */
    private static class Vehicle {
        int id;
        String type;
        String model;
        double ratePerDay;
        boolean available;

        Vehicle(int id, String type, String model, double ratePerDay, boolean available) {
            this.id = id;
            this.type = type;
            this.model = model;
            this.ratePerDay = ratePerDay;
            this.available = available;
        }

        @Override
        public String toString() {
            return String.format("ID:%d | %s - %s | rate/day: %.2f | available: %s",
                    id, type, model, ratePerDay, available ? "Yes" : "No");
        }
    }

    private static class Customer {
        int id;
        String name;
        String phone;

        Customer(int id, String name, String phone) {
            this.id = id;
            this.name = name;
            this.phone = phone;
        }

        @Override
        public String toString() {
            return String.format("ID:%d | %s | %s", id, name, phone);
        }
    }

    private static class Booking {
        int id;
        int vehicleId;
        int customerId;
        LocalDate startDate;
        LocalDate endDate;
        double totalCost;
        private boolean returned;

        Booking(int id, int vehicleId, int customerId, LocalDate startDate, LocalDate endDate, double totalCost, boolean returned) {
            this.id = id;
            this.vehicleId = vehicleId;
            this.customerId = customerId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.totalCost = totalCost;
            this.returned = returned;
        }

        boolean isReturned() { return returned; }
        void setReturned(boolean r) { returned = r; }
    }
}
