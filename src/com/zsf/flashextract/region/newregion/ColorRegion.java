package com.zsf.flashextract.region.newregion;

import com.zsf.StringProcessor;
import com.zsf.flashextract.regex.RegexCommomTools;
import com.zsf.interpreter.expressions.Expression;
import com.zsf.interpreter.expressions.regex.DynamicRegex;
import com.zsf.interpreter.expressions.regex.Regex;
import com.zsf.interpreter.model.ExamplePair;
import com.zsf.interpreter.model.ExpressionGroup;
import com.zsf.interpreter.model.Match;
import com.zsf.interpreter.model.ResultMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hasee on 2017/3/16.
 */
public class ColorRegion {
    private Color color;
    private String parentDocument;

    private List<Field> fieldsByUser = new ArrayList<Field>();
    private List<Field> fieldsGenerated = new ArrayList<Field>();

    private List<Integer> positiveLineIndex = new ArrayList<Integer>();
    private List<Integer> negativeLineIndex = new ArrayList<Integer>();
    private List<Integer> needSelectLineIndex = new ArrayList<Integer>();

    private List<LineField> lineFields = new ArrayList<LineField>();

    private List<Regex> lineSelectors = new ArrayList<Regex>();
    private Regex curLineSelector = null;

    private ExpressionGroup expressionGroup;
    private Expression curExpression;

    public ColorRegion(Color color, String parentDocument) {
        this.color = color;
        this.parentDocument = parentDocument;

        lineFields = new ArrayList<LineField>();
        String[] splitedLines = parentDocument.split("\n");
        for (int i = 0; i < splitedLines.length; i++) {
            String line = splitedLines[i];
            int beginPos = RegexCommomTools.indexNOf(parentDocument, "\n", i) + 1;
            int endPos = beginPos + line.length();
            lineFields.add(new LineField(null, beginPos, endPos, line));
        }
    }

    public void selectField(int beginPos, int endPos, String text) {
        int lineIndex = calculateLineIndex(beginPos, endPos);
        Field field = new PlainField(lineFields.get(lineIndex), color, beginPos, endPos, text);
        if (!fieldsByUser.contains(field)) {
            fieldsByUser.add(field);
            addPositiveLineIndex(lineIndex);
        }
        if (needGenerateLineSelectors()) {
            doGenerateLineSelectors();

            // FIXME: 2017/3/16 下面几个可能要一起调用？
            generateLineFieldsByCurSelector();

            generateExpressionGroup();
            generatePlainFieldsByCurExp();
        }
    }

    private boolean needGenerateLineSelectors() {
        // 当选中fields>=2时需要产生selector
        // 即使是已经有selector了，如果又选择了新的field，就可以认为之前的selector不够好，要重新生成
        return fieldsByUser.size() >= 2;
    }

    /**
     * 在selectField()时，如果判断需要产生LineSelectors就会调用此方法，产生LineSelecotr,排序结果后设置curSelector
     */
    private void doGenerateLineSelectors() {
        RegexCommomTools.addDynamicToken(fieldsByUser, MainDocument.usefulRegex);

        List<List<Regex>> startWithReges = new ArrayList<List<Regex>>();
        List<List<Regex>> endWithReges = new ArrayList<List<Regex>>();
        int curDeepth = 1;
        int maxDeepth = 3;

        for (int index : positiveLineIndex) {
            List<Match> matches = RegexCommomTools.buildStringMatches(lineFields.get(index).getText(), MainDocument.usefulRegex);
            startWithReges.add(RegexCommomTools.buildStartWith(curDeepth, maxDeepth, matches, 0, new DynamicRegex("", "")));
            endWithReges.add(RegexCommomTools.buildEndWith(curDeepth, maxDeepth, matches, lineFields.get(index).getText().length(), new DynamicRegex("", "")));
        }
        System.out.println("start with:");
        System.out.println(startWithReges.get(1));
        System.out.println("end with:");
        System.out.println(endWithReges);

        List<Regex> startWithLineSelector = RegexCommomTools.deDuplication(startWithReges, true);
        List<Regex> endWithLineSelector = RegexCommomTools.deDuplication(endWithReges, false);

        System.out.println(startWithLineSelector);
        System.out.println(endWithLineSelector);

        // 利用positive和negativeExamples对selectors进行筛选
        List<Regex> usefulLineSelector = new ArrayList<Regex>();
        usefulLineSelector.addAll(RegexCommomTools.filterUsefulSelector(startWithLineSelector, lineFields, positiveLineIndex, getNegativeLineIndex()));
        usefulLineSelector.addAll(RegexCommomTools.filterUsefulSelector(endWithLineSelector, lineFields, positiveLineIndex, getNegativeLineIndex()));

        // TODO: 2017/3/16 lineSelector的ranking
        this.lineSelectors = usefulLineSelector;
        this.curLineSelector = lineSelectors.get(0);
    }

    private void addPositiveLineIndex(int lineIndex) {
        if (!positiveLineIndex.contains(lineIndex)) {
            positiveLineIndex.add(lineIndex);
        }
    }

    /**
     * 根据当前的selector产生fileds，会在getFileds()之前调用
     */
    private void generateLineFieldsByCurSelector() {
        needSelectLineIndex = new ArrayList<Integer>();
        if (curLineSelector == null) {
            // FIXME: 2017/3/16 需要一个优雅的解决方案
            return;
        }
        for (int i = 0; i < lineFields.size(); i++) {
            if (lineFields.get(i).canMatch(curLineSelector)) {
                needSelectLineIndex.add(i);
            }
        }
    }

    private void generateExpressionGroup(){
        List<ExamplePair> examplePairs = new ArrayList<ExamplePair>();
        for (Field field : fieldsByUser) {
            examplePairs.add(new ExamplePair(field.getParentField().getText(), field.getText()));
        }
        StringProcessor stringProcessor = new StringProcessor();
        List<ResultMap> resultMaps = stringProcessor.generateExpressionsByExamples(examplePairs);
        ExpressionGroup expressionGroup = stringProcessor.selectTopKExps(resultMaps, 10);

        this.expressionGroup = expressionGroup;
        if (expressionGroup!=null){
            curExpression = expressionGroup.getExpressions().get(0);
        }
    }

    private void generatePlainFieldsByCurExp(){
        if (curExpression!=null){
            for (int lineIndex : needSelectLineIndex) {
                // TODO: 2017/3/16 去重复？
                // TODO: 2017/3/16 ChildField。坐标
                this.fieldsGenerated.addAll(
                        lineFields.get(lineIndex).selectChildFieldByExp(curExpression));
            }
        }
    }

    private int calculateLineIndex(int beginPos, int endPos) {
        String textBeforeSelect = parentDocument.substring(0, beginPos);
        int count = 0;
        int index = 0;
        while (true) {
            index = textBeforeSelect.indexOf("\n", index + 1);
            if (index > 0) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    public List<Field> getFieldsGenerated() {
        return fieldsGenerated;
    }

    public Color getColor() {
        return color;
    }

    public String getParentDocument() {
        return parentDocument;
    }

    public List<Regex> getLineSelectors() {
        return lineSelectors;
    }

    public Regex getCurLineSelector() {
        return curLineSelector;
    }

    public List<Integer> getNegativeLineIndex() {
        negativeLineIndex = new ArrayList<Integer>();
        int max = 0;
        for (int index : positiveLineIndex) {
            max = Math.max(max, index);
        }
        for (int i = 0; i < max; i++) {
            if (!positiveLineIndex.contains(i)) {
                negativeLineIndex.add(i);
            }
        }
        return negativeLineIndex;
    }
}
