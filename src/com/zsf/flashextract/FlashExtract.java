package com.zsf.flashextract;

import com.zsf.flashextract.region.Region;
import com.zsf.interpreter.expressions.regex.*;
import com.zsf.interpreter.model.Match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zsf.interpreter.tool.StringTools.getCommonStr;
import static com.zsf.interpreter.tool.StringTools.getReversedStr;

/**
 * Created by zsf on 2017/3/13.
 */
public class FlashExtract {
    private String inputDocument;
    List<Region> documentRegions = new ArrayList<Region>();
    Map<Integer,List<Region>> colorfulRegions=new HashMap<Integer, List<Region>>();

    public static List<Regex> usefulRegex = initUsefulRegex();

    /**
     * 增加有效的token可以强化匹配能力
     * <p>
     * 但是每添加一个token，答案数就要乘以这个token能产生的结果数
     * token过多会导致结果爆炸增长(很容易伤几千万)
     *
     * @return
     */
    private static List<Regex> initUsefulRegex() {
        List<Regex> regexList = new ArrayList<Regex>();
        regexList.add(new NormalRegex("SimpleNumberTok", "[0-9]+"));
        regexList.add(new NormalRegex("DigitToken", "[-+]?(([0-9]+)([.]([0-9]+))?)"));
        regexList.add(new NormalRegex("LowerToken", "[a-z]+"));
        regexList.add(new NormalRegex("UpperToken", "[A-Z]+"));
        regexList.add(new NormalRegex("AlphaToken", "[a-zA-Z]+"));
//        regexList.add(new Regex("WordToken","[a-z\\sA-Z]+")); // 匹配单词的token，会导致结果爆炸增长几十万倍

        // TimeToken可匹配[12:15 | 10:26:59 PM| 22:01:15 aM]形式的时间数据
        regexList.add(new RareRegex("TimeToken", "(([0-1]?[0-9])|([2][0-3])):([0-5]?[0-9])(:([0-5]?[0-9]))?([ ]*[aApP][mM])?"));
        // YMDToken可匹配[10/03/1979 | 1-1-02 | 01.1.2003]形式的年月日数据
        regexList.add(new RareRegex("YMDToken", "([0]?[1-9]|[1|2][0-9]|[3][0|1])[./-]([0]?[1-9]|[1][0-2])[./-]([0-9]{4}|[0-9]{2})"));
        // YMDToken2可匹配[2004-04-30 | 2004-02-29],不匹配[2004-04-31 | 2004-02-30 | 2004-2-15 | 2004-5-7]
        regexList.add(new RareRegex("YMDToken2", "[0-9]{4}-(((0[13578]|(10|12))-(0[1-9]|[1-2][0-9]|3[0-1]))|(02-(0[1-9]|[1-2][0-9]))|((0[469]|11)-(0[1-9]|[1-2][0-9]|30)))"));
        // TextDate可匹配[Apr 03 | February 28 | November 02] (PS:简化版，没处理日期的逻辑错误)
        regexList.add(new RareRegex("TextDate", "(Jan(uary)?|Feb(ruary)?|Ma(r(ch)?|y)|Apr(il)?|Jul(y)?|Ju((ly?)|(ne?))|Aug(ust)?|Oct(ober)?|(Sept|Nov|Dec)(ember)?)[ -]?(0[1-9]|[1-2][0-9]|3[01])"));
        regexList.add(new RareRegex("WhichDayToken", "(Mon|Tues|Fri|Sun)(day)?|Wed(nesday)?|(Thur|Tue)(sday)?|Sat(urday)?"));
//        regices.add(new Regex("AlphaNumToken", "[a-z A-Z 0-9]+"));

        // special tokens
        regexList.add(new EpicRegex("TestSymbolToken", "[-]+"));
        regexList.add(new EpicRegex("CommaToken", "[,]+"));
        regexList.add(new EpicRegex("<", "[<]+"));
        regexList.add(new EpicRegex(">", "[>]+"));
        regexList.add(new EpicRegex("/", "[/]+"));
        regexList.add(new EpicRegex("SpaceToken", "[ ]+")); // 加上之后就出不了结果？？
        // FIXME: 2017/2/5 如果开启这个SpTok在当前算法下会导致解过于庞大
//        regexList.add(new Regex("SpecialTokens","[ -+()\\[\\],.:]+"));

        return regexList;
    }
    /**
     * 在每次有新的input时就调用此方法，可以返回 各个pos上所有能够和input匹配的集合
     * 当generatePosition()需要时，直接根据match的pos(index)去查找使用，避免重复计算
     */
    private static List<Match> buildStringMatches(String inputString) {
        List<Match> matches = new ArrayList<Match>();
        for (int i = 0; i < usefulRegex.size(); i++) {
            Regex regex = usefulRegex.get(i);
            List<Match> curMatcher = regex.doMatch(inputString);
            matches.addAll(curMatcher);
        }
        return matches;
    }

