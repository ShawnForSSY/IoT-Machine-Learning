import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.sleep;

public class VibrationDataUI {
    private JFrame frame;
    private JLabel statusLabel;
    private JLabel imageLabel;
    public ClassifyVibration cv = new ClassifyVibration();


    // Paths to the images for each state
    private final String bottleClosedPath = "/Users/shawn/Documents/UCLA/Fall_2024/209AS_Internet_of_Things/Bake-off/IoT-Machine-Learning-main/resources/BottleClosed.jpg";
    private final String bottleOpenedPath = "/Users/shawn/Documents/UCLA/Fall_2024/209AS_Internet_of_Things/Bake-off/IoT-Machine-Learning-main/resources/BottleOpened.jpg";
    private final String pouringPillsPath = "/Users/shawn/Documents/UCLA/Fall_2024/209AS_Internet_of_Things/Bake-off/IoT-Machine-Learning-main/resources/PouringPills.jpg";

    private BufferedImage bottleClosedImage;
    private BufferedImage bottleOpenedImage;
    private BufferedImage pouringPillsImage;

    private String currentState = "Neutral State";
    private final int IMAGE_WIDTH = 300;
    private final int IMAGE_HEIGHT = 400;

    public VibrationDataUI() {
        // Load and resize images
        try {
            bottleClosedImage = loadAndResizeImage(bottleClosedPath);
            bottleOpenedImage = loadAndResizeImage(bottleOpenedPath);
            pouringPillsImage = loadAndResizeImage(pouringPillsPath);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error loading images. Please check the file paths.", "Image Loading Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Create the frame
        frame = new JFrame("Real-time Vibration Data Recognition");
        frame.setSize(400, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Create the status label
        statusLabel = new JLabel("Current State: " + currentState, SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        frame.add(statusLabel, BorderLayout.NORTH);

        // Create the image label to display the images
        imageLabel = new JLabel(new ImageIcon(bottleClosedImage), SwingConstants.CENTER);
        frame.add(imageLabel, BorderLayout.CENTER);

        // Make the frame visible
        frame.setVisible(true);

        // Start the timer to simulate real-time data updates
        startDataUpdateTimer();
    }

    // Load and resize image to fit the frame
    private BufferedImage loadAndResizeImage(String path) throws IOException {
        BufferedImage originalImage = ImageIO.read(new File(path));
        Image scaledImage = originalImage.getScaledInstance(IMAGE_WIDTH, IMAGE_HEIGHT, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();
        return resizedImage;
    }

    // Method to start receiving real-time data from the sensor
    private void startDataUpdateTimer() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Update the state based on the sensor data (replace with your real-time data logic)
                currentState = getRealTimeSensorData();
                // Update the UI to reflect the new state
                updateUI(currentState);
                if (currentState.equals("Bottle Opened")){
                    try{
                        Thread.sleep(3000);
                    }catch (InterruptedException e) {
                        e.printStackTrace(); // Handle the exception if the sleep is interrupted
                    }
                }
            }
        }, 0, 1000); // Update every second (adjust interval as needed)
    }

    // Placeholder method to integrate with real-time sensor data
    private String getRealTimeSensorData() {
        // Replace this with the actual logic to read data from your vibration sensor
        // For example, using your classifier object from ClassifyVibration

        // Simulate the sensor reading for demonstration purposes
//        double random = Math.random();
//        if (random < 0.33) {
//            return "Bottle Opened";
//        } else if (random < 0.66) {
//            return "Pouring Pills";
//        } else {
//            return "Bottle Closed";
//        }

         String label = cv.get_current_label();
         if (label.equals("open")) {

             return "Bottle Opened";
         } else if (label.equals("pour")) {
             return "Pouring Pills";
         } else {
             return "Bottle Closed";
         }
    }

    // Method to update the UI with the new state
    private void updateUI(String state) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Current State: " + state);
            switch (state) {
                case "Bottle Opened":
                    imageLabel.setIcon(new ImageIcon(bottleOpenedImage));
                    break;
                case "Pouring Pills":
                    imageLabel.setIcon(new ImageIcon(pouringPillsImage));
                    break;
                case "Bottle Closed":
                    imageLabel.setIcon(new ImageIcon(bottleClosedImage));
                    break;
                default:
                    statusLabel.setText("Current State: Neutral State");
                    break;
            }
            System.out.println("State updated to: " + state); // For debugging purposes
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VibrationDataUI::new);
    }
}

