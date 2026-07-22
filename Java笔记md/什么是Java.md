# 1 环境

IDEA Java Web Maven Spring Boot 插件

# 2.Java核心基础

## 2.1 变量

> 与之对应的是final常量

final 可以修饰变量,类,方法

修饰变量表示值不可以修改

修饰方法不能被重写

修饰类不能被继承

> JMM  Java Memory Model Java 内存模型

int num = 1

硬盘存永久数据 CPU负责运算 内存存放运行数据(瞬时态)

Java内存模型大致分为两部分,栈内存和堆内存,Java中所有数据(变量)都是存储在栈内存和堆内存中

基本数据类型存储在堆内存,引用类型数据存储在栈内存和堆内存

通过内存地址寻找具体位置取值,内存地址是16进制随机数值

变量名的作用实际上是内存地址的简写,方便记忆和使用

根据数据类型来决定分配内存空间大小

1个byte=8个bite(8个0和1数字)

int 4个byte

long 8个byte

```java
getClass().getName() + "@" + Integer.toHexString(hashCode());
```

## 2,2基本数据类型

1 byte 

1kb=1024(2^10)byte

1mb-1024kb

1gb=1024mb

1tb=1024gb

1byte 8 位二进制数

01010101

| 基本数据类型 | 占用空间            |
| ------------ | :------------------ |
| byte         | 1个字节 8位二进制位 |
| short        | 2个字节             |
| int          | 4个字节             |
| long         | 8个字节             |
| float        | 4个字节             |
| double       | 8个字节             |
| char         | 2个字节             |
| boolean      | 1/8字节 1位(bit)    |

## 2.3数据类型转换

### 2.3.1自动类型转换

```java
int num=100;
long num1=num;
```

从小到大

### 2.3.2强制类型转换

```java
long num=100;
int num1=(int)num;
```

## 3.1逻辑运算符

### 3.1.1逻辑运算符

只能用于boolean类型的数据运算,判断boolean数据之间的逻辑关系,包括与,或,非3种关系

运算符包括 &,|, &&,||,!

> A&B 全为true为true,否则为false
>
> A|B 有一个为true为true,否则为false
>
> A&&B A和B全为true才为true,否则为false
>
> A||B 当A或者B为true,为true,否则为false
>
> !A 如果A为true,结果为false

运算效率&&和||效率更高

```java
int num1=10;
int num2=11;
System.out.println((num1++==num2)&(++num1==num2));
System.out.println(num1);
输出 false和12
```

```java
int num1=10;    
int num2=11;    
System.out.println((num1++==num2)&&(++num1==num2));    
System.out.println(num1);
输出 false和11
```

&&执行效率高于&,判断num1++=num2,直接返回false,短路运算符,输出num1被num2赋值为11

### 3.1.2位运算符

位运算符以二进制进行运算

十进制转二进制,目标数字除以2,除尽记作0,否则记作1,之后继续出2

10转换2进制 1010

17转换2进制  10001

位运算符包括&(按位与),|(按位或),^(按位异或),<<左移,>>右移

> A&B 每一位数字一一对应,若都为1,则该位记作1,否则记作0
>
> A|B 每一位数字一一对应,只有有一个为1,则记作1,否则记作0
>
> A^B 每一位数字一一对应,相同记作0,不同记作1
>
> A<<B A乘以2的B次方
>
> A>>B A除以2的B次方

10&5,10|5,10^3,2<<3,2>>3

10&5 =0

1010(10)

  101(5)位数不够往前补0,不改变值

10|5 =15 (1111)

10^3=9(1001)

1010

0011

2<<3=2*2^3=16

2>>3=2/2^3=0(整除为0)

## 4.1流程控制符

switch-case(只能做等于判断,无法比较大小关系),支持数据类型包括int,short,byte,枚举,String,不支持boolean

```java
switch(变量){
	case 值1;
		代码1;
	break;
    case 值2:
        代码2;
    break;	
}
```

使用switch-case注意添加break防止击穿

# 5.循环

for,while,do-while,foreach

四种循环

循环四要素

>初始化循环变量
>
>循环条件
>
>循环体
>
>更新循环变量

while和do-while循环的区别

while先判断再执行,do-while先执行再判断(至少执行一次)

foreach对集合快速遍历的方法

双重循环

一个循环的循环体是另一个循环

```java
for(int i = 1; i <= 9; i++){   
    for(int j = 1; j <= i; j++){        
        System.out.print(j+"*"+i+"="+i*j+"\t");    }    
    System.out.println();}
```

双重循环打印99乘法表

终止循环(break和continue)

break跳出整个循环

continue跳出当前循环,执行下一次循环

```java
int sum=0;
for (int i = 0; i <= 200; i++) 
	{    if(i%2==0)
		{  continue; }    
 			sum+=i;}
System.out.println(sum);
```

# 6.数组

数组是一种可以存放大量相同数据类型的数据结构,是一个具有相同数据类型的数据集合

## 6.1数组的基本要素

1.数组名称

2.数组元素

3.元素下标

4.数据类型

数组中的每一个元素都有一个对应的下标,并且下标从0开始,相当于编号,数组中所有元素类型相同

## 6.2如何声明数组

1.声明数组

2.分配内存空间

3.给数组赋值

4.使用数组

数组在创建时候必须指定长度

```java
public static void main(String[] args) {    
    //开辟数组    
    int [] array;   
    //分配内存空间   
    array = new int[5];    
    array[0] = 1;    
    array[1] = 2;    
    array[2] = 3;    
    array[3] = 4;    
    for (int i = 0; i < 4; i++) 
    {        System.out.println(array[i]);    
    }
```

## 6.3数组常见错误

1.数组声明数据类型不匹配

数组中能否存入int和String类型的数据?可以,但需要利用多态,将数组命名为object类

2.边声明边赋值必须写在同一行

3.数组下标越界

通过下标取值,需要注意取值时给出的下标不能超出数组的长度,长度范围:数组长度

## 6.4数组常用操作

1.求数组中的最大值

2.求数组中的最小值

3.在数组的指定位置插入一个数据

4.数组排序

```java
public static void main(String[] args) {
    int []array={23,45,67,32,12};int max=array[0];
    for (int i = 1; i < array.length; i++) { 
        if(array[i]>max){           
            max=array[i];       
        }    
    }    System.out.println(max);}
```

```java
    public static void main(String[] args) {
        int[] array = {23, 45, 67, 32, 12};
//将83插入下标为3的位置    
        int[] arraylist = new int[array.length + 1];
        for (int i = 0; i < 3; i++) {
            arraylist[i] = array[i];
        }
        arraylist[3] = 83;
        for (int i = 4; i < arraylist.length; i++) {
            arraylist[i] = array[i - 1];
        }
        for (int i = 0; i < arraylist.length; i++) {
            System.out.println(arraylist[i]);
        }
    }
```

数组排序(冒泡排序)

```java
        int[] array = {23, 45, 67, 32, 12};
        //冒泡排序
        for (int j = 0; j < array.length - 1; j++) {
            for (int i = 0; i < array.length - 1-j; i++) {
                if (array[i] > array[i + 1]) {
                    int temp = array[i];
                    array[i] = array[i + 1];
                    array[i + 1] = temp;
                }
            }
        }
    System.out.println(Arrays.toString(array));
    }
```