1.面向对象

面向对象是一种编程思维,Java就是面向对象编程语言

万物皆是对象,我们可以把Java程序看作是多个系统对象系统构建,Java将参与程序运行的所有角色看作是一个个对象

## 1.1 什么是面向对象

面向过程:结构化编程方式

面向过程注重的每一个过程,面向对象关注的是整个事件的模块化结构

面向对象的核心思想是重复性和灵活性(灵活性=可扩展性+变化性)

重复性是已经写好的某个程序,可以在其他业务场景中反复使用

## 1.2 类和对象

1.属性:对象的静态特征

2.方法:对象的动态特征

对象就是描述客观存在的实体,该实体由一组属性和方法组成

类就是产生对象的模板

对象和类的关系:类是对象的抽象化描述,对象是类的具体实例

## 1.3 构造函数

类是用来创造对象如何创建?通过构造函数来创建

构造函数就是类中的一个方法,只能用来创造对象

构造函数,构造器,构造方法

每个类都有一个默认的无参构造函数

当类里面有一个有参构造函数时候,默认的无参构造函数就会被覆盖

## 1.4 如何创建对象

创建对象只要调用对应类的构造函数即可,构造函数分为有参和无参构造

通过无参对象构造对象时,需要手动给对象赋值

可以有参构造对象,创建和赋值一起完成

对修改关闭,对扩展开放

## 1.5 使用对象

使用对象包括获取和修改属性,以及调用方法,访问属性,对象名,属性名来完成,调用方法通过 对象名.方法名(参数列表)来完成

## 1.6 this关键字

this指当前对象,每一次的this都不一样

## 1.7 方法重载

方法重载是Java代码复用的重要方式

1.在同一个类中

2.方法名相同

3.参数列表不同(个数或数据类型不同)

4.与方法返回值和访问权限无关

## 1.8 成员变量和局部变量

变量作用域:通过变量名来访问变量的范围

变量的作用域由变量被声明时所在的位置来决定

成员变量和局部变量

如果一个变量声明在方法中,则该变量是局部变量

如果一个变量声明在方法外,则该变量是成员变量

成员变量的作用域在整个类中,局部变量的作用域只在该方法中,出了方法就无法访问

# 2. 封装

面向对象三大特性:封装,继承,多态

封装就是将类的属性隐藏在内部,外部不能直接访问和修改,必须通过类提供的方法来完成对对象属性的访问和修改

封装的核心思想就是尽可能把属性隐藏在内部,对外提供方法来访问,我们可以在这些方法中添加逻辑处理来实现过滤,以此来屏蔽错误的数据赋值

## 2.1 封装的步骤



1.修改访问权限,使得外部不能直接访问

2.提供外部可以直接访问的方法

3.在该方法中加入属性和逻辑控制

## 2.2 static关键字

static表示静态或全局,可以用来修饰成员变量和成员方法以及代码块

成员变量和成员方法的方法必须依赖于对象

使用static修饰的成员变量和成员方法独立于该类的任何一个实例对象,访问时不需要依赖于对象,可以直接通过类来访问

可以被理解为被该类的所有实例对象共用

static 修饰的变量和方法叫类(静态)变量和类(静态)方法

都可以直接通过类来访问

static修饰的静态变量在内存中只有一份,多个对象共享

static除了可以修饰成员变量和成员方法之外,还可以修饰代码块

静态代码块只执行一次,当该类被加载到内存里面自动执行,不需要手动调用

程序运行时,首先会将需要的类加入到内存中,并且只加载一次

然后通过这个类来创建多个对象完成具体的业务逻辑

如果有多个静态代码块,则按照先后顺序执行,类的构造方法用于初始化类的实例,类的静态代码块用于初始化类,给类的变量赋值

# 3. 继承

## 3.1 子类访问父类



创建子类的时候,会默认优先构建父类对象,无论调用的是子类无参还是有参构造,创建父类的时候都是调用无参构造.

如果想调用有参,需要用到super()关键字

```java
public class Teacher {
    private int id;
    private String name;
    private int age;

    public Teacher(String name) {
        this.name = name;
        System.out.println("调用了有参构造创建Teacher对象");
    }

    public Teacher() {
        System.out.println("调用无参构造创建Teacher对象");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
```

```java
public class Student extends Teacher {
    public Student() {
        super("张三");
        System.out.println("调用无参构造创建Student对象");
    }

    public Student(int id) {
        super("李四");
        System.out.println("调用了有参构造来创建Student对象");
    }
}
```

 

## 3.2 子类的访问权限

访问权限修饰符,可以修饰类,属性,方法,不同访问权限表示不同作用域,包括public,protected,默认友好,private

| 修饰符    | 同一个类 | 同一个包   | 不同包     | 子类       |
| --------- | -------- | ---------- | ---------- | ---------- |
| public    | 可以访问 | 可以访问   | 可以访问   | 可以访问   |
| protected | 可以访问 | 可以访问   | 不可以访问 | 可以访问   |
| 默认友好  | 可以访问 | 可以访问   | 不可以访问 | 不可以访问 |
| private   | 可以访问 | 不可以访问 | 不可以访问 | 不可以访问 |

