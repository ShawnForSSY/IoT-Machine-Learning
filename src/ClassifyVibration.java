import java.io.*;
import java.util.*;

import processing.core.PApplet;
import processing.sound.AudioIn;
import processing.sound.FFT;
import processing.sound.Sound;
import processing.sound.Waveform;

/* A class with the main function and Processing visualizations to run the demo */

public class ClassifyVibration extends PApplet {
	int model_no=0;
	int collect_sign_open=0;
	int drawtest=1;

	FFT fft;
	AudioIn in;
	Waveform waveform;
	int bands = 512; // number of frequency bands in the FFT analysis
	int nsamples = 1024; // the number of samples taken from the audio signal at a time for FFT analysis
	float[] spectrum = new float[bands];
	float[] fftFeatures = new float[bands];
	String[] classNames = {"quiet", "open", "close", "pour"};
	int classIndex = 0;
	int dataCount = 0;

	MLClassifier classifier;
	
	Map<String, List<DataInstance>> trainingData = new HashMap<>();
	{for (String className : classNames){
		trainingData.put(className, new ArrayList<DataInstance>());
	}}
	
	Map<String, List<DataInstance>> testData = new HashMap<>();
	{for (String className : classNames){
		testData.put(className, new ArrayList<DataInstance>());
	}}

	DataInstance captureInstance (String label){
		DataInstance res = new DataInstance();
		res.label = label;
		res.measurements = fftFeatures.clone();
		return res;
	}

