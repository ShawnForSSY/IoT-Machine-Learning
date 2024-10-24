import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import processing.core.PApplet;
import processing.sound.AudioIn;
import processing.sound.FFT;
import processing.sound.Sound;
import processing.sound.Waveform;

/* A class with the main function and Processing visualizations to run the demo */

public class ClassifyVibration extends PApplet {
	int model_no=0;
	int collect_sign=0;
	int drawtest=1;
	int start_recording=0;
	// need edit
	int classifier4test=1;
	String now_collect_class="grab";
	String testsetPath = "./data/test.csv";
	// threshold
	// collect data, path

	FFT fft;
	AudioIn in;
	Waveform waveform;
	int bands = 512; // number of frequency bands in the FFT analysis
	int nsamples = 1024; // the number of samples taken from the audio signal at a time for FFT analysis
	float[] spectrum = new float[bands];
	float[] fftFeatures = new float[bands];
	String[] classNames = {"neutral", "pour", "grab"};
	int classIndex = 0;
	int dataCount = 0;
	Map<String, Integer> classCountMap = new HashMap<>();

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
			line(i, height, i, height - savedFFTFeatures[i] * height * 40); 
    	}
	}

	public static void main(String[] args) { // entry point for any Java program.
		PApplet.main("ClassifyVibration"); // Processing library(where PApplet comes from) will look for the settings, setup, and draw methods in this class and start executing them.
	}
	
	public void settings() {
		size(512, 400);
		// size(1600, 800);  
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












// int bufferSize = 100;  // 存储历史帧的数量，可以根据需要调整
// List<float[]> waveformBuffer = new ArrayList<>();
// List<float[]> fftBuffer = new ArrayList<>();

// // 绘制波形缓冲区的历史数据
// void drawWaveformBuffer() {
//     stroke(0, 255, 0);
//     for (int t = 0; t < waveformBuffer.size(); t++) {
//         float[] waveformData = waveformBuffer.get(t);
//         beginShape();
//         for (int i = 0; i < nsamples; i++) {
//             // 计算每一帧的 x 坐标，根据缓冲区索引 t 调整偏移
//             float x = map(i, 0, nsamples, 0, width) + (t * width / bufferSize);  // 每个 t 帧向右偏移
//             float y = map(waveformData[i], -1, 1, 0, height);
//             if (x < width) {  // 确保只在屏幕内绘制
//                 vertex(x, y);
//             }
//         }
//         endShape();
//     }
// }

// // 绘制 FFT 特征缓冲区的历史数据
// void drawFFTBuffer() {
//     stroke(255, 0, 0);
//     for (int t = 0; t < fftBuffer.size(); t++) {
//         float[] fftData = fftBuffer.get(t);
//         for (int i = 0; i < bands; i++) {
//             // 计算每一帧的 x 坐标，根据缓冲区索引 t 调整偏移
//             float x = map(i, 0, bands, 0, width) + (t * width / bufferSize);  // 每个 t 帧向右偏移
//             float y = height - fftData[i] * height * 40;
//             if (x < width) {  // 确保只在屏幕内绘制
//                 line(x, height, x, y);
//             }
//         }
//     }
// }


	float getMaxValue(float[] data) {
		float maxValue = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < data.length; i++) {
			if (data[i] > maxValue) {
				maxValue = data[i];
			}
		}
		return maxValue;
	}

	public double testModelOnValidationSet(MLClassifier classifier, Map<String, List<DataInstance>> testData) {
		int correctCount = 0;
		int totalCount = 0;

		for (List<DataInstance> instanceList : testData.values()) {
			totalCount += instanceList.size(); 
			for (DataInstance testInstance : instanceList) {
				String trueLabel = testInstance.label;  // 实际标签
				String predictedLabel = classifier.classify(testInstance);  // 模型预测的标签

				if (trueLabel.equals(predictedLabel)) {
					correctCount++;
				}
			}
		}

		return (double) correctCount / totalCount;
	}










	public void draw() {
		background(0);
		fill(0);
		stroke(255);
		
		// // 采集当前帧的波形数据
		// waveform.analyze();
		// float[] currentWaveform = new float[nsamples];
		// for (int i = 0; i < nsamples; i++) {
		// 	currentWaveform[i] = waveform.data[i];
		// }

		// // 采集当前帧的 FFT 数据
		// fft.analyze(spectrum);
		// float[] currentFFT = new float[bands];
		// for (int i = 0; i < bands; i++) {
		// 	currentFFT[i] = spectrum[i];
		// }

		// // 将当前帧的数据存入缓冲区
		// waveformBuffer.add(currentWaveform);
		// fftBuffer.add(currentFFT);

		// // 保证缓冲区大小不超过 bufferSize
		// if (waveformBuffer.size() > bufferSize) {
		// 	waveformBuffer.remove(0);  // 移除最早的帧
		// }
		// if (fftBuffer.size() > bufferSize) {
		// 	fftBuffer.remove(0);  // 移除最早的帧
		// }

		// // 绘制历史帧的波形
		// drawWaveformBuffer();

		// // 绘制历史帧的 FFT 特征
		// drawFFTBuffer();
		
		// fill(255);
		// textSize(30);
		//-----------------------
		
		
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
			// text("classified as: " + guessedLabel, 20, 30);
			
			//---------------------test model and get classification result-------------------------------------------------------------------
			if ((classifier4test == 1) && (start_recording==1)) {
				classCountMap.put(guessedLabel, classCountMap.getOrDefault(guessedLabel, 0) + 1);
			}
			if ((classifier4test == 1) && (start_recording==0)) { 
				for (Map.Entry<String, Integer> entry : classCountMap.entrySet()) {
					println("Class: " + entry.getKey() + ", Count: " + entry.getValue());
				}

				Map<String, Integer> sortedByCount = classCountMap.entrySet()
						.stream()
						.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
						.limit(2)  // top 2
						.collect(Collectors.toMap(
								Map.Entry::getKey,
								Map.Entry::getValue,
								(e1, e2) -> e1,  
								HashMap::new));

				if (!sortedByCount.isEmpty()) {
					println("Top two classes: ");
					for (Map.Entry<String, Integer> entry : sortedByCount.entrySet()) {
						println("Class: " + entry.getKey() + ", Count: " + entry.getValue());
					}
					println("----------------------------------------------------");

				}
				classCountMap.clear();
			}


			//---------------------collect data-------------------------------------------------------------------
			if ((classifier4test == 0) && (collect_sign==1)) {
				if (!guessedLabel.equals("quiet")){ 
					float[] testFFT = current_test_data.measurements;
					if (drawtest==1) {
						float qualityThreshold = 0.005f; 
						float maxFFTValue = getMaxValue(testFFT);
						if (maxFFTValue < qualityThreshold) {
							println("Data discarded due to low quality.");
						} else {
							noLoop(); 
							// background(0);  
							// stroke(255);
							renderDataInstanceFeatures(testFFT); 
							String imageFolderPath = "data_image/" + now_collect_class; 
							File imageDirectory = new File(imageFolderPath);
							if (!imageDirectory.exists()) {
								imageDirectory.mkdirs();  
							}
							String imageFileName = imageFolderPath + "/"  + System.currentTimeMillis() + ".png";
							save(imageFileName);
							// println("Open Image saved: " + imageFileName);
							loop();

							// println("Data quality is acceptable. Processing data...");
							DataInstance new_data = new DataInstance();
							new_data.label = now_collect_class;
							new_data.measurements = testFFT;
							String folderPath = "data/" + now_collect_class; 
							String fileName = now_collect_class + ".csv"; 
							new_data.saveToCSV(folderPath, fileName);
						}
					}
					else {
						DataInstance new_data = new DataInstance();
						new_data.label = now_collect_class;
						new_data.measurements = testFFT;
						String folderPath = "data/" + now_collect_class; 
						String fileName = now_collect_class + ".csv"; 
						new_data.saveToCSV(folderPath, fileName);
					}					
            	}

				if (guessedLabel.equals("neutral")){  
					float[] testFFT = current_test_data.measurements;
					if (drawtest==1) {
						noLoop(); 
						// background(0);  // 清除屏幕，防止残留的 waveform 被显示
						// stroke(255);
						renderDataInstanceFeatures(testFFT); 
						String imageFolderPath = "data_image/neutral"; 
						File imageDirectory = new File(imageFolderPath);
						if (!imageDirectory.exists()) {
							imageDirectory.mkdirs();  
						}
						String imageFileName = imageFolderPath + "/"  + System.currentTimeMillis() + ".png";
						save(imageFileName);
						// println("Open Image saved: " + imageFileName);
						loop();

						DataInstance new_data = new DataInstance();
						new_data.label = "neutral";
						new_data.measurements = testFFT;
						String folderPath = "data/neutral"; 
						String fileName = "neutral" + ".csv"; 
						new_data.saveToCSV(folderPath, fileName);						
					}					
            	}


			}		
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
		
		// else if (key == 't') {
		// 	if(classifier == null) {
		// 		println("Start training ...");
		// 		classifier = new MLClassifier();
		// 		classifier.train(trainingData, 1, 0.7);
		// 	}else {
		// 		classifier = null;
		// 	}
		// }

		else if (key == 't') {
			if (classifier == null) {
				println("Start training with parameter tuning...");

				double[] C_values = {0.1, 1.0, 10.0,20.0, 30.0,40.0,50.0};
				double[] gamma_values = {0.01, 0.1, 1.0};

				double bestAccuracy = 0;
				String bestParams = "";
				double bestc=0;
				double bestgamma=0;
				classifier = new MLClassifier();

				for (double C : C_values) {
					for (double gamma : gamma_values) {
						println("Training with C=" + C + ", gamma=" + gamma);
						
						classifier.train(trainingData, C, gamma);
						loadFromCSV(testsetPath, testData);
						double accuracy = testModelOnValidationSet(classifier, testData);
						println("Accuracy with C=" + C + ", gamma=" + gamma + ": " + accuracy);
						if (accuracy > bestAccuracy) {
							bestAccuracy = accuracy;
							bestParams = "C=" + C + ", gamma=" + gamma;
							bestc = C;
							bestgamma=gamma;
						}
					}
				}
				println("Best parameters: " + bestParams);
				println("Best accuracy: " + bestAccuracy);
				
				classifier.train(trainingData, bestc, bestgamma);

			} else {
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
			//load trainingData
			// loadFromCSV("./data_old/data.csv", trainingData);
			loadFromCSV("./data/train.csv", trainingData);
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

				// loadFromCSV("./output/data.csv", testData);

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
			// collect open data
			if (collect_sign == 0) {
				collect_sign = 1;
			}
			else if (collect_sign == 1) {
				collect_sign = 0;
			}
		}

		else if (key == ' ') {
			// for test and get classification result
			if (start_recording == 0) {
				start_recording = 1;
			}
			else if (start_recording == 1) {
				start_recording = 0;
			}
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
