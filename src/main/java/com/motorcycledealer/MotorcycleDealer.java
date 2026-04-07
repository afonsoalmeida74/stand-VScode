package com.motorcycledealer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Manages the inventory of motorcycles in the dealership.
 * Follows OOP: Encapsulation, uses composition.
 */
public class MotorcycleDealer {
    private static final Path STORAGE_FILE = Paths.get("data", "motorcycles.csv");
    private static final String FILE_HEADER = "brand,model,year,price,color,imagePath";
    private static final String LEGACY_FILE_HEADER = "brand,model,year,price,color";

    private List<Motorcycle> inventory;

    public MotorcycleDealer() {
        this.inventory = new ArrayList<>();
    }

    public void addMotorcycle(Motorcycle motorcycle) throws IOException {
        inventory.add(motorcycle);
        persistOrRollback(() -> inventory.remove(inventory.size() - 1));
    }

    public void removeMotorcycle(Motorcycle motorcycle) throws IOException {
        int index = inventory.indexOf(motorcycle);
        if (index >= 0) {
            removeMotorcycleByIndex(index);
        }
    }

    public void removeMotorcycleByIndex(int index) throws IOException {
        Motorcycle removed = inventory.remove(index);
        persistOrRollback(() -> inventory.add(index, removed));
    }

    public Motorcycle getMotorcycleAt(int index) {
        return inventory.get(index);
    }

    public void updateMotorcycle(int index, Motorcycle motorcycle) throws IOException {
        Motorcycle previousMotorcycle = inventory.set(index, motorcycle);
        persistOrRollback(() -> inventory.set(index, previousMotorcycle));
    }

    public List<Motorcycle> getInventory() {
        return new ArrayList<>(inventory); // Return a copy to prevent external modification
    }

    public int getInventorySize() {
        return inventory.size();
    }

    public Path getStorageFile() {
        return STORAGE_FILE;
    }

    public int loadInventory() throws IOException {
        inventory.clear();

        if (!Files.exists(STORAGE_FILE)) {
            return 0;
        }

        List<String> lines = Files.readAllLines(STORAGE_FILE, StandardCharsets.UTF_8);
        int loadedCount = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }

            if (i == 0 && isHeaderLine(line)) {
                continue;
            }

            Motorcycle motorcycle = parseMotorcycleLine(line);
            if (motorcycle != null) {
                inventory.add(motorcycle);
                loadedCount++;
            }
        }

        return loadedCount;
    }

    private Motorcycle parseMotorcycleLine(String line) {
        List<String> fields = parseCsvLine(line);
        if (fields.size() != 5 && fields.size() != 6) {
            return null;
        }

        try {
            String brand = fields.get(0);
            String model = fields.get(1);
            int year = Integer.parseInt(fields.get(2));
            double price = Double.parseDouble(fields.get(3));
            String color = fields.get(4);
            String imagePath = fields.size() >= 6 ? fields.get(5) : "";
            return new Motorcycle(brand, model, year, price, color, imagePath);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isHeaderLine(String line) {
        String normalized = line.toLowerCase(Locale.ROOT);
        return normalized.equals(FILE_HEADER.toLowerCase(Locale.ROOT))
                || normalized.equals(LEGACY_FILE_HEADER.toLowerCase(Locale.ROOT));
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);

            if (currentChar == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    currentField.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (currentChar == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(currentChar);
            }
        }

        fields.add(currentField.toString());
        return fields;
    }

    private void persistOrRollback(Runnable rollbackAction) throws IOException {
        try {
            saveInventory();
        } catch (IOException ex) {
            rollbackAction.run();
            throw ex;
        }
    }

    private void saveInventory() throws IOException {
        Path parent = STORAGE_FILE.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<String> lines = new ArrayList<>();
        lines.add(FILE_HEADER);

        for (Motorcycle motorcycle : inventory) {
            lines.add(toCsvLine(motorcycle));
        }

        Files.write(
                STORAGE_FILE,
                lines,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private String toCsvLine(Motorcycle motorcycle) {
        String brand = csvEscape(motorcycle.getBrand());
        String model = csvEscape(motorcycle.getModel());
        String year = String.valueOf(motorcycle.getYear());
        String price = String.format(java.util.Locale.US, "%.2f", motorcycle.getPrice());
        String color = csvEscape(motorcycle.getColor());
        String imagePath = csvEscape(motorcycle.getImagePath());

        return String.join(",", brand, model, year, price, color, imagePath);
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "\"\"";
        }

        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
