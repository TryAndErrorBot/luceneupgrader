/* Generated By:JavaCC: Do not edit this line. QueryParserConstants.java */
package org.trypticon.luceneupgrader.lucene3.internal.lucene.queryParser;


public interface QueryParserConstants {

  int EOF = 0;
  int _NUM_CHAR = 1;
  int _ESCAPED_CHAR = 2;
  int _TERM_START_CHAR = 3;
  int _TERM_CHAR = 4;
  int _WHITESPACE = 5;
  int _QUOTED_CHAR = 6;
  int AND = 8;
  int OR = 9;
  int NOT = 10;
  int PLUS = 11;
  int MINUS = 12;
  int BAREOPER = 13;
  int LPAREN = 14;
  int RPAREN = 15;
  int COLON = 16;
  int STAR = 17;
  int CARAT = 18;
  int QUOTED = 19;
  int TERM = 20;
  int FUZZY_SLOP = 21;
  int PREFIXTERM = 22;
  int WILDTERM = 23;
  int RANGEIN_START = 24;
  int RANGEEX_START = 25;
  int NUMBER = 26;
  int RANGEIN_TO = 27;
  int RANGEIN_END = 28;
  int RANGEIN_QUOTED = 29;
  int RANGEIN_GOOP = 30;
  int RANGEEX_TO = 31;
  int RANGEEX_END = 32;
  int RANGEEX_QUOTED = 33;
  int RANGEEX_GOOP = 34;

  int Boost = 0;
  int RangeEx = 1;
  int RangeIn = 2;
  int DEFAULT = 3;

  String[] tokenImage = {
    "<EOF>",
    "<_NUM_CHAR>",
    "<_ESCAPED_CHAR>",
    "<_TERM_START_CHAR>",
    "<_TERM_CHAR>",
    "<_WHITESPACE>",
    "<_QUOTED_CHAR>",
    "<token of kind 7>",
    "<AND>",
    "<OR>",
    "<NOT>",
    "\"+\"",
    "\"-\"",
    "<BAREOPER>",
    "\"(\"",
    "\")\"",
    "\":\"",
    "\"*\"",
    "\"^\"",
    "<QUOTED>",
    "<TERM>",
    "<FUZZY_SLOP>",
    "<PREFIXTERM>",
    "<WILDTERM>",
    "\"[\"",
    "\"{\"",
    "<NUMBER>",
    "\"TO\"",
    "\"]\"",
    "<RANGEIN_QUOTED>",
    "<RANGEIN_GOOP>",
    "\"TO\"",
    "\"}\"",
    "<RANGEEX_QUOTED>",
    "<RANGEEX_GOOP>",
  };

}
