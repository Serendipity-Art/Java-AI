package com.javabetter.arraystring;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/29
 */
public class PrintArray {
    public static void main(String[] args) {
        //一.打印数组
        String [] cmowers = {"沉默","王二","一枚有趣的程序员"};
        System.out.println(cmowers);//打印数组的地址
        //1.使用流打印数组
        Arrays.asList(cmowers).stream().forEach(s -> System.out.println(s));
        Stream.of(cmowers).forEach(System.out::println);
        Arrays.stream(cmowers).forEach(System.out::println);
        //2.for循环打印数组
        for(int i = 0; i < cmowers.length; i++){
            System.out.println(cmowers[i]);
        }
        for (String s : cmowers) {
            System.out.println(s);
        }
        //3.最佳打印方式,toString打印
        System.out.println(Arrays.toString(cmowers));
        String[][] deepArray = new String[][] {{"沉默", "王二"}, {"一枚有趣的程序员"}};
        System.out.println(Arrays.deepToString(deepArray));
    }
}
