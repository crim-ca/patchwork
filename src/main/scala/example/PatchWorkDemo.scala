import ca.crim.spark.mllib.clustering._
import org.apache.spark.rdd.RDD
import org.apache.spark.{ SparkContext, SparkConf }

/**
 * Sample application that leverages PatchWork to cluster different synthetic datasets.
 * While the sample datasets are of limited size 5000-10000 data points,
 * PatchWork can scale to tens of billions of points.
 *
 * You need Apache Spark running locally for this demo.
 * You can also run the demo on a cluster, just update the launch script runPatchWork.sh
 * and the SparkContext with your settings and execute build.sh to compile.
 *
 * To run the demo: ./runPatchwork.sh
 */
object PatchWorkDemo extends App {
  val sc = new SparkContext(new SparkConf()
    .setMaster("local[4]")
    .setAppName("PatchworkDemo")
  )

  // Reading and parsing Data
  val dataRDD: RDD[Array[Double]] = sc.textFile("datasets/Compound.csv")
    .map(_.split(",")).map(s => Array(s(0).toDouble, s(1).toDouble)).cache

  // PatchWork parameters
  val epsilon = Array(0.2, 0.2)
  val minPts = 1
  val minCellInCluster = 40
  val ratio = 0.0

  // Training a model with the data
  val (patchworkModel, execTime) = Utils.time(
    new PatchWork(epsilon, minPts, ratio, minCellInCluster).run(dataRDD)
  )

  // Display the cluster for each data point
  dataRDD.collect().map(x =>
    x(0) + "\t" + x(1) + "\t" + patchworkModel.predict(x).getID
  ).foreach(println)

  // Display some stats about the clusters
  var cs = ""
  for (i <- Range(0, patchworkModel.clusters.size)) {
    cs = cs + "   cluster " + patchworkModel.clusters(i).getID + " has " + patchworkModel.clusters(i).cellsList.size + " cells \n"
  }
  println("\n----------------------------------------- \n" +
    "number of points : " + dataRDD.count() + "\n" +
    "number of clusters : " + patchworkModel.clusters.size + "\n" +
    "----------------------------------------- \n" +
    cs +
    "----------------------------------------- \n" +
    "size of epsilon : [" + epsilon.mkString(",") + "] \n" +
    "min pts in each cell : " + minPts + "\n" +
    "time of training : " + execTime + " ms" + "\n----------------------------------------- \n")

  sc.stop
} // eo PatchWorkDemo

object Utils {
  /**
   * Executes the given function and reports the running time
   *
   * @param f The function to run
   * @tparam A Type returned by the given function
   * @return A pair with the function output and the time in milliseconds
   */
  def time[A](f: => A): (A, Long) = {
    val s = System.currentTimeMillis()
    val ret = f
    (ret, System.currentTimeMillis() - s)
  }
}

