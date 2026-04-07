package com.motorcycledealer;

/**
 * Represents a motorcycle in the dealership.
 * Follows OOP principles: Encapsulation with private fields and public getters/setters.
 */
public class Motorcycle {
    private String brand;
    private String model;
    private int year;
    private double price;
    private String color;
    private String imagePath;

    // Constructor
    public Motorcycle(String brand, String model, int year, double price, String color) {
        this(brand, model, year, price, color, "");
    }

    // Constructor with optional image path
    public Motorcycle(String brand, String model, int year, double price, String color, String imagePath) {
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.price = price;
        this.color = color;
        this.imagePath = imagePath == null ? "" : imagePath;
    }

    // Getters and Setters
    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath == null ? "" : imagePath;
    }

    @Override
    public String toString() {
        String imageInfo = imagePath.isEmpty() ? "" : " - image: " + imagePath;
        return brand + " " + model + " (" + year + ") - $" + String.format("%.2f", price) + " - " + color + imageInfo;
    }

    // Main method for testing the Motorcycle class
    public static void main(String[] args) {
        // Create sample motorcycles
        Motorcycle moto1 = new Motorcycle("Honda", "CBR600RR", 2020, 12000.00, "Red");
        Motorcycle moto2 = new Motorcycle("Yamaha", "YZF-R1", 2021, 17000.00, "Blue");
        Motorcycle moto3 = new Motorcycle("Kawasaki", "Ninja ZX-10R", 2019, 15000.00, "Black");

        // Print details
        System.out.println("Sample Motorcycles:");
        System.out.println(moto1);
        System.out.println(moto2);
        System.out.println(moto3);

        // Demonstrate getters and setters
        System.out.println("\nDemonstrating getters and setters:");
        System.out.println("Original price of moto1: $" + moto1.getPrice());
        moto1.setPrice(13000.00);
        System.out.println("Updated price of moto1: $" + moto1.getPrice());
    }
}
