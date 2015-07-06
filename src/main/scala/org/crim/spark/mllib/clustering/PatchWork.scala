package org.crim.spark.mllib.clustering

import org.apache.spark.Logging
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import scala.collection.mutable.ListBuffer

/**
 * PatchWork is a novel clustering algorithm for Apache Spark.
 * It has linear complexity and near linear horizontal scalability.
 * As a result, PatchWork can cluster a billion points in a few minutes only,
 * a 40x improvement over Spark MLLib native implementation of the well-known K-Means.
*/

class PatchWorkCluster (private var id: Int) extends Serializable{
  def getID: Int = this.id
  val cellsList: ListBuffer[PatchWorkCellKey] = new ListBuffer[PatchWorkCellKey]
}

class PatchWorkCellKey (var cellNumber:String,var cellArray:Array[Int],var cluster:Option[PatchWorkCluster]) extends Serializable {
  def this(p: Array[Int]) {
    this(p.mkString(",").trim,p,None)
  }

  def this(p: String) {
    this(p,p.split(",").map(_.toInt),None)
  }

  override def equals(o: Any) = o match {
    case that: PatchWorkCellKey => that.cellNumber.equals(this.cellNumber)
    case _ => false
  }
}

class PatchWorkCell(val cellNumber: Array[Int]) extends Serializable {

  def this(p: Array[Double], eps: Array[Double]) {
    this(p.zipWithIndex.map(x => math.ceil(x._1 / eps(x._2)).toInt))
  }

  var ptsInCell: Int = 0

  def addPtsToCell = this.ptsInCell += 1

  def mergeCell(points: Int): this.type = {
    this.ptsInCell += points
    this
  }

  override def equals(o: Any) = o match {
    case that: PatchWorkCell => that.cellNumber.equals(this.cellNumber)
    case _ => false
  }
}

class PatchWordModel(private var epsilon: Array[Double],
                     var cardinalsPatchwork: RDD[(String, Int)],
                     var clusters: List[PatchWorkCluster]) extends Serializable {

  def getEpsilon: Array[Double] = epsilon

  def setEpsilon(epsilon: Array[Double]): this.type = {
    this.epsilon = epsilon
    this
  }

  def setCardinalsPatchwork(cardinalsPatchwork: RDD[(String, Int)]): this.type = {
    this.cardinalsPatchwork = cardinalsPatchwork
    this
  }

  def getCardinalsPatchwork: RDD[(String, Int)] = this.cardinalsPatchwork

  def predict(point: Array[Double]): PatchWorkCluster = {
    val cellKey = new PatchWorkCellKey(point.zipWithIndex.map(x => math.ceil(x._1/this.epsilon(x._2)).toInt))
    val cl = this.clusters.filter(cluster => cluster.cellsList.contains(cellKey))
    if (cl.isEmpty) {
      // point is noise
      val cluster = new PatchWorkCluster(-1)
      cluster.cellsList.append(cellKey)
      cluster
    }
    else {
      cl.head
    }
  }
}