	public void writeClassificationToCSV(String guessedLabel) {
		String folderPath = "output";  // Define the folder path
		String fileName = "classification_results.csv";  // Define the CSV file name
		
		File directory = new File(folderPath);
		if (!directory.exists()) {
			directory.mkdirs();
		}

		File file = new File(directory, fileName);
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
			// Write the classification label and timestamp
			if (guessedLabel != "quiet") {
				writer.write(guessedLabel + "," + System.currentTimeMillis());
				writer.newLine();  // Add a new line after each record
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

   	public void loadFromCSV(String csvFilePath, Map<String, List<DataInstance>> trainingData) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) {
					continue;
				}
                String[] values = line.split(",");  
                String label = values[values.length - 1];
                float[] fftFeatures = new float[values.length - 2];
                for (int i = 1; i < values.length - 2; i++) {
					String value = values[i].trim();
                    if (value.isEmpty()) {
                        continue;
                    } else {
                        fftFeatures[i-1] = Float.parseFloat(value);  // 转换为浮点数
                    }
                }
                DataInstance instance = new DataInstance();
                instance.label = label;
                instance.measurements = fftFeatures;
                if (trainingData.containsKey(label)) {
                    trainingData.get(label).add(instance);
                } else {
                    List<DataInstance> instances = new ArrayList<>();
                    instances.add(instance);
                    trainingData.put(label, instances);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public void renderDataInstanceFeatures(float[] savedFFTFeatures) {
		// Render saved FFT features
		for (int i = 0; i < bands; i++) {
			line(i, height, i, height - savedFFTFeatures[i] * height * 40); // 使用保存的FFT特征
    	}
	}

	public static void main(String[] args) { // entry point for any Java program.
		PApplet.main("ClassifyVibration"); // Processing library(where PApplet comes from) will look for the settings, setup, and draw methods in this class and start executing them.
	}
	
	public void settings() {
		size(512, 400);
	}

	public void setup() {
		
		/* list all audio devices */
		Sound.list();
		Sound s = new Sound(this);
		  
		/* select microphone device */
		s.inputDevice(4);
		
		// FFT (for frequency analysis) and Waveform (for time-domain analysis)
		/* create an Input stream which is routed into the FFT analyzer */
		fft = new FFT(this, bands); // FFT will break down the incoming audio signal into 512 frequency components.
		in = new AudioIn(this, 0);
		waveform = new Waveform(this, nsamples);
		waveform.input(in);
		
		/* start the Audio Input */
		in.start();
		  
		/* patch the AudioIn */
		fft.input(in);
	}









	public void draw() {
		background(0);
		fill(0);
		stroke(255);
		
		waveform.analyze();

		beginShape();
		  
		for(int i = 0; i < nsamples; i++)
		{
			vertex(
					map(i, 0, nsamples, 0, width),
					map(waveform.data[i], -1, 1, 0, height)
					);
		}
		
		endShape();

		fft.analyze(spectrum);

		for(int i = 0; i < bands; i++){
			/* the result of the FFT is normalized */
			/* draw the line for frequency band i scaling it up by 40 to get more amplitude */
			line( i, height, i, height - spectrum[i]*height*40);
			fftFeatures[i] = spectrum[i];
		} 

		fill(255);
		textSize(30);
		if(classifier != null) {
			DataInstance current_test_data = captureInstance(null);
			String guessedLabel = classifier.classify(current_test_data);
	
			
			// Yang: add code to stabilize your classification results
			
			text("classified as: " + guessedLabel, 20, 30);
			
			if (collect_sign_open==1) {
				if (!guessedLabel.equals("quiet")){  // guessedLabel != "quiet"
					float[] testFFT = current_test_data.measurements;
					if (drawtest==1) {
						noLoop(); 
						// background(0);  // 清除屏幕，防止残留的 waveform 被显示
						// stroke(255);
						renderDataInstanceFeatures(testFFT); 
						String imageFolderPath = "data_image/open"; 
						File imageDirectory = new File(imageFolderPath);
						if (!imageDirectory.exists()) {
							imageDirectory.mkdirs();  
						}
						String imageFileName = imageFolderPath + "/"  + System.currentTimeMillis() + ".png";
						save(imageFileName);
						println("Open Image saved: " + imageFileName);
						loop();
						// collect_sign_open=0;
					}
					else {
						DataInstance new_data = new DataInstance();
						new_data.label = "open";
						new_data.measurements = testFFT;
						String folderPath = "data/open"; 
						String fileName = System.currentTimeMillis() + ".png"; 
						new_data.saveToCSV(folderPath, fileName);
						// collect_sign_open=0;
					}
					
            	}

			}

			// if (!guessedLabel.equals("quiet")){  // guessedLabel != "quiet"
			// 	float[] testFFT = current_test_data.measurements; // 提取保存的FFT特征
			// 	noLoop(); // 暂时停止 draw 刷新，防止冲突
			// 	background(0);  // 清除屏幕，防止残留的 waveform 被显示
    		// 	stroke(255);
			// 	renderDataInstanceFeatures(testFFT); 
			// 	String imageFolderPath = "output/test_images"; 
			// 	File imageDirectory = new File(imageFolderPath);
			// 	if (!imageDirectory.exists()) {
			// 		imageDirectory.mkdirs();  
			// 	}
			// 	String imageFileName = imageFolderPath + "/"  + System.currentTimeMillis() + ".png";
			// 	save(imageFileName);
			// 	println("Test Image saved: " + imageFileName);
			// 	loop();
            // }
		
		
		}else {
			text(classNames[classIndex], 20, 30);
			dataCount = trainingData.get(classNames[classIndex]).size();
			text("Data collected: " + dataCount, 20, 60);
		}
	}














	public void keyPressed() {
		

		if (key == CODED && keyCode == DOWN) {
			classIndex = (classIndex + 1) % classNames.length;
		}
		
		else if (key == 't') {
			if(classifier == null) {
				println("Start training ...");
				classifier = new MLClassifier();
				classifier.train(trainingData);
			}else {
				classifier = null;
			}
		}
		
		else if (key == 's') {
			if (classifier != null) {
				try {
					String modelName = "model" + "/"  + "model_" + model_no + ".ser";
					File modelFile = new File(modelName);
					File parentDir = modelFile.getParentFile();  
					if (!parentDir.exists()) {
						parentDir.mkdirs();  
					}
					FileOutputStream fileOut = new FileOutputStream(modelName);
					ObjectOutputStream out = new ObjectOutputStream(fileOut);
					out.writeObject(classifier);  
					out.close();
					fileOut.close();
					println("Model saved");
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				println("No trained model to save.");
			}
		}

		else if (key == 'l') {
			try {
				String modelName = "model" + "/"  + "model_" + model_no + ".ser";
				FileInputStream fileIn = new FileInputStream(modelName);
				ObjectInputStream in = new ObjectInputStream(fileIn);
				classifier = (MLClassifier) in.readObject();  
				in.close();
				fileIn.close();
				println("Model loaded");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}


		else if (key == 'o') {
			//trainingData把数据load进来
			loadFromCSV("./output/data.csv", trainingData);
		}


		else if (key == 'e') {
			String modelName = "model" + "/"  + "model_" + model_no + ".ser";  
			try {
				// load
				FileInputStream fileIn = new FileInputStream(modelName);
				ObjectInputStream inn = new ObjectInputStream(fileIn);
				MLClassifier loadedClassifier = (MLClassifier) inn.readObject();  
				inn.close();
				fileIn.close();
				println("Model loaded from " + modelName);

				loadFromCSV("./output/data.csv", testData);

				int correctCount = 0;
				int totalCount = testData.size();
				StringBuilder resultLog = new StringBuilder();  // 用于记录测试结果

				for (List<DataInstance> instanceList : testData.values()) {
					for (DataInstance testInstance : instanceList) {
						String trueLabel = testInstance.label;  // 实际标签
						String predictedLabel = loadedClassifier.classify(testInstance);  // 模型预测的标签

						// 记录结果
						resultLog.append("True: ").append(trueLabel).append(", Predicted: ").append(predictedLabel).append("\n");

						if (trueLabel.equals(predictedLabel)) {
							correctCount++;
						}
					}
				}


				// 3. 计算模型得分
				double accuracy = (double) correctCount / totalCount;
				println("Model accuracy: " + accuracy);

				// 4. 保存测试结果和得分
				String resultFileName = "model" + "/" + "test_results_model_" + model_no + ".txt";
				File resultFile = new File(resultFileName);

				// 将结果和得分写入文件
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile))) {
					writer.write("Model Test Results\n");
					writer.write("Accuracy: " + accuracy + "\n");
					writer.write(resultLog.toString());  
				}

				println("Test results saved to " + resultFileName);

			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		else if (key == 'k') {
			// collect open
			collect_sign_open = 1;
		}



		else {
			//收集20个quiet 20个其他->train
			DataInstance current_data = captureInstance(classNames[classIndex]);
			trainingData.get(classNames[classIndex]).add(current_data);			





			// float[] capturedFFT = current_data.measurements; 
			// noLoop(); 
			// renderDataInstanceFeatures(capturedFFT); 
			// String imageFolderPath = "output/train_images"; 
			// File imageDirectory = new File(imageFolderPath);
			// if (!imageDirectory.exists()) {
			// 	imageDirectory.mkdirs();  
			// }
			// String imageFileName = imageFolderPath + "/" + current_data.label + System.currentTimeMillis() + ".png";
			// save(imageFileName);
			// println("Image saved: " + imageFileName);
			// loop();




			// String folderPath = "output"; // Folder path where CSV will be saved
			// String fileName = "data.csv";  // CSV file name
			// current_data.saveToCSV(folderPath, fileName);
		}

		
	}

}
