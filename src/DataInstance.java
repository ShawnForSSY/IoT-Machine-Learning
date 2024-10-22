import java.io.*;
import java.util.*;

/* A recorded instance of the training data */

public class DataInstance implements Serializable {

	private static final long serialVersionUID = -1L;
	public String label;
	public Date date;
	public float[] measurements;
	
	public String toCSVRow() {
		StringBuilder sb = new StringBuilder();
		sb.append(',');
		for (int i = 0; i < measurements.length; i++) {
			sb.append(measurements[i]);
			sb.append(',');
		}
		sb.append(label);
		return sb.toString();
	}

	public void saveToCSV(String folderPath, String folder_name) {
        // Define the file path (create folder if it doesn't exist)
        File directory = new File(folderPath);
        if (!directory.exists()) {
            directory.mkdirs(); // Creates the directory if it does not exist
        }

        File file = new File(directory, folder_name);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(this.toCSVRow());
            writer.newLine(); // Add a new line after each CSV row
        } catch (IOException e) {
            e.printStackTrace();
        }
	}


}