package com.javabetter.basicgrammar;

/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/28
 */
public class Operator {
    //一.算数运算符
    public static void main(String[] args) {
        int a = 10;
        int b = 5;
        System.out.println(a + b);//15
        System.out.println(a - b);//5
        System.out.println(a * b);//50
        System.out.println(a / b);//2
        System.out.println(a % b);//0
        b = 3;
        System.out.println(a + b);//13
        System.out.println(a - b);//7
        System.out.println(a * b);//30
        System.out.println(a / b);//3
//        System.out.println(a % b);//1
        float c = 3.0f;
        double d = 3.0;
        System.out.println(a / c); // 3.3333333
        System.out.println(a / d); // 3.3333333333333335
        System.out.println(a % c); // 1.0
        System.out.println(a % d); // 1.0
        System.out.println(10.0 / 0.0); // Infinity
        System.out.println(0.0 / 0.0); // NaN
//        Infinity 的中文意思是无穷大，NaN 的中文意思是这不是一个数字（Not a Number）。
        int x = 10;
        System.out.println(x++);//10 (11)
        System.out.println(++x);//12
        System.out.println(x--);//12 (11)
        System.out.println(--x);//10
        int y = ++x;
        System.out.println(y + " " + x);// 11 11
        x = 10;
        y = x++;
        System.out.println(y + " " + x);// 10 11
    }


}
