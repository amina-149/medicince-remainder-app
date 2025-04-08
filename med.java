package company.com;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Timer;
import java.util.TimerTask;

public class MedicineReminderApp extends JFrame {

    private final JTable reminderTable;
    private final DefaultTableModel tableModel;
    private final String REMINDER_FOLDER = "reminders";
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
    private Timer alarmTimer;

    public MedicineReminderApp() {
        super("Medicine Reminder App");
        
        // Initialize table model
        tableModel = new DefaultTableModel(new String[]{"Medicine Name", "Reminder Time"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reminderTable = new JTable(tableModel);
        
        initializeUI();
        setupReminderFolder();
        loadRemindersFromFiles();
        startAlarmChecker();
    }

    private void initializeUI() {
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        add(mainPanel);

        // Title
        JLabel titleLabel = new JLabel("Medicine Reminder", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(0, 102, 204));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Table
        customizeTable();
        JScrollPane scrollPane = new JScrollPane(reminderTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        
        // Add Reminder Button (Green)
        JButton addButton = new JButton("Add Reminder");
        styleButton(addButton, new Color(46, 204, 113), Color.BLACK);
        addButton.addActionListener(e -> showAddReminderDialog());

        // Delete Selected Button (Red)
        JButton deleteButton = new JButton("Delete Selected");
        styleButton(deleteButton, new Color(231, 76, 60), Color.BLACK);
        deleteButton.addActionListener(e -> deleteSelectedReminder());

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void styleButton(JButton button, Color bgColor, Color textColor) {
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(200, 50));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(10, 25, 10, 25)
        ));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
    }

    private void customizeTable() {
        reminderTable.setRowHeight(30);
        reminderTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        reminderTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 15));
        reminderTable.getTableHeader().setBackground(new Color(70, 130, 180));
        reminderTable.getTableHeader().setForeground(Color.WHITE);
        reminderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reminderTable.setGridColor(new Color(220, 220, 220));
        reminderTable.setShowGrid(true);
    }

    private void setupReminderFolder() {
        File folder = new File(REMINDER_FOLDER);
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    private void showAddReminderDialog() {
        JDialog dialog = new JDialog(this, "Add New Reminder", true);
        dialog.setSize(400, 200);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(this);

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel nameLabel = new JLabel("Medicine Name:");
        JTextField nameField = new JTextField();

        JLabel timeLabel = new JLabel("Time (hh:mm AM/PM):");
        JTextField timeField = new JTextField();

        inputPanel.add(nameLabel);
        inputPanel.add(nameField);
        inputPanel.add(timeLabel);
        inputPanel.add(timeField);

        dialog.add(inputPanel, BorderLayout.CENTER);

        JButton addButton = new JButton("Add Reminder");
        styleButton(addButton, new Color(46, 204, 113), Color.BLACK);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        


        addButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String time = timeField.getText().trim();

            if (name.isEmpty() || time.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Both fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                LocalTime.parse(time, timeFormatter);
                
                if (medicineExists(name)) {
                    JOptionPane.showMessageDialog(dialog, "This medicine already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                tableModel.addRow(new Object[]{name, time});
                saveReminderToFile(name, time);
                dialog.dispose();
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid time format! Use hh:mm AM/PM", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private boolean medicineExists(String name) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0).equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void deleteSelectedReminder() {
        int selectedRow = reminderTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a reminder to delete", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String medicineName = (String) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete reminder for " + medicineName + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (deleteReminderFile(medicineName)) {
                tableModel.removeRow(selectedRow);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete reminder file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveReminderToFile(String name, String time) {
        try {
            File file = new File(REMINDER_FOLDER + File.separator + sanitizeFilename(name) + ".txt");
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("Medicine: " + name);
                writer.println("Time: " + time);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving reminder: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean deleteReminderFile(String name) {
        File file = new File(REMINDER_FOLDER + File.separator + sanitizeFilename(name) + ".txt");
        return file.exists() && file.delete();
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_.]", "_");
    }

    private void loadRemindersFromFiles() {
        File folder = new File(REMINDER_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));
        
        if (files != null) {
            for (File file : files) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String nameLine = reader.readLine();
                    String timeLine = reader.readLine();
                    
                    if (nameLine != null && timeLine != null) {
                        String name = nameLine.split(": ")[1];
                        String time = timeLine.split(": ")[1];
                        tableModel.addRow(new Object[]{name, time});
                    }
                } catch (Exception e) {
                    System.err.println("Error loading file " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private void startAlarmChecker() {
        alarmTimer = new Timer("MedicineReminderTimer", true);
        alarmTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!SwingUtilities.isEventDispatchThread()) {
                    SwingUtilities.invokeLater(this::run);
                    return;
                }
                checkRemindersAndAlert();
            }
        }, 0, 60000); // Check every minute
    }

    private void checkRemindersAndAlert() {
        String currentTime = LocalTime.now().format(timeFormatter);
        
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String time = (String) tableModel.getValueAt(i, 1);
            if (time.equalsIgnoreCase(currentTime)) {
                String medicineName = (String) tableModel.getValueAt(i, 0);
                showReminderAlert(medicineName);
            }
        }
    }

    private void showReminderAlert(String medicineName) {
        Toolkit.getDefaultToolkit().beep();
        
        JOptionPane.showMessageDialog(
            this,
            "<html><div style='text-align: center;'>" +
            "<h2 style='color: #4682B4;'>⏰ Reminder</h2>" +
            "<p style='font-size: 16px;'>Time to take:<br><b style='color: #2E8B57;'>" + medicineName + "</b></p>" +
            "</div></html>",
            "Medicine Reminder",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    @Override
    public void dispose() {
        if (alarmTimer != null) {
            alarmTimer.cancel();
        }
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MedicineReminderApp app = new MedicineReminderApp();
            app.setVisible(true);
        });
    }
}