    public String getInputDocument() {
        return inputDocument;
    }

    public void setInputDocument(String inputDocument) {
        this.inputDocument = inputDocument;


        documentRegions = new ArrayList<Region>();
        String[] splitedLines = inputDocument.split("\n");
        for (String line : splitedLines) {
            documentRegions.add(new Region(null, 0, -1, line));
        }
    }

    public void doSelectRegion(int color, int lineIndex, int beginPos, int endPos, String selectedText) {
        List<Region> regions=colorfulRegions.get(color);
        if (regions==null){
            regions=new ArrayList<Region>();
            colorfulRegions.put(color,regions);
        }
        regions.add(new Region(documentRegions.get(lineIndex), beginPos, endPos, selectedText));
        // 直接将这一行设置为positiveLine
        doSetPositiveLineIndex(color,lineIndex);
    }

    Map<Integer,List<Integer>> colorfulPositiveLineIndex=new HashMap<Integer, List<Integer>>();
    public void doSetPositiveLineIndex(int color, int lineIndex) {
        List<Integer> positiveLineIndex=colorfulPositiveLineIndex.get(color);
        if (positiveLineIndex==null){
            positiveLineIndex=new ArrayList<Integer>();
            colorfulPositiveLineIndex.put(color,positiveLineIndex);
        }
        positiveLineIndex.add(lineIndex);
    }
    Map<Integer,List<Integer>> colorfulNegativeLineIndex=new HashMap<Integer, List<Integer>>();
    public List<Integer> getNegativeLineIndex(int color) {
        generateNegativeLineIndex(color);
        return colorfulNegativeLineIndex.get(color);
    }

    private void generateNegativeLineIndex(int color) {
        List<Integer> positiveLineIndex=getPositiveLineIndex(color);
        List<Integer> negativeLineIndex = new ArrayList<Integer>();
        int max = 0;
        for (int index : positiveLineIndex) {
            max = Math.max(max, index);
        }
        for (int i = 0; i < max; i++) {
            if (!positiveLineIndex.contains(i)) {
                negativeLineIndex.add(i);
            }
        }
        colorfulNegativeLineIndex.put(color,negativeLineIndex);
    }

    public List<Integer> getPositiveLineIndex(int color){
        return colorfulPositiveLineIndex.get(color);
    }

    public List<Region> getDocumentRegions() {
        return documentRegions;
    }

    public void setDocumentRegions(List<Region> documentRegions) {
        this.documentRegions = documentRegions;
    }

    public Map<Integer, List<Region>> getColorfulRegions() {
        return colorfulRegions;
    }

    public void setColorfulRegions(Map<Integer, List<Region>> colorfulRegions) {
        this.colorfulRegions = colorfulRegions;
    }

