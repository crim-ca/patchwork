hdfs dfs -mkdir -p /datasets && hdfs dfs -put datasets/Compound.csv /datasets/
spark-submit --class PatchWorkDemoo --master local[4] bin/patchwork_2.11-1.1.jar