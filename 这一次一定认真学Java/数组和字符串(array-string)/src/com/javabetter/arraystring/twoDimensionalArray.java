package com.javabetter.arraystring;

import java.util.Scanner;

/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/29
 */
class YangHuiTriangle{
    public static void printYangHuiTriangle(int n) {
        int[][] triangle = new int[n][n];
        for (int i = 0; i < n; i++) {
            triangle[i][0] = 1;
            triangle[i][i] = 1;
            for (int j = 1; j < i; j++) {
                triangle[i][j] = triangle[i - 1][j - 1] + triangle[i - 1][j];
            }
        }
        //打印杨辉三角
        for (int i = 0; i < n; i++) {
            for(int k = 0; k<n-i-1; k++){
                System.out.print("  ");
            }
            for (int j = 0; j <= i; j++) {
                System.out.printf(" %2d ",triangle[i][j]);
            }
            System.out.println();
        }
    }
}
public class twoDimensionalArray{
    public static void main(String[] args) {
        //一.二维数组
        //1.打印杨辉三角
        Scanner sc = new Scanner(System.in);
        System.out.println("请输入你要打印的行数");
        int n = sc.nextInt();
        YangHuiTriangle.printYangHuiTriangle(n);
    }







}