class PatchWork(
                 // cell size
                 private var epsilon: Array[Double],
                 // min number of points in a cell
                 private var minPts: Int,
                 // density changes between cells
                 private var ratio: Double,
                 // minimum spatial size of clusters
                 private var minCell: Int
                 ) extends Serializable with Logging {

  // PatchWork.PatchWork getters and setters
  def getEpsilon: Array[Double] = epsilon

  def setEpsilon(epsilon: Array[Double]): this.type = {
    this.epsilon = epsilon
    this
  }

  def getMinPts: Int = minPts

  def setMinPts(minPts: Int): this.type = {
    this.minPts = minPts
    this
  }

  // Main source code

  /**
   * This function check if the data has been cached or not
   * uncached data hurts perfomance of the algortihm
   * @param data
   * @return
   */
  def run(data: RDD[Array[Double]]): PatchWordModel = {

    if (data.getStorageLevel == StorageLevel.NONE) {
      logWarning("The input data is not directly cached, which may hurt performance if its"
        + " parent RDDs are also uncached.")
    }

    // Launch the algorithm
    val model = runAlgorithm(data)

    // Warn at the end of the run as well, for increased visibility.
    if (data.getStorageLevel == StorageLevel.NONE) {
      logWarning("The input data was not directly cached, which may hurt performance if its"
        + " parent RDDs are also uncached.")
    }
    model
  }

  /**
   * Main function of the algorithm with the map and reduce by key operations
   * @param data
   * @return
   */
  def runAlgorithm(data: RDD[Array[Double]]): PatchWordModel = {
    // map part of the algorithm, creating pairs of (cellID,1)
    val pairs: RDD[(String, Int)] = data.map(p => (p.zipWithIndex.map(x => math.ceil(x._1 / epsilon(x._2)).toInt).mkString(","), 1))
    // reduce by key that transforms the pairs into pairs with the right value
    val cardinalsPatchwork: RDD[(String, Int)] = pairs.reduceByKey(_ + _)
    // creates clusters from the set of cells got from last operation
    val clusters = createClusters(cardinalsPatchwork.map(x => (x._1.split(",").map(_.toInt), x._2)).collect().toList, minPts)
    // returning the PatchWorkModel
    new PatchWordModel(this.epsilon, cardinalsPatchwork.map(x => (x._1, x._2)), clusters)
  }

  /**
   * computes all possible arrays in the neighbourhood of a given array
   * @param p
   * @return
   */
  def getNearCell(p: Array[Int]): List[Array[Int]] = {
    if (p.size == 1) {
      List(Array(p.head), Array(p.head - 1), Array(p.head + 1))
    }
    else {
      List.concat(
        getNearCell(p.tail).map(x => Array.concat(Array(p.head), x)),
        getNearCell(p.tail).map(x => Array.concat(Array(p.head - 1), x)),
        getNearCell(p.tail).map(x => Array.concat(Array(p.head + 1), x))
      )
    }
  }

  def nearCells(p: Array[Int]): List[Array[Int]] = {
    val nearCellsList:ListBuffer[Array[Int]] = new ListBuffer[Array[Int]]
    for (i <- Range(0,p.length)){
      val tempP1 = p.clone()
      tempP1.update(i,p(i)-1)
      nearCellsList.append(tempP1)
      val tempP2 = p.clone()
      tempP2.update(i,p(i)+1)
      nearCellsList.append(tempP2)
    }
    nearCellsList.toList
  }

  /**
   * returns inner cells of cell
   * @param cell
   * @return
   */
  def innerCells(cell: Array[Int]): List[Array[Int]] = {
    if(cell.length < 3 ) {
      getNearCell(cell)
    }
    else {
      nearCells(cell)
    }
  }

  /**
   * From a unique cell, expands the cluster to the nearest cells if they meet requirements
   * @param cardinalsPatchwork : set of Cells
   * @param cluster : current cluster being created
   * @param cell  : last considered cell
   * @param card : value to perform the ratio checking
   * @return
   */
  def expandCluster(cardinalsPatchwork: List[(Array[Int], Int)], cluster: PatchWorkCluster, cell: Array[Int], card: Int) {
    // looking for all nearest cells and foreach perform
    innerCells(cell).foreach { inCell =>
      // computes the cell key
      val inCellKey = new PatchWorkCellKey(inCell)
      // if this cell is not in the cluster already
      if (!cluster.cellsList.contains(inCellKey)) {
        val c = cardinalsPatchwork.filter(c => new PatchWorkCellKey(c._1).equals(inCellKey))
        // if this cellID is in the set of cells, meaning their is at least one point in this cell
        if (!c.isEmpty) {
          // if this cell meets the requirements
          if (c.head._2 >= card * this.ratio && c.head._2 >= this.minPts) {
            // we add this cell to the cluster and expand the cluster from her
            cluster.cellsList.append(inCellKey)
            // Ratio from origin cell
            //expandCluster(cardinalsPatchwork, cluster, inCell, card)
            // Ratio from nearest cell
            expandCluster(cardinalsPatchwork, cluster, inCell, c.head._2)
          }
        }
      }
    }
  }

  /**
   * creating clusters from the set of cells
   * @param cardinalsPatchwork
   * @param minPts
   * @return
   */
  def createClusters(cardinalsPatchwork: List[(Array[Int], Int)], minPts: Int): List[PatchWorkCluster] = {
    // initialising an empty list of cluster
    val clusterList: ListBuffer[PatchWorkCluster] = new ListBuffer[PatchWorkCluster]
    var id = 1
    // for each cells in the set of cells
    cardinalsPatchwork.sortBy(_._2).foreach { cell =>
      //if this cell has enough points
      if (cell._2 >= minPts) {
        val cellKey = new PatchWorkCellKey(cell._1)
        // if this cells is not already in a cluster
        if (clusterList.filter(cluster => cluster.cellsList.contains(cellKey)).isEmpty) {
          // we create a new cluster and expand it
          val cluster = new PatchWorkCluster(id)
          id = id + 1
          cluster.cellsList.append(cellKey)
          expandCluster(cardinalsPatchwork, cluster, cell._1, cell._2)
          // once the cluster is expanded we add it to the list
          clusterList.append(cluster)
        }
      }
    }
    // we keep only cluster that meets the minimum spatial size criteria
    clusterList.filter(cluster => cluster.cellsList.size > this.minCell).toList
  }

}
