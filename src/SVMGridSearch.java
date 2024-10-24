import weka.classifiers.functions.SMO;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.Evaluation;
import java.util.Random;

public class SVMGridSearch {
    
    public static void main(String[] args) throws Exception {
        // 加载训练和测试数据
        String trainPath = "./data_old/data.csv"; // 替换为训练集路径
        String testPath = "./data/test.csv";   // 替换为测试集路径
        Instances trainData = DataSource.read(trainPath);
        Instances testData = DataSource.read(testPath);
        trainData.setClassIndex(trainData.numAttributes() - 1); // 假设最后一列为标签
        testData.setClassIndex(testData.numAttributes() - 1);

        // 定义参数网格
        double[] Cs = {0.1, 1.0, 10.0}; // SVM 正则化参数 C 的值范围
        double[] gammas = {0.1, 0.5, 1.0}; // RBFKernel 中的 gamma 参数的值范围

        // 存储最佳参数和最高分数
        double bestC = Cs[0];
        double bestGamma = gammas[0];
        double bestAccuracy = 0.0;

        // 网格搜索
        for (double C : Cs) {
            for (double gamma : gammas) {
                SMO classifier = new SMO();
                try {
                    // 设置 SVM 参数 - 使用 RBFKernel
                    String[] options = weka.core.Utils.splitOptions("-C " + C + " -L 0.001 -P 1.0E-12 -N 0 -V -1 -W 1 "
                            + "-K \"weka.classifiers.functions.supportVector.RBFKernel -C 0 -G " + gamma + "\"");
                    classifier.setOptions(options);

                    // 在训练集上训练模型
                    classifier.buildClassifier(trainData);

                    // 在测试集上评估模型
                    Evaluation eval = new Evaluation(trainData);
                    eval.evaluateModel(classifier, testData);
                    
                    // 输出当前参数组合的准确率
                    double accuracy = eval.pctCorrect();
                    System.out.println("C = " + C + ", Gamma = " + gamma + " -> Accuracy: " + accuracy + "%");

                    // 更新最佳参数
                    if (accuracy > bestAccuracy) {
                        bestAccuracy = accuracy;
                        bestC = C;
                        bestGamma = gamma;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 输出最佳参数和性能
        System.out.println("Best C: " + bestC + ", Best Gamma: " + bestGamma);
        System.out.println("Best Accuracy: " + bestAccuracy + "%");

        // 使用最佳参数在测试集上进行最终评估
        SMO bestClassifier = new SMO();
        String[] bestOptions = weka.core.Utils.splitOptions("-C " + bestC + " -L 0.001 -P 1.0E-12 -N 0 -V -1 -W 1 "
                + "-K \"weka.classifiers.functions.supportVector.RBFKernel -C 0 -G " + bestGamma + "\"");
        bestClassifier.setOptions(bestOptions);
        bestClassifier.buildClassifier(trainData);

        Evaluation finalEval = new Evaluation(trainData);
        finalEval.evaluateModel(bestClassifier, testData);
        System.out.println("Final Accuracy with best params: " + finalEval.pctCorrect() + "%");
    }
}
