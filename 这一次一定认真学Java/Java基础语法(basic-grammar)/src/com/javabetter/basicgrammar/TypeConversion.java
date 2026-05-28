package com.javabetter.basicgrammar;

/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/28
 */
public class TypeConversion {
    //    byte -> short -> int -> long -> float -> double
//    char -> int -> long -> float -> double
    public static void main(String[] args) {
        //一.自动类型转换
        int intValue = 5;
        double doubleValue = 2.5f;
        double result=intValue*doubleValue;
        System.out.println("结果"+result);
        byte b=50;
        //b=b*2;会报错,类型不匹配,无法从int转化为byte
        //b*=2,不会报错,自动进行了强制类型转换
        System.out.println(b*=2);
//        char 类型比较特殊，char 自动转换成 int、long、float 和 double，
//        但 byte 和 short 不能自动转换为 char，而且 char 也不能自动转换为 byte 或 short
        //二.强制类型转换
        double doubleValue1 = 42.8;
// 强制类型转换：将 double 类型转换为 int 类型
        int intValue1 = (int) doubleValue1;
        System.out.println("整数值: " + intValue1); // 输出：整数值: 42
        int a = 1500000000, c = 1500000000;
        int sum = a + c;
        long sum1 = a + c;
        long sum2 = (long)a + c;
        long sum3 = (long)(a + c);
//        int sum = a + b，a 和 b 都是 int 类型，所以 a+b 的结果也是 int 类型，
//        但是 a+b 的结果超出了 int 类型的取值范围，所以会出现溢出的情况。
//        long sum1 = a + b，a 和 b 都是 int 类型，于是 a+b 的和也是 int 类型，但超出了 int 的取值范围，
//        所以会出现溢出的情况；如果 a+b 的和没有超出 int 取值范围，其实会将 a+b 的结果隐式转换为 long 类型。
//        long sum2 = (long)a + b，a 是 int 类型，但是 (long)a 将 a 强转为了 long 类型，然后再和 b
//        相加，此时 b 将隐式提升为 long 型，于是等式右边的结果也是 long 型，而 3000000000 并没有超出 long 型的取值范围。
//        long sum3 = (long)(a + b)，a 和 b 都是 int 类型，a+b 的结果也是 int 类型，但超出了
//        int 的取值范围，所以会出现溢出的情况；即便是外面有一层 long 的强转，但还没有来得及强转，
//        a+b 的结果已经溢出了，所以强转也没用。
        System.out.println(sum);
        System.out.println(sum1);
        System.out.println(sum2);
        System.out.println(sum3);
    }
}
