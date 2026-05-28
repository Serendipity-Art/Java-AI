package com.javabetter.basicgrammar;

/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/28
 */
public class ConditionalControl {
    public static void main(String[] args) {
        //条件判断年份是否为闰年
        int year = 2000;
        if ((year % 4 == 0) && (year % 100 != 0)||(year % 400 == 0)) {
            System.out.println(year + "是闰年");
        } else {
            System.out.println(year + "不是闰年");
        }
    }
}