    /**
     * 现在假设所有的待提取数据都处于同一行，所以只有处理第一种颜色的时候会调用这个函数
     *
     * 即由第一种颜色确定dataLines，后面的颜色都只在目标数据中运用FF
     * @return
     */
    public List<Regex> getLineSelector(int color) {
        // FIXME: 2017/3/13 这里的代码还没有重构过，可能存在冗余
        List<Region> selectedRegion=colorfulRegions.get(color);
        addDynamicToken(selectedRegion);

        List<List<Regex>> startWithReges = new ArrayList<List<Regex>>();
        List<List<Regex>> endWithReges = new ArrayList<List<Regex>>();
        int curDeepth=1;
        int maxDeepth=3;
        for (Region region : selectedRegion) {
            List<Match> matches = buildStringMatches(region.getParentRegion().getText());
            startWithReges.add(buildStartWith(curDeepth, maxDeepth, matches, 0, new DynamicRegex("", "")));
            endWithReges.add(buildEndWith(curDeepth, maxDeepth, matches, region.getParentRegion().getText().length(), new DynamicRegex("", "")));
        }
        System.out.println("start with:");
        System.out.println(startWithReges.get(1));
        System.out.println("end with:");
        System.out.println(endWithReges);

        List<Regex> startWithLineSelector = deDuplication(startWithReges, true);
        List<Regex> endWithLineSelector = deDuplication(endWithReges, false);

        System.out.println(startWithLineSelector);
        System.out.println(endWithLineSelector);

        // 利用positive和negativeExamples对selectors进行筛选
        List<Regex> usefulLineSelector = new ArrayList<Regex>();
        usefulLineSelector.addAll(filterUsefulSelector(startWithLineSelector, documentRegions, getPositiveLineIndex(color), getNegativeLineIndex(color)));
        usefulLineSelector.addAll(filterUsefulSelector(endWithLineSelector, documentRegions,getPositiveLineIndex(color), getNegativeLineIndex(color)));

        return usefulLineSelector;
    }

    private static List<Regex> filterUsefulSelector(List<Regex> regices, List<Region> documentRegions,
                                                    List<Integer> positiveLineIndex, List<Integer> negataiveLineIndex) {
        List<Regex> usefulLineSelector = new ArrayList<Regex>();

        for (Regex regex : regices) {
            boolean needAddIn = true;
            for (int index : positiveLineIndex) {
                Region region = documentRegions.get(index);
                if (!region.canMatch(regex)) {
                    needAddIn = false;
                    break;
                }
            }
            for (int index : negataiveLineIndex) {
                Region region = documentRegions.get(index);
                if (region.canMatch(regex)) {
                    needAddIn = false;
                    break;
                }
            }
            if (needAddIn) {
                usefulLineSelector.add(regex);
            }
        }
        return usefulLineSelector;
    }

    private static List<Regex> deDuplication(List<List<Regex>> regexs, boolean isStartWith) {
        List<Regex> deDuplicatedList = new ArrayList<Regex>();

        List<Regex> baseRegexList = regexs.get(0);
        for (Regex baseRegex : baseRegexList) {
            boolean needAddIn = false;
            for (int j = 1; j < regexs.size(); j++) {
                List<Regex> regexList = regexs.get(j);

                for (Regex regex : regexList) {
                    if ((baseRegex.equals(regex))) {
                        needAddIn = true;
                        break;
                    }
                }
                if (!needAddIn) {
                    break;
                }
            }
            if (needAddIn) {
                if (isStartWith) {
                    baseRegex.setReg("^" + baseRegex.getReg());
                    baseRegex.setRegexName("startWith(" + baseRegex.getRegexName() + ")");
                } else {
                    baseRegex.setReg(baseRegex.getReg() + "$");
                    baseRegex.setRegexName("endWith(" + baseRegex.getRegexName() + ")");
                }
                deDuplicatedList.add(baseRegex);
            }
        }
        return deDuplicatedList;
    }

