package com.zsf.interpreter.expressions.string;

import com.zsf.interpreter.expressions.Expression;
import com.zsf.interpreter.expressions.pos.PosExpression;

/**
 * Created by hasee on 2017/1/22.
 */
public class SubStringExpression extends StringExpression {
    private PosExpression posExpression1;
    private PosExpression posExpression2;

    public SubStringExpression(PosExpression posExpression1, PosExpression posExpression2) {
        this.posExpression1 = posExpression1;
        this.posExpression2 = posExpression2;
    }

    @Override
    public String interpret(String inputString) {
        int len=inputString.length();

        int pos1=posExpression1.interpret(inputString);
        int pos2=posExpression2.interpret(inputString);
        // pos=-1就表示len-1位，如input="abcde", input.subStr(1,-1)=input.subStr(1,4)="bcd"
        // END_POS表示最后一位,如果如果碰到END_POS，就把pos转换为input.length
        pos1=pos1<0?pos1+len:pos1;
        pos1=pos1==PosExpression.END_POS?len:pos1;
        pos2=pos2<0?pos2+len:pos2;
        pos2=pos2==PosExpression.END_POS?len:pos2;

        if (isIllegalPos(inputString,pos1)||isIllegalPos(inputString,pos2)){
            return "illegalPos";
        }else {
            return inputString.substring(pos1,pos2);
        }
    }

    private boolean isIllegalPos(String inputString, int pos) {
        if (pos<0||pos>inputString.length()){
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("substr(%s,%s)", posExpression1.toString(), posExpression2.toString());
    }

    @Override
    public Expression deepClone() {
        return new SubStringExpression((PosExpression) posExpression1.deepClone(), (PosExpression) posExpression2.deepClone());
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public PosExpression getPosExpression1() {
        return posExpression1;
    }

    public void setPosExpression1(PosExpression posExpression1) {
        this.posExpression1 = posExpression1;
    }

    public PosExpression getPosExpression2() {
        return posExpression2;
    }

    public void setPosExpression2(PosExpression posExpression2) {
        this.posExpression2 = posExpression2;
    }


}
