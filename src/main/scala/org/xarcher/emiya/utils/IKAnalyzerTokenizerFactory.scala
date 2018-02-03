package org.wltea.analyzer.lucene

import java.io.Reader
import java.util

import org.apache.lucene.analysis.Tokenizer
import org.apache.lucene.analysis.util.TokenizerFactory
import org.apache.lucene.util.AttributeFactory

class IKAnalyzerTokenizerFactory(val args: java.util.Map[String, String]) extends TokenizerFactory(args) {

  this.setUseSmart(args.get("useSmart").toString == "true")
  private var useSmart = false

  def setUseSmart(useSmart: Boolean): Unit = {
    this.useSmart = useSmart
  }

  def getUseSmart(useSmart: Boolean): Unit = {
    this.useSmart = useSmart
  }

  /*def create(factory: Nothing, input: Reader): Tokenizer = {
    val _IKTokenizer = new IKTokenizer(this.useSmart)
    _IKTokenizer
  }*/

  override def create(factory: AttributeFactory): Tokenizer = {
    val _IKTokenizer = new IKTokenizer(this.useSmart)
    _IKTokenizer
  }
}