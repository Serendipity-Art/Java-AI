package com.javabetter.basicgrammar;

import java.util.Random;
import java.util.Scanner;

/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/28
 */
public class CycleControl {
    public static void main(String[] args) {
        //循环控制
        //一.打印99乘法表
        for (int i = 1; i < 10; i++) {
            System.out.println();
            for (int j = 1; j <= i; j++) {
                System.out.printf("%d*%d=%d ", i, j, i * j);
            }
        }
        //二.求1+1!/1+2!/1+...+20!/1
        double sum = 0;
        double num = 1.0;
        for (int i = 1; i <= 3; i++) {
            num *= i;
            sum += 1.0 / num;
        }
        System.out.println();
        System.out.println(sum);
        //三.求阶乘
        int n = 5;
        int factorial = 1;
        for (int i = 5; i >= 1; i--) {
            factorial *= i;
        }
        System.out.println(factorial);
        //四.判断水仙花数
        for (int i = 100; i <= 999; i++) {
            int ge = i % 10;
            int shi = i / 10 % 10;
            int bai = i / 100;
            if (i == ge * ge * ge + shi * shi * shi + bai * bai * bai) {
                System.out.println(i + "是水仙花数");
            }
        }
        //五.猜测数字游戏
        Random random = new Random();
        int goal = random.nextInt(100);
        Scanner sc = new Scanner(System.in);
        int guess;
        int count = 0;
        do {
            System.out.println("请输入你猜测的数字");
            guess = sc.nextInt();
            count++;
            if (guess > goal) {
                System.out.println("大了");
            } else if (guess < goal) {
                System.out.println("小了");
            } else {
                System.out.println("猜中了"+"共猜测了"+count+"次");
            }
        } while (guess != goal);
    }
}

