# Motorcycle Dealer Stand

A simple Java Swing application for managing a motorcycle dealership inventory.

## Features

- Add new motorcycles to the inventory
- Edit selected motorcycles directly from the form
- View all motorcycles in a table
- Attach a photo path and preview the selected motorcycle image
- Remove selected motorcycles
- Persistent storage: records are saved automatically in `data/motorcycles.csv`
- Theme colors loaded from a CSS file (`src/main/resources/ui.css`)
- Follows OOP principles: Encapsulation, Abstraction
- Good UI/UX: Intuitive layout, error handling, responsive design

## Requirements

- Java 8 or higher
- JDK installed

## How to Run

1. Compile the Java files:
   ```
   javac -d . src/main/java/com/motorcycledealer/*.java
   ```

2. Run the application:
   ```
   java com.motorcycledealer.Main
   ```

## Usage

- Fill in the form fields and click "Adicionar" to create a new motorcycle record.
- (Optional) use "Browse" to attach a model photo.
- Select a row in the table to load the record into the form.
- Update the fields and click "Salvar edicao" to edit the selected record.
- Click "Remover selecionada" to remove the selected record.
- Every add/edit/remove is automatically saved to disk.

## Troubleshooting

- Ensure Java is installed: `java -version`
- If compilation fails, check for syntax errors.
- For UI issues, ensure Swing is supported on your system.