1.创建子类对象会默认调用父类无参构造,子类构造方法默认使用无参super()

2.子类调用父类的共有方法

## 3.3 方法重写

子类在继承父类方法的基础上,对父类方法重新定义并覆盖的操作叫方法重写

通过重写的方式可以实现子类完成特定需求的功能,构造函数不能被重写

方法重写规则:

1.父子类方法名相同

2.父子类方法参数列表相同

3.子类方法和父类方法返回值类型相同或是其子类(父类Object,子类就可以是String/Integer)

4.子类方法的访问权限不能低于父类

public>protected>默认>private

重写的前提是继承,private修饰的方法不能被继承

## 3.4 方法重写和方法重载的区别

| 类型     | 所在位置 | 方法名 | 参数列表 | 返回值       | 访问权限       |
| -------- | -------- | ------ | -------- | ------------ | -------------- |
| 方法重写 | 子类     | 相同   | 相同     | 相同或是子类 | 不能或小于父类 |
| 方法重载 | 同一个类 | 相同   | 不同     | 没有要求     | 没有要求       |

# 4. 多态

多态概念本身比较抽象

定义一个方法,在具体的生产环境中根据不同的需求呈现出不同的业务逻辑

```java
public class Cashier {
    private Member member;

    public Member getOrdinaryMember() {
        return member;
    }
    public void setMember(Member member) {
        this.member = member;
    }
    public void settlement(){
        this.member.buybook();
    }
}
```

```java
public class Member {
    public void buybook(){}
}
```

```java
public class OrdinaryMember extends Member {
    public void buybook(){
        System.out.println("普通会员买书打9折");
    }
}
```

```java
public class SuperMember extends Member{
    public void buybook(){
        System.out.println("超级会员买书打6折");
    }
}
```

```java
public class Test {
    public static void main(String[] args) {
        OrdinaryMember Member1 = new OrdinaryMember();
        SuperMember Member2 = new SuperMember();
        Cashier cashier = new Cashier();
        cashier.setMember(Member1);
        cashier.settlement();
        cashier.setMember(Member2);
        cashier.settlement();
    }
}
```

## 4.1 多态的使用

在实际开发中,多态主要有两种表现形式,一种是定义方法时形参为父类,调用方法时传入的参数为子类对象;另外一种是定义方法时返回值的数据类型为父类,调用方法时返回子类对象

1.定义方法时形参为父类

```java
    public void settlement(Member member){
        member.buybook();
    }
```

```java
{
    public static void main(String[] args) {
        OrdinaryMember Member1 = new OrdinaryMember();
        SuperMember Member2 = new SuperMember();
        Cashier cashier = new Cashier();
        cashier.setMember(Member1);
        cashier.settlement();
        cashier.setMember(Member2);
        cashier.settlement();
    }
}
```

2.定义方法返回值的数据类型为父类,调用方法时返回子类对象

```java
   public Member getMember(String username){
        if(username.equals("ordinaryMember")){
            return new OrdinaryMember();
        }
        else {
            return new SuperMember();
        }
   }
```

```java
        System.out.println(cashier.getMember("ordinaryMember"));
        System.out.println(cashier.getMember("SuperMember"));
```

## 4.2抽象方法和抽象类

父类

```java
public class Member {
    public void buybook(){}
}
```

子类

```java
public class OrdinaryMember extends Member {
    public void buybook(){
        System.out.println("普通会员买书打9折");
    }
}
```

buyBook方法只需要声明,不需要具体实现,没有方法体的方法叫抽象方法,抽象方法需要添加abstract关键字

一旦一个类中有抽象方法,则该类必须被声明为抽象类

```java
public abstract class Member {
    public abstract void buybook(){}
}
```

抽象类不能被实例化,抽象方法没有方法体.

抽象类中可以没有抽象方法,但包含抽象方法的类一定为抽象类

# 5. 面向对象高级部分

## 5.1 Object类

Object类是JDK中提供的一个类,位于java.lang包中,所有的类都是Object的子类

## 5,2 重写Obiect类方法

| 方法                              | 描述                                   |
| --------------------------------- | -------------------------------------- |
| public String toString()          | 以字符串的形式返回该类的实例化对象信息 |
| public Boolean equals(Obiect obi) | 判断两个对象是否相等                   |
| public native int hashcode()      | 返回对象的散列码                       |

toString

```java
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }
```

```java
    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
```

equals

```java
    public boolean equals(Object obj) {
        return (this == obj);
    }
```

```java
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof String) {
            String anotherString = (String)anObject;
            int n = value.length;
            if (n == anotherString.value.length) {
                char v1[] = value;
                char v2[] = anotherString.value;
                int i = 0;
                while (n-- != 0) {
                    if (v1[i] != v2[i])
                        return false;
                    i++;
                }
                return true;
            }
        }
        return false;
    }
```

hashcode

```java
punlic native int hashCode()
```

native修饰的方法叫本地方法,通过其他编程语言来实现该方法,通过C++来实现

hashCode返回对象的散列值,由对象的内存地址和内存信息综合得出

```java
    public int hashCode() {
        return 1;
    }
```



































