package com.javabetter.basicgrammar;
import java.util.Arrays;
/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/26
 */
public class RefDataType {
    //引用数据类型String
    private String a;
    static String b;
    public static void main(String[] args) {
        RefDataType ref = new RefDataType();
        System.out.println(ref.a);
        System.out.println(b);
        //数组(引用类型)
        int [] arrays={1,2,3};
        System.out.println(Arrays.toString(arrays));
        //接口(引用类型)
//        基本数据类型：
//        1、变量名指向具体的数值。
//        2、基本数据类型存储在栈上。
//        引用数据类型：
//        1、变量名指向的是存储对象的内存地址，在栈上。
//        2、内存地址指向的对象存储在堆上。
    }
}
