package com.zsf.interpreter.model;

/**
 * 在generateStr时要把各regex和Input匹配，得到一系列的match
 * Created by hasee on 2017/1/22.
 */
public class Match {
    private String inputString;
    private int matchedIndex;
    private String matchedString;
    private Regex regex;

    public Match(String inputString, int matchedIndex, String matchedString, Regex regex) {
        this.inputString = inputString;
        this.matchedIndex = matchedIndex;
        this.matchedString = matchedString;
        this.regex = regex;
    }


    public String getInputString() {
        return inputString;
    }

    public void setInputString(String inputString) {
        this.inputString = inputString;
    }

    public int getMatchedIndex() {
        return matchedIndex;
    }

    public void setMatchedIndex(int matchedIndex) {
        this.matchedIndex = matchedIndex;
    }

    public String getMatchedString() {
        return matchedString;
    }

    public void setMatchedString(String matchedString) {
        this.matchedString = matchedString;
    }

    public Regex getRegex() {
        return regex;
    }

    public void setRegex(Regex regex) {
        this.regex = regex;
    }
}
