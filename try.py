import pandas as pd
from collections import defaultdict

class DataInstance:
    def __init__(self, label, fft_features):
        self.label = label
        self.fft_features = fft_features

# 加载 CSV 并转换为 trainingData 结构
def load_from_csv_with_pandas(csv_file_path):
    # 使用 pandas 读取 CSV 文件
    df = pd.read_csv(csv_file_path, header=None)
    
    # 最后一列是标签，其他列是 fft_features
    training_data = defaultdict(list)
    
    for _, row in df.iterrows():
        fft_features = row.iloc[:-1].values.astype(float)  # 取所有除最后一列的列作为特征
        label = row.iloc[-1]  # 最后一列是标签
        instance = DataInstance(label, fft_features)
        training_data[label].append(instance)
    
    return training_data

# 示例用法
csv_file_path = './output/data.csv'
training_data = load_from_csv_with_pandas(csv_file_path)

# 输出训练数据的情况
for label, instances in training_data.items():
    print(f"Class: {label}, Number of instances: {len(instances)}")
