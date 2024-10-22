import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

/* A wrapper class to use Weka's classifiers */

public class MLClassifier implements Serializable {
    private static final long serialVersionUID = 1L;  // 添加序列化版本号
    
    transient FeatureCalc featureCalc = null;
    transient SMO classifier = null;  // SMO 不能直接序列化，所以标记为 transient
    Attribute classattr;
    Filter filter = new Normalize();

    public MLClassifier() {
    }

    public void train(Map<String, List<DataInstance>> instances) {
        
        /* generate instances using the collected map of DataInstances */
        
        /* pass on labels */
        featureCalc = new FeatureCalc(new ArrayList<>(instances.keySet()));
        
        /* pass on data */
        List<DataInstance> trainingData = new ArrayList<>();
         
        for(List<DataInstance> v : instances.values()) {
            trainingData.addAll(v);
        }
         
        /* prepare the training dataset */
        Instances dataset = featureCalc.calcFeatures(trainingData);
         
        /* call build classifier */
        classifier = new SMO();
         
         try {
             
             // Yang: RBFKernel requires tuning but might perform better than PolyKernel
             
             /* 
            classifier.setOptions(weka.core.Utils.splitOptions("-C 1.0 -L 0.0010 "
                     + "-P 1.0E-12 -N 0 -V -1 -W 1 "
                     + "-K \"weka.classifiers.functions.supportVector.RBFKernel "
                     + "-C 0 -G 0.7\""));
                     */
            
            classifier.setOptions(weka.core.Utils.splitOptions("-C 1.0 -L 0.0010 "
                     + "-P 1.0E-12 -N 0 -V -1 -W 1 "
                     + "-K \"weka.classifiers.functions.supportVector.PolyKernel "
                     + "-C 0 -E 1.0\""));
            
            classifier.buildClassifier(dataset);
            this.classattr = dataset.classAttribute();
            
            System.out.println("Training done!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String classify(DataInstance data) {
        if(classifier == null || classattr == null) {
            return "Unknown";
        }
        
		// 确保在分类前重新初始化 featureCalc
        if (featureCalc == null) {
			List<String> classLabels = new ArrayList<>();
            classLabels.add("quiet");
			classLabels.add("open");
			classLabels.add("close");
			classLabels.add("pour");

			featureCalc = new FeatureCalc(classLabels);  // 使用已有的分类标签重新初始化
        }

        Instance instance = featureCalc.calcFeatures(data);
        
        try {
            int result = (int) classifier.classifyInstance(instance);
            return classattr.value((int)result);
        } catch(Exception e) {
            e.printStackTrace();
            return "Error";
        }
    }
    
    /* 自定义序列化方法，保存 Weka 模型到文件 */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();  // 序列化普通对象
        
        // 使用 Weka 自带的方法将模型保存到文件
        if (classifier != null) {
            try {
                out.writeObject("weka_model.model");  // 写入模型文件路径
                weka.core.SerializationHelper.write("weka_model.model", classifier);  // 保存模型到文件
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* 自定义反序列化方法，从文件中加载 Weka 模型 */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();  // 反序列化普通对象

        // 从文件中加载 Weka 模型
        try {
            String modelFilePath = (String) in.readObject();
            classifier = (SMO) weka.core.SerializationHelper.read(modelFilePath);  // 从文件加载模型
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (featureCalc == null) {
			List<String> classLabels = new ArrayList<>();
            classLabels.add("quiet");
			classLabels.add("open");
			classLabels.add("close");
			classLabels.add("pour");

			featureCalc = new FeatureCalc(classLabels);  // 使用已有的分类标签重新初始化
        }
    }
}






// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;

// import weka.classifiers.functions.SMO;
// import weka.core.Attribute;
// import weka.core.Instance;
// import weka.core.Instances;
// import weka.filters.Filter;
// import weka.filters.unsupervised.attribute.Normalize;

// /* A wrapper class to use Weka's classifiers */

// public class MLClassifier {
// 	FeatureCalc featureCalc = null;
//     SMO classifier = null;
//     Attribute classattr;
//     Filter filter = new Normalize();

//     public MLClassifier() {
    	
//     }

//     public void train(Map<String, List<DataInstance>> instances) {
    	
//     	/* generate instances using the collected map of DataInstances */
    	
//     	/* pass on labels */
//     	featureCalc = new FeatureCalc(new ArrayList<>(instances.keySet()));
    	
//     	/* pass on data */
//     	List<DataInstance> trainingData = new ArrayList<>();
    	 
//     	for(List<DataInstance> v : instances.values()) {
//     		trainingData.addAll(v);
//     	}
         
//     	/* prepare the training dataset */
//     	Instances dataset = featureCalc.calcFeatures(trainingData);
         
//     	/* call build classifier */
//     	classifier = new SMO();
         
//          try {
        	 
//         	 // Yang: RBFKernel requires tuning but might perform better than PolyKernel
        	 
//         	 /* 
// 			classifier.setOptions(weka.core.Utils.splitOptions("-C 1.0 -L 0.0010 "
// 			         + "-P 1.0E-12 -N 0 -V -1 -W 1 "
// 			         + "-K \"weka.classifiers.functions.supportVector.RBFKernel "
// 			         + "-C 0 -G 0.7\""));
// 			         */
			         
        	
//         	classifier.setOptions(weka.core.Utils.splitOptions("-C 1.0 -L 0.0010 "
// 			         + "-P 1.0E-12 -N 0 -V -1 -W 1 "
// 			         + "-K \"weka.classifiers.functions.supportVector.PolyKernel "
// 			         + "-C 0 -E 1.0\""));
			
// 			classifier.buildClassifier(dataset);
// 			this.classattr = dataset.classAttribute();
			
// 			System.out.println("Training done!");
			
// 		} catch (Exception e) {
// 			// TODO Auto-generated catch block
// 			e.printStackTrace();
// 		}
//     }

//     public String classify(DataInstance data) {
//         if(classifier == null || classattr == null) {
//             return "Unknown";
//         }
        
//         Instance instance = featureCalc.calcFeatures(data);
        
//         try {
//             int result = (int) classifier.classifyInstance(instance);
//             return classattr.value((int)result);
//         } catch(Exception e) {
//             e.printStackTrace();
//             return "Error";
//         }
//     }
    
// }