    /**
     * 从当前新选择的区域出发，分别向左&向右匹配相同str作为dynamicToken添加到usefulRegex中
     *
     * @param targetLines
     */
    private static void addDynamicToken(List<Region> targetLines) {
        Region region = targetLines.get(0);

        // 左匹配
        String textBeforeSelected = region.getParentRegion().getText().substring(0, region.getBeginPos() + 1);
        String leftCommonStr = textBeforeSelected;
        System.out.println(textBeforeSelected);
        for (int i = 1; i < targetLines.size(); i++) {
            Region curRegion = targetLines.get(i);
            leftCommonStr = getCommonStr(getReversedStr(leftCommonStr),
                    getReversedStr(curRegion.getParentRegion().getText().substring(0, curRegion.getBeginPos() + 1)));
            leftCommonStr = getReversedStr(leftCommonStr);
            System.out.println("leftCommonStr:  " + leftCommonStr);
        }

        // 右匹配
        String textAfterSelected = region.getParentRegion().getText().substring(region.getEndPos());
        String rightCommonStr = textAfterSelected;
        System.out.println(textAfterSelected);
        for (int i = 1; i < targetLines.size(); i++) {
            Region curRegion = targetLines.get(i);
            rightCommonStr = getCommonStr(rightCommonStr,
                    curRegion.getParentRegion().getText().substring(curRegion.getEndPos()));
            System.out.println("rightCommonStr:  " + rightCommonStr);
        }

        Regex leftRegex = new DynamicRegex("DynamicTok(" + leftCommonStr + ")", leftCommonStr);
        Regex rightRegex = new DynamicRegex("DynamicTok(" + rightCommonStr + ")", rightCommonStr);

        usefulRegex.add(leftRegex);
        usefulRegex.add(rightRegex);
    }

    /**
     * 获得符合所有examples的endWith语法
     *
     * @param curDeepth
     * @param maxDeepth
     * @param matches
     * @param endNode
     * @param lastRegex
     */
    private static List<Regex> buildEndWith(int curDeepth, int maxDeepth,
                                            List<Match> matches, int endNode, Regex lastRegex) {
        if (curDeepth > maxDeepth) {
            return null;
        }
        String connector = "+";
        if (curDeepth == 1) {
            connector = "";
        }
        List<Regex> regexList = new ArrayList<Regex>();
        for (int i = matches.size() - 1; i >= 0; i--) {
            Match match = matches.get(i);
            if ((match.getMatchedIndex() + match.getMatchedString().length()) == endNode) {

                Regex curRegex = new DynamicRegex(match.getRegex().getRegexName() + connector + lastRegex.getRegexName(),
                        match.getRegex().getReg() + lastRegex.getReg());
                regexList.add(curRegex);
                List<Regex> curList = buildEndWith(curDeepth + 1, maxDeepth,
                        matches, match.getMatchedIndex(),
                        curRegex);
                if (curList != null) {
                    regexList.addAll(curList);
                }
            }
        }
        return regexList;
    }

    /**
     * 获得符合所有examples的startWith语法
     *
     * @param curDeepth
     * @param maxDeepth
     * @param matches
     * @param beginNode
     * @param lastRegex
     */
    private static List<Regex> buildStartWith(int curDeepth, int maxDeepth,
                                              List<Match> matches, int beginNode, Regex lastRegex) {
        if (curDeepth > maxDeepth) {
            return null;
        }
        String connector = "+";
        if (curDeepth == 1) {
            connector = "";
        }
        List<Regex> regexList = new ArrayList<Regex>();

        for (int i = 0; i < matches.size(); i++) {
            Match match = matches.get(i);
            if (match.getMatchedIndex() == beginNode) {
                Regex curRegex = new DynamicRegex(lastRegex.getRegexName() + connector + match.getRegex().getRegexName(),
                        lastRegex.getReg() + match.getRegex().getReg());
                regexList.add(curRegex);

                List<Regex> curList = buildStartWith(curDeepth + 1, maxDeepth,
                        matches, match.getMatchedIndex() + match.getMatchedString().length(),
                        curRegex);
                if (curList != null) {
                    regexList.addAll(curList);
                }
            }
        }
        return regexList;
    }
}
