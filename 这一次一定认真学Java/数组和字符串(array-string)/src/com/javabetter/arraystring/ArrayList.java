package com.javabetter.arraystring;

import java.util.List;

/**
 * 正在努力学习Java的小白
 *
 * @author Serendipity
 * @date 2026/5/28
 */
public class ArrayList {
    public static void main(String[] args) {
        //一.数组的声明
//        int[] anArray = new int[10];
        //数组声明使用了 new 关键字，这就意味着数组的确是一个对象，只有对象的创建才会用到 new 关键字，
        // 基本数据类型是不用的（基本数据的包装类型是可以 new 的，包装类型就是对象）。然后，我们需要在方括号中指定数组的长度。
        //二.数组的访问
        //for循环访问
        int anOtherArray[] = new int[] {1, 2, 3, 4, 5};
        for (int i = 0; i < anOtherArray.length; i++) {
            System.out.println(anOtherArray[i]);
        }
        for (int element : anOtherArray) {
            System.out.println(element);
        }
        //不需要关心索引的话（意味着不需要修改数组的某个元素），使用 for-each 遍历更简洁一些。
        int[] anArray = new int[] {1, 2, 3, 4, 5};
        //数组9大操作核心
//        创建数组 比较数组 数组排序 数组检索 数组转流 打印数组
////        数组转 List setAll（没想好中文名）parallelPrefix（没想好中文名）
//        01、创建数组
//        使用 Arrays 类创建数组可以通过以下三个方法：
//        copyOf，复制指定的数组，截取或用 null 填充
//        copyOfRange，复制指定范围内的数组到一个新的数组
//        fill，对数组进行填充
//        (1)copyOf
        




    }

}
