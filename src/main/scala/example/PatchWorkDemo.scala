import org.crim.spark.mllib.clustering._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}

/**
 * Created by gouinefr on 2015-07-14.
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
  val epsilon = Array(0.2,0.2)
  val minPts = 1
  val minCellInCluster = 40
  val ratio = 0.0
  val patchworkdoop = new PatchWork(epsilon, minPts,ratio, minCellInCluster)

  // Training model
  val (model, time) = Utils.time(patchworkdoop.run(dataRDD))

  // Display the cluster for each data point
  dataRDD.collect().map(x =>
    x(0) + "\t" + x(1) + "\t" + model.predict(x).getID
  ).foreach(println)


  // Display some stats about the clusters
  var cs = ""
  for (i <- Range(0, model.clusters.size)) {
    cs = cs + "   cluster " + model.clusters(i).getID + " has " + model.clusters(i).cellsList.size + " cells \n"
  }
  println("\n----------------------------------------- \n" +
    "number of points : " + dataRDD.count() + "\n" +
    "number of clusters : " + model.clusters.size + "\n" +
    "----------------------------------------- \n" +
    cs +
    "----------------------------------------- \n" +
    "size of epsilon : [" + epsilon.mkString(",") + "] \n" +
    "min pts in each cell : " + minPts + "\n" +
    "time of training : " + time + " ms" + "\n----------------------------------------- \n")

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

