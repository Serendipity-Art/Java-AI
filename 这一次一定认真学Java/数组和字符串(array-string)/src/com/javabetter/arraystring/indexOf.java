package com.javabetter.arraystring;

/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/31
 */
public class indexOf {
    public static void main(String[] args) {
        //①、示例 1：查找子字符串的位置
        String str1 = "Hello, world!";
        int index1 = str1.indexOf("world");  // 查找 "world" 子字符串在 str 中第一次出现的位置
        System.out.println(index1);
        //②、示例 2：查找字符串中某个字符的位置
        String str2 = "Hello, world!";
        int index2 = str2.indexOf(",");     // 查找逗号在 str 中第一次出现的位置
        System.out.println(index2);        // 输出 5// 输出 7
        String str3 = "Hello, world!";
        //③、示例 3：查找子字符串的位置（从指定位置开始查找
        int index3 = str3.indexOf("l", 3);  // 从索引为3的位置开始查找 "l" 子字符串在 str 中第一次出现的位置
        System.out.println(index3);// 输出 3
//        String 类的其他方法
//        ①、比如说 length() 用于返回字符串长度。
//        ②、比如说 isEmpty() 用于判断字符串是否为空。
//        ③、比如说 charAt() 用于返回指定索引处的字符。
//        ④、比如说 valueOf() 用于将其他类型的数据转换为字符串。
    }
}
