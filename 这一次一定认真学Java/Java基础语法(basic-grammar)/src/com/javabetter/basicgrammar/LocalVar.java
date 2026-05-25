package com.javabetter.basicgrammar;

/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/25
 */
//变量分为成员变量,静态变量和局部变量
public class LocalVar {
    private int a;//成员变量
    static int b;//静态变量(类变量)
    public static void main(String[] args) {
        LocalVar var=new LocalVar();
        System.out.println(var.a);
        System.out.println(b);
    }
}
