package websiteschema.mpsegment.filter.ner

import websiteschema.mpsegment.hmm.{HmmClassifier, HmmModel}
import websiteschema.mpsegment.util.FileUtil
import websiteschema.mpsegment.conf.MPSegmentConfiguration
import websiteschema.mpsegment.filter.AbstractSegmentFilter
import websiteschema.mpsegment.dict.POSUtil
import websiteschema.mpsegment.concept.Concept

class HmmNameFilter(config: MPSegmentConfiguration, classifier: HmmClassifier) extends AbstractSegmentFilter {

  private var nameStartIndex = -1
  private var nameEndIndex = -1
  private var numOfNameWordItem = -1
  private var shouldSeparateXing = false
  private var labels: Seq[String] = null
  private var curIndex = 0

  override def doFilter() {
    val words = segmentResult.getWordAtoms().map(wordAtom => wordAtom.word)
    labels = classifier.classify(words)
    for (index <- 0 until labels.length; label = labels(index)) {
      curIndex = index
      if (label != "A") {
        if (label == "B" && (nextLabel == "E" || nextLabel == "Z")) {
          processXingAndSingleName
        } else if (label == "B" && nextLabel == "C" && nextNextLabel == "D") {
          processXingAndDoubleName
        } else if (label == "B" && nextLabel == "G") {
          processXingAndSuffix
        } else if (label == "F" && nextLabel == "B") {
          processXingAndPrefix
        } else if (label == "X" && nextLabel == "D") {
          processWordContainsXingAndOtherName
        } else if (label == "Y") {
          separateWordAt(curIndex, POSUtil.POS_NR, POSUtil.POS_NR)
        } else if (label == "U" && (nextLabel == "E" || nextLabel == "Z")) {
          separateWordAt(curIndex, POSUtil.POS_UNKOWN, POSUtil.POS_NR)
          processXingAndSingleName
        } else if (label == "U" && nextLabel == "C" && nextNextLabel == "D") {
          separateWordAt(curIndex, POSUtil.POS_UNKOWN, POSUtil.POS_NR)
          processXingAndDoubleName
        } else if (label == "B" && nextLabel == "C" && nextNextLabel == "V") {
          processXingAndSingleName
          separateWordAt(curIndex + 2, POSUtil.POS_NR, POSUtil.POS_UNKOWN)
          setWordIndexesAndPOSForMerge(curIndex, curIndex + 1, POSUtil.POS_NR)
        } else if (label == "B" && nextLabel == "V") {
          processXingAndSingleName
          separateWordAt(curIndex + 1, POSUtil.POS_NR, POSUtil.POS_UNKOWN)
        } else if (label == "H" && "IJ".contains(nextLabel)) {
          processForeignName
        }
      }
    }
  }

  def processForeignName {
    nameStartIndex = curIndex
    nameEndIndex = labels.indexWhere(l => !"IJ".contains(l), curIndex + 1) - 1
    nameEndIndex = if (nameEndIndex > 0) nameEndIndex else segmentResult.length - 1
    shouldSeparateXing = false
    processPotentialName
  }

  private def processWordContainsXingAndOtherName {
    nameStartIndex = curIndex
    nameEndIndex = curIndex + 1
    shouldSeparateXing = true

    val word = segmentResult(nameStartIndex).word
    segmentResult(nameStartIndex).word = word.charAt(0).toString
    segmentResult(nameEndIndex).word = word.substring(1) + segmentResult(nameEndIndex).word

    processPotentialName
  }

  private def processXingAndSuffix {
    nameStartIndex = curIndex
    nameEndIndex = curIndex + 1
    shouldSeparateXing = false
    processPotentialName
  }

  private def processXingAndPrefix {
    nameStartIndex = curIndex
    nameEndIndex = curIndex + 1
    shouldSeparateXing = false
    processPotentialName
  }

  private def processXingAndDoubleName {
    nameStartIndex = curIndex
    nameEndIndex = curIndex + 2
    shouldSeparateXing = true
    processPotentialName
  }

  private def processXingAndSingleName {
    nameStartIndex = curIndex
    nameEndIndex = curIndex + 1
    shouldSeparateXing = true
    processPotentialName
  }

  private def nextLabel = if (labels.length > curIndex + 1) labels(curIndex + 1) else ""

  private def nextNextLabel = if (labels.length > curIndex + 2) labels(curIndex + 2) else ""

  private def processPotentialName {
    if (nameEndIndex - nameStartIndex >= 1) {
      recognizeNameWord
    }
    resetStatus()
  }

  private def resetStatus() {
    shouldSeparateXing = false
    nameStartIndex = -1
    nameEndIndex = -1
  }

  private def recognizeNameWord: Int = {
    numOfNameWordItem = nameEndIndex - nameStartIndex + 1
    if (numOfNameWordItem > 0) {
      if (config.isXingMingSeparate()) {
        separateXingMing
      } else if (numOfNameWordItem >= 2) {
        setWordIndexesAndPOSForMerge(nameStartIndex, nameEndIndex, POSUtil.POS_NR)
        segmentResult.setConcept(nameStartIndex, Concept.UNKNOWN.getName())
      }
    }
    numOfNameWordItem
  }

  private def separateXingMing {
    val isChineseName = numOfNameWordItem <= 3
    if (isChineseName && shouldSeparateXing) {
      if (numOfNameWordItem >= 2) {
        val numOfMingWord = numOfNameWordItem - 1
        setWordIndexesAndPOSForMerge(nameStartIndex + 1, nameStartIndex + numOfMingWord, POSUtil.POS_NR)
      }
      segmentResult.setPOS(nameStartIndex, POSUtil.POS_NR)
      segmentResult.setConcept(nameStartIndex, Concept.UNKNOWN.getName())
      segmentResult.setConcept(nameStartIndex + 1, Concept.UNKNOWN.getName())
    } else {
      setWordIndexesAndPOSForMerge(nameStartIndex, (nameStartIndex + numOfNameWordItem) - 1, POSUtil.POS_NR)
      segmentResult.setConcept(nameStartIndex, Concept.UNKNOWN.getName())
    }
  }

}

object HmmNameFilter {
  val model = new HmmModel()
  model.load(FileUtil.getResourceAsStream("ner-hmm.m"))

  def apply(config: MPSegmentConfiguration): HmmNameFilter = {
    val classifier = new HmmClassifier
    classifier.setModel(model)
    new HmmNameFilter(config, classifier)
  }
}