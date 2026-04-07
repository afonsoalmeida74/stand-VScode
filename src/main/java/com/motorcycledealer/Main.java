package com.motorcycledealer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main application class for the Motorcycle Dealer Swing UI.
 * Implements MVC: This class acts as View and Controller, using MotorcycleDealer as Model.
 */
public class Main extends JFrame {
    private static final String CSS_FILE = "src/main/resources/ui.css";

    private final MotorcycleDealer dealer;
    private final Map<String, Color> themeColors;

    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private JTable table;
    private JTextField searchField;
    private JLabel tableInfoLabel;

    private JTextField brandField;
    private JTextField modelField;
    private JTextField yearField;
    private JTextField priceField;
    private JTextField colorField;
    private JTextField imagePathField;

    private JButton editButton;
    private JButton removeButton;

    private JLabel formTitleLabel;
    private JLabel statusLabel;
    private JLabel imagePreviewLabel;
    private JLabel inventoryCountValueLabel;
    private JLabel averagePriceValueLabel;
    private JLabel newestYearValueLabel;

    private int editingRowIndex = -1;
    private String startupStatusMessage;
    private String startupStatusColorKey = "--text-muted";

    public Main() {
        dealer = new MotorcycleDealer();
        themeColors = loadCssPalette();
        loadPersistedInventory();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Motorcycle Dealer Stand");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 730));
        setSize(1180, 760);
        setLocationRelativeTo(null);

        JPanel mainPanel = new GradientPanel(color("--bg-gradient-start"), color("--bg-gradient-end"));
        mainPanel.setLayout(new BorderLayout(18, 18));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        mainPanel.add(buildHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(buildBodyPanel(), BorderLayout.CENTER);

        setContentPane(mainPanel);
        updateTable();
        resetForm(false);

        if (startupStatusMessage != null && !startupStatusMessage.isEmpty()) {
            showStatus(startupStatusMessage, startupStatusColorKey);
        }
    }

    private void loadPersistedInventory() {
        try {
            int loadedCount = dealer.loadInventory();
            if (loadedCount > 0) {
                startupStatusMessage = String.format(Locale.US, "%d records loaded from disk.", loadedCount);
                startupStatusColorKey = "--success";
            } else {
                startupStatusMessage = "No saved records found. Add your first motorcycle.";
                startupStatusColorKey = "--text-muted";
            }
        } catch (IOException ex) {
            startupStatusMessage = "Could not load saved records from disk.";
            startupStatusColorKey = "--danger";
        }
    }

    private JPanel buildHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(16, 0));
        headerPanel.setBackground(color("--hero-bg"));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color("--hero-border"), 1, true),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Motorcycle Dealer Dashboard");
        titleLabel.setFont(new Font("Bahnschrift", Font.BOLD, 33));
        titleLabel.setForeground(color("--hero-text"));

        JLabel subtitleLabel = new JLabel("Search-first inventory control with cleaner visual hierarchy.");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(color("--hero-muted"));

        titleBlock.add(titleLabel);
        titleBlock.add(Box.createVerticalStrut(6));
        titleBlock.add(subtitleLabel);

        JLabel badgeLabel = new JLabel("LIVE STAND");
        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badgeLabel.setOpaque(true);
        badgeLabel.setForeground(color("--on-primary"));
        badgeLabel.setBackground(color("--primary"));
        badgeLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        JPanel badgePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgePanel.setOpaque(false);
        badgePanel.add(badgeLabel);

        headerPanel.add(titleBlock, BorderLayout.WEST);
        headerPanel.add(badgePanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JPanel buildBodyPanel() {
        JPanel bodyPanel = new JPanel(new GridBagLayout());
        bodyPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        gbc.gridx = 0;
        gbc.weightx = 0.68;
        gbc.insets = new Insets(0, 0, 0, 12);
        bodyPanel.add(buildInventoryPanel(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.32;
        gbc.insets = new Insets(0, 12, 0, 0);
        bodyPanel.add(buildFormPanel(), gbc);

        return bodyPanel;
    }

    private JPanel buildInventoryPanel() {
        JPanel inventoryPanel = new JPanel(new BorderLayout(14, 14));
        inventoryPanel.setBackground(color("--card-bg"));
        inventoryPanel.setBorder(createCardBorder());

        JPanel topArea = new JPanel();
        topArea.setOpaque(false);
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
        topArea.add(buildStatsPanel());
        topArea.add(Box.createVerticalStrut(12));
        topArea.add(buildInventoryToolbar());

        inventoryPanel.add(topArea, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Brand", "Model", "Year", "Price", "Color"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        tableSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(tableSorter);

        styleTable();

        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                onRowSelected();
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(color("--card-bg"));

        inventoryPanel.add(scrollPane, BorderLayout.CENTER);
        return inventoryPanel;
    }

    private JPanel buildStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        statsPanel.setOpaque(false);

        inventoryCountValueLabel = new JLabel("0");
        averagePriceValueLabel = new JLabel("$0");
        newestYearValueLabel = new JLabel("-");

        statsPanel.add(createStatCard("Bikes In Stock", inventoryCountValueLabel, "--primary"));
        statsPanel.add(createStatCard("Average Price", averagePriceValueLabel, "--success"));
        statsPanel.add(createStatCard("Newest Year", newestYearValueLabel, "--neutral"));

        return statsPanel;
    }

    private JPanel createStatCard(String labelText, JLabel valueLabel, String accentColorKey) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(color("--stat-bg"));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color("--border-color"), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(color("--text-muted"));

        valueLabel.setFont(new Font("Bahnschrift", Font.BOLD, 25));
        valueLabel.setForeground(color(accentColorKey));

        card.add(label, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildInventoryToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel inventoryLabel = new JLabel("Inventory");
        inventoryLabel.setFont(new Font("Bahnschrift", Font.BOLD, 22));
        inventoryLabel.setForeground(color("--text-main"));

        tableInfoLabel = new JLabel("0 motorcycles");
        tableInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableInfoLabel.setForeground(color("--text-muted"));

        left.add(inventoryLabel);
        left.add(Box.createVerticalStrut(4));
        left.add(tableInfoLabel);

        JPanel right = new JPanel(new BorderLayout(8, 0));
        right.setOpaque(false);

        searchField = createSearchField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyTableFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyTableFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyTableFilter();
            }
        });

        JButton clearFilterButton = new JButton("Reset");
        styleSecondaryButton(clearFilterButton);
        clearFilterButton.addActionListener(event -> searchField.setText(""));

        right.add(searchField, BorderLayout.CENTER);
        right.add(clearFilterButton, BorderLayout.EAST);

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);

        return toolbar;
    }

    private JPanel buildFormPanel() {
        JPanel formPanel = new JPanel(new BorderLayout(0, 12));
        formPanel.setBackground(color("--card-bg"));
        formPanel.setBorder(createCardBorder());

        JPanel formHeader = new JPanel();
        formHeader.setOpaque(false);
        formHeader.setLayout(new BoxLayout(formHeader, BoxLayout.Y_AXIS));

        formTitleLabel = new JLabel("Add Motorcycle");
        formTitleLabel.setFont(new Font("Bahnschrift", Font.BOLD, 27));
        formTitleLabel.setForeground(color("--text-main"));

        statusLabel = new JLabel("Fill every field and click Add.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(color("--text-muted"));

        formHeader.add(formTitleLabel);
        formHeader.add(Box.createVerticalStrut(6));
        formHeader.add(statusLabel);

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 0, 6, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        brandField = createInputField();
        modelField = createInputField();
        yearField = createInputField();
        priceField = createInputField();
        colorField = createInputField();
        imagePathField = createInputField();

        addFormField(fieldsPanel, gbc, 0, "Brand", brandField);
        addFormField(fieldsPanel, gbc, 1, "Model", modelField);
        addFormField(fieldsPanel, gbc, 2, "Year", yearField);
        addFormField(fieldsPanel, gbc, 3, "Price", priceField);
        addFormField(fieldsPanel, gbc, 4, "Color", colorField);
        addImagePathField(fieldsPanel, gbc, 5);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setOpaque(false);
        centerPanel.add(fieldsPanel, BorderLayout.NORTH);
        centerPanel.add(buildImagePreviewPanel(), BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        actionsPanel.setOpaque(false);

        JButton addButton = new JButton("Add");
        stylePrimaryButton(addButton, "--primary");
        addButton.addActionListener(new AddMotorcycleListener());

        editButton = new JButton("Save");
        stylePrimaryButton(editButton, "--success");
        editButton.addActionListener(new EditMotorcycleListener());

        removeButton = new JButton("Delete");
        stylePrimaryButton(removeButton, "--danger");
        removeButton.addActionListener(new RemoveMotorcycleListener());

        JButton clearButton = new JButton("Clear");
        styleSecondaryButton(clearButton);
        clearButton.addActionListener(event -> resetForm(true));

        actionsPanel.add(addButton);
        actionsPanel.add(editButton);
        actionsPanel.add(removeButton);
        actionsPanel.add(clearButton);

        formPanel.add(formHeader, BorderLayout.NORTH);
        formPanel.add(centerPanel, BorderLayout.CENTER);
        formPanel.add(actionsPanel, BorderLayout.SOUTH);

        return formPanel;
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String labelText, JTextField field) {
        gbc.gridy = row * 2;

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(color("--text-main"));
        panel.add(label, gbc);

        gbc.gridy = row * 2 + 1;
        panel.add(field, gbc);
    }

    private void addImagePathField(JPanel panel, GridBagConstraints gbc, int row) {
        gbc.gridy = row * 2;

        JLabel label = new JLabel("Photo Path (optional)");
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(color("--text-main"));
        panel.add(label, gbc);

        JButton browseButton = new JButton("Browse");
        styleSecondaryButton(browseButton);
        browseButton.addActionListener(event -> chooseImageFile());

        JPanel pathInputPanel = new JPanel(new BorderLayout(8, 0));
        pathInputPanel.setOpaque(false);
        pathInputPanel.add(imagePathField, BorderLayout.CENTER);
        pathInputPanel.add(browseButton, BorderLayout.EAST);

        imagePathField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshImagePreviewFromInput();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshImagePreviewFromInput();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshImagePreviewFromInput();
            }
        });

        gbc.gridy = row * 2 + 1;
        panel.add(pathInputPanel, gbc);
    }

    private JPanel buildImagePreviewPanel() {
        JPanel previewPanel = new JPanel(new BorderLayout(0, 8));
        previewPanel.setOpaque(false);

        JLabel previewTitle = new JLabel("Model Photo Preview");
        previewTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        previewTitle.setForeground(color("--text-main"));

        imagePreviewLabel = new JLabel("", SwingConstants.CENTER);
        imagePreviewLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        imagePreviewLabel.setForeground(color("--text-muted"));
        imagePreviewLabel.setOpaque(true);
        imagePreviewLabel.setBackground(color("--field-bg"));
        imagePreviewLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color("--border-color"), 1, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        imagePreviewLabel.setPreferredSize(new Dimension(260, 170));

        previewPanel.add(previewTitle, BorderLayout.NORTH);
        previewPanel.add(imagePreviewLabel, BorderLayout.CENTER);

        setEmptyImagePreview("No photo selected.");
        return previewPanel;
    }

    private void styleTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(color("--border-color"));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(color("--primary-soft"));
        table.setSelectionForeground(color("--text-main"));

        table.setDefaultRenderer(Object.class, new StripeTableCellRenderer(SwingConstants.LEFT, false));
        table.getColumnModel().getColumn(2).setCellRenderer(new StripeTableCellRenderer(SwingConstants.CENTER, false));
        table.getColumnModel().getColumn(3).setCellRenderer(new StripeTableCellRenderer(SwingConstants.RIGHT, true));

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 36));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());
    }

    private JTextField createInputField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(color("--field-bg"));
        field.setForeground(color("--text-main"));
        field.setCaretColor(color("--text-main"));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color("--border-color"), 1, true),
                BorderFactory.createEmptyBorder(9, 10, 9, 10)
        ));
        return field;
    }

    private JTextField createSearchField() {
        JTextField field = new JTextField(22);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(color("--field-bg"));
        field.setForeground(color("--text-main"));
        field.setCaretColor(color("--text-main"));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color("--border-color"), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setToolTipText("Search by brand, model, year, price, or color");
        return field;
    }

    private void chooseImageFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select motorcycle photo");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Image files (*.jpg, *.jpeg, *.png, *.gif, *.bmp)",
                "jpg", "jpeg", "png", "gif", "bmp"
        ));

        int result = fileChooser.showOpenDialog(Main.this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path selectedPath = fileChooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        imagePathField.setText(selectedPath.toString());
        showStatus("Photo selected. Click Add or Save to keep it.", "--primary");
    }

    private void refreshImagePreviewFromInput() {
        if (imagePathField == null) {
            return;
        }

        updateImagePreview(imagePathField.getText(), "No photo selected.");
    }

    private void updateImagePreview(String imagePathText, String emptyMessage) {
        if (imagePreviewLabel == null) {
            return;
        }

        String value = imagePathText == null ? "" : imagePathText.trim();
        if (value.isEmpty()) {
            setEmptyImagePreview(emptyMessage);
            return;
        }

        Path resolvedPath;
        try {
            resolvedPath = resolveImagePath(value);
        } catch (InvalidPathException ex) {
            setEmptyImagePreview("Invalid photo path.");
            return;
        }

        if (resolvedPath == null || !Files.exists(resolvedPath) || !Files.isRegularFile(resolvedPath)) {
            setEmptyImagePreview("Photo not found.");
            return;
        }

        try {
            BufferedImage image = ImageIO.read(resolvedPath.toFile());
            if (image == null) {
                setEmptyImagePreview("Unsupported image format.");
                return;
            }

            imagePreviewLabel.setIcon(buildScaledImageIcon(image, 260, 170));
            imagePreviewLabel.setText("");
            imagePreviewLabel.setToolTipText(resolvedPath.toString());
        } catch (IOException ex) {
            setEmptyImagePreview("Could not load photo.");
        }
    }

    private void setEmptyImagePreview(String message) {
        imagePreviewLabel.setIcon(null);
        imagePreviewLabel.setText(message);
        imagePreviewLabel.setToolTipText(null);
    }

    private ImageIcon buildScaledImageIcon(BufferedImage image, int maxWidth, int maxHeight) {
        double widthRatio = (double) maxWidth / image.getWidth();
        double heightRatio = (double) maxHeight / image.getHeight();
        double scale = Math.min(1.0, Math.min(widthRatio, heightRatio));

        int targetWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int targetHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));

        Image scaled = image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private Path resolveImagePath(String imagePathText) {
        Path path = Paths.get(imagePathText);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Paths.get("").toAbsolutePath().resolve(path).normalize();
    }

    private void stylePrimaryButton(JButton button, String backgroundColorKey) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(color("--on-primary"));
        button.setBackground(color(backgroundColorKey));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color(backgroundColorKey).darker(), 1, true),
                BorderFactory.createEmptyBorder(9, 14, 9, 14)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void styleSecondaryButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(color("--text-main"));
        button.setBackground(color("--neutral-soft"));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color("--border-color"), 1, true),
                BorderFactory.createEmptyBorder(9, 14, 9, 14)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color("--border-color"), 1, true),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        );
    }

    private void updateTable() {
        tableModel.setRowCount(0);

        List<Motorcycle> inventory = dealer.getInventory();
        for (Motorcycle moto : inventory) {
            tableModel.addRow(new Object[]{moto.getBrand(), moto.getModel(), moto.getYear(), moto.getPrice(), moto.getColor()});
        }

        updateInventoryInsights(inventory);
        applyTableFilter();
    }

    private void updateInventoryInsights(List<Motorcycle> inventory) {
        int total = inventory.size();
        inventoryCountValueLabel.setText(String.valueOf(total));

        if (total == 0) {
            averagePriceValueLabel.setText("$0");
            newestYearValueLabel.setText("-");
            return;
        }

        double totalPrice = 0;
        int newestYear = Integer.MIN_VALUE;

        for (Motorcycle motorcycle : inventory) {
            totalPrice += motorcycle.getPrice();
            newestYear = Math.max(newestYear, motorcycle.getYear());
        }

        double averagePrice = totalPrice / total;
        averagePriceValueLabel.setText(String.format(Locale.US, "$%,.0f", averagePrice));
        newestYearValueLabel.setText(String.valueOf(newestYear));
    }

    private void applyTableFilter() {
        if (tableSorter == null) {
            return;
        }

        String query = searchField == null ? "" : searchField.getText().trim();
        if (query.isEmpty()) {
            tableSorter.setRowFilter(null);
        } else {
            tableSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(query)));
        }

        updateTableInfo();
    }

    private void updateTableInfo() {
        if (tableInfoLabel == null || table == null || tableModel == null) {
            return;
        }

        int visibleRows = table.getRowCount();
        int totalRows = tableModel.getRowCount();

        if (visibleRows == totalRows) {
            tableInfoLabel.setText(String.format(Locale.US, "%d motorcycles", totalRows));
        } else {
            tableInfoLabel.setText(String.format(Locale.US, "%d of %d motorcycles", visibleRows, totalRows));
        }
    }

    private void onRowSelected() {
        int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow == -1) {
            clearFields();
            setEditingMode(false);
            showStatus("Fill every field and click Add.", "--text-muted");
            return;
        }

        editingRowIndex = table.convertRowIndexToModel(selectedViewRow);
        Motorcycle selected = dealer.getMotorcycleAt(editingRowIndex);

        brandField.setText(selected.getBrand());
        modelField.setText(selected.getModel());
        yearField.setText(String.valueOf(selected.getYear()));
        priceField.setText(String.format(Locale.US, "%.2f", selected.getPrice()));
        colorField.setText(selected.getColor());
        imagePathField.setText(selected.getImagePath());
        updateImagePreview(selected.getImagePath(), "No photo saved for this motorcycle.");

        setEditingMode(true);
        showStatus("Editing selected motorcycle.", "--primary");
    }

    private void setEditingMode(boolean editing) {
        if (!editing) {
            editingRowIndex = -1;
        }

        updateActionButtonState(editButton, editing, "--success");
        updateActionButtonState(removeButton, editing, "--danger");
        formTitleLabel.setText(editing ? "Edit Motorcycle" : "Add Motorcycle");
    }

    private void updateActionButtonState(JButton button, boolean enabled, String enabledColorKey) {
        button.setEnabled(enabled);

        if (enabled) {
            button.setForeground(color("--on-primary"));
            button.setBackground(color(enabledColorKey));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(color(enabledColorKey).darker(), 1, true),
                    BorderFactory.createEmptyBorder(9, 14, 9, 14)
            ));
            return;
        }

        button.setForeground(color("--disabled-text"));
        button.setBackground(color("--disabled-bg"));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color("--disabled-bg").darker(), 1, true),
                BorderFactory.createEmptyBorder(9, 14, 9, 14)
        ));
    }

    private void resetForm(boolean clearSelection) {
        clearFields();
        setEditingMode(false);

        if (clearSelection && table != null) {
            table.clearSelection();
        }

        showStatus("Fill every field and click Add.", "--text-muted");
    }

    private class AddMotorcycleListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                FormData data = readFormData();
                dealer.addMotorcycle(new Motorcycle(data.brand, data.model, data.year, data.price, data.color, data.imagePath));
                updateTable();
                resetForm(true);
                updateImagePreview(data.imagePath, "No photo saved for this motorcycle.");
                showStatus("Motorcycle added successfully.", "--success");
            } catch (IllegalArgumentException ex) {
                showValidationError(ex.getMessage());
            } catch (IOException ex) {
                showPersistenceError("Could not save the new record.", ex);
            }
        }
    }

    private class EditMotorcycleListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedModelRow = getSelectedModelRow();
            if (selectedModelRow < 0) {
                showValidationError("Select a motorcycle to edit.");
                return;
            }

            try {
                FormData data = readFormData();
                dealer.updateMotorcycle(selectedModelRow, new Motorcycle(data.brand, data.model, data.year, data.price, data.color, data.imagePath));
                updateTable();

                int viewIndex = table.convertRowIndexToView(selectedModelRow);
                if (viewIndex >= 0) {
                    table.setRowSelectionInterval(viewIndex, viewIndex);
                } else {
                    setEditingMode(false);
                }

                showStatus("Record saved successfully.", "--success");
            } catch (IllegalArgumentException ex) {
                showValidationError(ex.getMessage());
            } catch (IOException ex) {
                showPersistenceError("Could not save the edited record.", ex);
            }
        }
    }

    private class RemoveMotorcycleListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedModelRow = getSelectedModelRow();
            if (selectedModelRow < 0) {
                showValidationError("Select a motorcycle to delete.");
                return;
            }

            int confirmation = JOptionPane.showConfirmDialog(
                    Main.this,
                    "Delete the selected motorcycle from inventory?",
                    "Confirm deletion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirmation != JOptionPane.YES_OPTION) {
                return;
            }

            try {
                dealer.removeMotorcycleByIndex(selectedModelRow);
                updateTable();
                resetForm(true);
                showStatus("Record removed.", "--danger");
            } catch (IOException ex) {
                showPersistenceError("Could not save the deletion.", ex);
            }
        }
    }

    private int getSelectedModelRow() {
        int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow < 0) {
            return -1;
        }
        return table.convertRowIndexToModel(selectedViewRow);
    }

    private FormData readFormData() {
        String brand = brandField.getText().trim();
        String model = modelField.getText().trim();
        String yearText = yearField.getText().trim();
        String priceText = priceField.getText().trim().replace(',', '.');
        String color = colorField.getText().trim();
        String imagePathText = imagePathField.getText().trim();

        if (brand.isEmpty() || model.isEmpty() || yearText.isEmpty() || priceText.isEmpty() || color.isEmpty()) {
            throw new IllegalArgumentException("Fill all fields before saving.");
        }

        int year;
        try {
            year = Integer.parseInt(yearText);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid year. Use only numbers.");
        }

        int maxYear = Year.now().getValue() + 1;
        if (year < 1950 || year > maxYear) {
            throw new IllegalArgumentException("Year must be between 1950 and " + maxYear + ".");
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid price. Example: 14999.90");
        }

        if (price <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }

        String imagePath = normalizeImagePathForStorage(imagePathText);
        return new FormData(brand, model, year, price, color, imagePath);
    }

    private String normalizeImagePathForStorage(String imagePathText) {
        if (imagePathText == null || imagePathText.trim().isEmpty()) {
            return "";
        }

        try {
            return resolveImagePath(imagePathText.trim()).toString();
        } catch (InvalidPathException ex) {
            throw new IllegalArgumentException("Invalid photo path.");
        }
    }

    private void showValidationError(String message) {
        showStatus(message, "--danger");
        JOptionPane.showMessageDialog(Main.this, message, "Validation error", JOptionPane.ERROR_MESSAGE);
    }

    private void showPersistenceError(String message, IOException exception) {
        showStatus(message, "--danger");
        String detailedMessage = message
                + "\nStorage file: " + dealer.getStorageFile().toAbsolutePath()
                + "\nReason: " + exception.getMessage();
        JOptionPane.showMessageDialog(Main.this, detailedMessage, "Storage error", JOptionPane.ERROR_MESSAGE);
    }

    private void showStatus(String message, String colorKey) {
        statusLabel.setText(message);
        statusLabel.setForeground(color(colorKey));
    }

    private void clearFields() {
        brandField.setText("");
        modelField.setText("");
        yearField.setText("");
        priceField.setText("");
        colorField.setText("");
        imagePathField.setText("");
    }

    private Color color(String key) {
        Color loadedColor = themeColors.get(key);
        return loadedColor != null ? loadedColor : Color.GRAY;
    }

    private Map<String, Color> loadCssPalette() {
        Map<String, Color> palette = createDefaultPalette();
        Path cssPath = Paths.get(CSS_FILE);

        if (!Files.exists(cssPath)) {
            return palette;
        }

        try {
            String css = new String(Files.readAllBytes(cssPath), StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("--([a-zA-Z0-9-]+)\\s*:\\s*(#[0-9a-fA-F]{6})").matcher(css);
            while (matcher.find()) {
                String key = "--" + matcher.group(1);
                String value = matcher.group(2);
                palette.put(key, Color.decode(value));
            }
        } catch (IOException ignored) {
            // If CSS fails to load, keep default palette.
        }

        return palette;
    }

    private Map<String, Color> createDefaultPalette() {
        Map<String, Color> defaults = new HashMap<>();
        defaults.put("--bg-color", Color.decode("#F2F6FB"));
        defaults.put("--bg-gradient-start", Color.decode("#F8FBFF"));
        defaults.put("--bg-gradient-end", Color.decode("#EAF1FA"));

        defaults.put("--hero-bg", Color.decode("#12263D"));
        defaults.put("--hero-border", Color.decode("#2A4664"));
        defaults.put("--hero-text", Color.decode("#F6FBFF"));
        defaults.put("--hero-muted", Color.decode("#B8C8D9"));

        defaults.put("--card-bg", Color.decode("#FFFFFF"));
        defaults.put("--stat-bg", Color.decode("#F7FAFF"));
        defaults.put("--field-bg", Color.decode("#F9FCFF"));

        defaults.put("--text-main", Color.decode("#132942"));
        defaults.put("--text-muted", Color.decode("#677D95"));

        defaults.put("--border-color", Color.decode("#D4DFEC"));
        defaults.put("--header-bg", Color.decode("#EAF1FA"));
        defaults.put("--row-alt", Color.decode("#F4F8FF"));

        defaults.put("--primary", Color.decode("#0F6BCD"));
        defaults.put("--primary-soft", Color.decode("#D6E9FF"));
        defaults.put("--success", Color.decode("#1A9B61"));
        defaults.put("--danger", Color.decode("#D04444"));
        defaults.put("--neutral", Color.decode("#4A627E"));
        defaults.put("--neutral-soft", Color.decode("#E9EFF7"));
        defaults.put("--disabled-bg", Color.decode("#D7E1ED"));
        defaults.put("--disabled-text", Color.decode("#7C8FA3"));
        defaults.put("--on-primary", Color.decode("#FFFFFF"));
        return defaults;
    }

    private static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep default look and feel if system look and feel is unavailable.
        }
    }

    private static class FormData {
        private final String brand;
        private final String model;
        private final int year;
        private final double price;
        private final String color;
        private final String imagePath;

        private FormData(String brand, String model, int year, double price, String color, String imagePath) {
            this.brand = brand;
            this.model = model;
            this.year = year;
            this.price = price;
            this.color = color;
            this.imagePath = imagePath;
        }
    }

    private class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBackground(color("--header-bg"));
            label.setForeground(color("--text-main"));
            label.setFont(new Font("Segoe UI", Font.BOLD, 12));
            label.setHorizontalAlignment(SwingConstants.LEFT);
            label.setBorder(new EmptyBorder(0, 8, 0, 8));
            return label;
        }
    }

    private class StripeTableCellRenderer extends DefaultTableCellRenderer {
        private final int horizontalAlignment;
        private final boolean formatAsCurrency;

        private StripeTableCellRenderer(int horizontalAlignment, boolean formatAsCurrency) {
            this.horizontalAlignment = horizontalAlignment;
            this.formatAsCurrency = formatAsCurrency;
        }

        @Override
        protected void setValue(Object value) {
            if (formatAsCurrency && value instanceof Number) {
                setText(String.format(Locale.US, "$%,.2f", ((Number) value).doubleValue()));
                return;
            }

            super.setValue(value);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(horizontalAlignment);
            label.setBorder(new EmptyBorder(0, 8, 0, 8));

            if (isSelected) {
                label.setBackground(color("--primary-soft"));
                label.setForeground(color("--text-main"));
            } else {
                Color striping = (row % 2 == 0) ? color("--card-bg") : color("--row-alt");
                label.setBackground(striping);
                label.setForeground(color("--text-main"));
            }

            return label;
        }
    }

    private static class GradientPanel extends JPanel {
        private final Color start;
        private final Color end;

        private GradientPanel(Color start, Color end) {
            this.start = start;
            this.end = end;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setPaint(new GradientPaint(0, 0, start, 0, getHeight(), end));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            configureLookAndFeel();
            new Main().setVisible(true);
        });
    }
}
