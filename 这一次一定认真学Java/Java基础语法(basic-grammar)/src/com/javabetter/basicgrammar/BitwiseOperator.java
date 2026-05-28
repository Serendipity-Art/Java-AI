package com.javabetter.basicgrammar;

/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/28
 */
public class BitwiseOperator {
    public static void main(String[] args) {
        //位运算符
        int c = 60, d = 13;
        System.out.println("a 的二进制：" + Integer.toBinaryString(c)); // 111100
        System.out.println("b 的二进制：" + Integer.toBinaryString(d)); // 1101

        int e = c & d;
        System.out.println("c & d：" + e + "，二进制是：" + Integer.toBinaryString(e));

        e = c | d;
        System.out.println("c | d：" + e + "，二进制是：" + Integer.toBinaryString(e));

        c = c ^ d;
        System.out.println("c ^ d：" + e + "，二进制是：" + Integer.toBinaryString(e));

        e = ~c;
        System.out.println("~c：" + e + "，二进制是：" + Integer.toBinaryString(e));

        e = c << 2;
        System.out.println("e << 2：" + e + "，二进制是：" + Integer.toBinaryString(e));

        e = c >> 2;
        System.out.println("c >> 2：" + e + "，二进制是：" + Integer.toBinaryString(e));

        e = c >>> 2;
        System.out.println("c >>> 2：" + e + "，二进制是：" + Integer.toBinaryString(e));
        //四.逻辑运算符
        int a=10;
        int b=5;
        int x=20;
        System.out.println(a<b&&a<x);//false && true = false
        System.out.println(a>b||a<x);//true || true = true
    }

}
