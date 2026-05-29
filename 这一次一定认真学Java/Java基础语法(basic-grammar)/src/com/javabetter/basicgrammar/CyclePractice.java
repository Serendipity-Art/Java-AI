package com.javabetter.basicgrammar;

/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/28
 */
public class CyclePractice {
    public static void main(String[] args) {
        int x = 19;
        int y = x++ + ++x;
        System.out.println("x=" + x + ", y=" + y);
    }
}
