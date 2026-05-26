package com.javabetter.basicgrammar;

/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/26
 */
public class BasicDataType {
    //一.基本数据类型
    //(1)布尔类型,取值范围trueo or false
    boolean hanMoney=true;
    boolean hasGirlfriend=false;
    //(2)byte类型,取值范围-128~127
    //(3)short类型,取值范围-32768~32767
    //(4)int类型,取值范围-2147,483,648(-2^31)~2147,483,647(2^31-1)
    //(5)long类型,取值范围非常大(-2^63~2^63)
    //(6)float类型,单精度类型
    //(7)double类型,双精度类型
    //(8)char类型,字符类型
    //单精度(1位符号,8位指数,23位小数,32位(4字节))和双精度(1位符号,11位指数,52位小数,64位(8字节))
    public static void main(String[] args) {
        //二.int和char类型互化
        int value_int = 65;
        char value_char = (char)value_int;//(1)强制转换,'A'对应的ASCIi值为65
        System.out.println(value_char);
        int radix=10;
        int value_int2=6;
        char value_char2 = Character.forDigit(value_int2,radix);//(2)使用Character.forDigit()转换
        System.out.println(value_char2);
        int value_int3=1;
        char value_char3 = Integer.toString(value_int3).charAt(0);//(3)Integer的toString()方法+String的`charAt()
        System.out.println(value_char3);
        int a='a';//(4)char转换为int,自动类型转换,不适用'1'
        System.out.println(a);
        int b='1'-'0';
        System.out.println(b);
        //三.包装器类型
        //Byte(byte),Short(short),Integer(int),Long(long),Float(float),Double(double)
        //Character(char),Boolean(boolean)
        //使用Integer包装饰器
        int integervalue= 42;
        System.out.println("整数值:"+integervalue);
        //将字符串转化为整数
        String numberString="123";
        int parseInt=Integer.parseInt(numberString);
        System.out.println("整数值:"+parseInt);
        //使用Character包修饰器类型
        char character='a';
        System.out.println("字符:"+character);
        //检查数字是否为数字
        char testChar='9';
        if (Character.isDigit(testChar)) {
            System.out.println("字符是一个数字");
        }
    }
}
