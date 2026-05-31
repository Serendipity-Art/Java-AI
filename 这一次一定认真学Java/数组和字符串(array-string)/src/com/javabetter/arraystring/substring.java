package com.javabetter.arraystring;

/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/31
 */
public class substring {
    //提取字符串
    public static void main(String[] args) {
        //二.String的substring方法
        //①、提取字符串中的一段子串：
        String str = "Hello, world!";
        String subStr = str.substring(7, 12);  // 从第7个字符（包括）提取到第12个字符（不包括）
        System.out.println(subStr);  // 输出 "world"
        //②、提取字符串中的前缀或后缀：
        String str1 = "Hello, world!";
        String prefix = str1.substring(0, 5);  // 提取前5个字符，即 "Hello"
        String suffix = str1.substring(7); // 提取从第7个字符开始的所有字符，即 "world!"
        //③、处理字符串中的空格和分隔符
        String str2 = "   Hello,   world!  ";
        String trimmed = str2.trim();                  // 去除字符串开头和结尾的空格
        String[] words = trimmed.split("\\s+");  // 将字符串按照空格分隔成单词数组
        String firstWord = words[0].substring(0, 1);  // 提取第一个单词的首字母
        System.out.println(firstWord);                // 输出 "H"
        //④、处理字符串中的数字和符号：
        String str3 = "1234-5678-9012-3456";
        String[] parts = str3.split("-");             // 将字符串按照连字符分隔成四个部分
        String last4Digits = parts[3].substring(1);  // 提取最后一个部分的后三位数字
        System.out.println(last4Digits);
    }
}