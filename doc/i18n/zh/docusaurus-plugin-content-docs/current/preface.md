---
sidebar_position: 1
title: 前言
---

## 1. 本文的讨论前提

OLTP类型项目很大一部分操作是都针对数据库原始数据，这时软件系统中的对象结构和数据库的中数据结构大体一致，是本文讨论的场景。

而因业务计算而引入的计算指标相关的数据类型，和数据库的原始结构并不相同，并非本文的讨论范场景。

## 2. 现有技术流派的缺陷

现在，用户访问关系型数据库的框架很多，总体上分为两个派别

- 传统ORM派，JPA是知名度最高的代表。
- DTO Mapper派，MyBatis是知名度最高的代表。

### 2.1. 以JPA为代表的传统ORM派

在传统ORM中，开发人员创建实体类，和数据库表结构直接对应。从映射的角度讲，非常简单。

传统ORM注重维护对象之间的关系，以JPA为例
```java
List<Book> books = entityManager
    .createQuery(
        "select book from Book book " +
        "left join fetch book.store " +
        "left join fetch book.authors",
        Book.class
    ).getResultList();
```
这个例子中的`join fetch`是JPA的一个特色功能，可以利用`SQL JOIN`使返回的`Book`对象不再是孤单对象，而是附带了关联属性`store`和`authors`。

通过可选的`join fetch`*（或其他技巧，不同的ORM框架手段不尽相同）*，传统ORM既可以返回孤单的数据对象，也可以返回带关联的复杂对象，这其实一种对返回数据结构的裁剪能力。

这种裁切能力是以对象为粒度的，但是，返回的数据结构中每个对象都是完整的，也就是说缺少普通属性级别的裁剪能力。

无法做到普通属性级的裁剪，当对象属性很多导致查询所有列效率很低，或需要对低权限用户进行重要属性脱敏时，会成为问题。很不幸，现实中的项目就是这样的。

:::note
虽然Hibernate从3.x开始，普通（非关联）属性也可以被设置为lazy。然而，这个特性是为lob属性而设计，并非为了实现普通属性级的裁剪而设计，灵活度非常有限。不予讨论
:::

如果想要让传统ORM精确地实施属性级的裁切，会使用这样的代码

```java
List<BookDTO> bookDTOs = entityManager
    .createQuery(
	    "select new BookDTO(book.id, book.name) " +
        "from Book book",
        BookDTO.class
    ).getResultList();
```

在这个例子中，我们只想查询id和name属性，为此，不得不构建一个全新的类型`BookDTO`用作只有两个属性的残缺对象的载体。在我们获得普通属性级裁剪能力的同时，因`BookDTO`是一个普通对象而非实体对象，丧失了对象级的裁剪能力。

:::note
也正是因为这种用法丧失了ORM的核心能力，在传统ORM中实践中属于非主流用法，很少使用。
:::

传统ORM的另外一个问题是，返回的数据复杂度很高，难以直接使用。

对于未加载的lazy属性，开发人员很容易在Json序列化中忽略他们，这不是问题。

真正麻烦的是对象之间存在双向关联，而前端和微服务客户端更期望看到只有单向关联的对象树。

比如TreeNode实体同时具备向上的`parent`属性和向下的`childNodes`属性。

- 有些业务可以需要查询某个节点和其所有下级，返回`aggregateRoot->childNodes->childNodes->...`这样的数据结构；
- 而有些业务查询某个节点和其所有上级，返回`aggregateRoot->parent->parent->...`这样的数据结构。

所以，你无法简单地规定`parent`和`childNodes`中，哪个是对外暴露的，哪个是对外隐藏的。你无法简单地通过`@JsonIgnore`注解来解决这个问题，这是一个非常棘手的问题。

### 2.2. 以MyBatis为代表的DTO Mapper派

通过上文描述，我们知道，传统ORM有两个缺点。

1. 便于发挥传统ORM能力的主流方法，虽然有灵活的对象级裁剪能力，但同时也丧失了普通属性级的裁剪能力。
2. ORM返回的实体对象过于复杂，难以直接返回，无法和HTTP交互。

这两个问题，都是数据对象表达能力弱导致的，其实可以通过定义特定业务所需的DTO类解决。

既然人们注定需要定义特定业务相关的DTO类型，为什么还要编写代码把ORM实体转换为DTO呢？为什么不直接实现从SQL结果到DTO的映射呢？

因此DTO Mapper派被开发人员认同，这个流派提出了截然不同的解决方案。开发人员不再定义和数据库结构直接对应的实体类，而是直接为每个特定业务定义DTO类型，比如：

- 为表达孤单的Book对象，新建类`Book`
- 为表达带关联属性`store`的Book对象，新建类`BookWithStore`
- 为表达带关联属性`authors`的Book对象，新建类`BookWithAuthors`
- 为表达带关联属性`store`和`authors`的Book对象，新建类`BookWithStoreAndAuthors`

各业务API返回自己需要的DTO对象，每个API都是用特定的SqlResultMapper，把特定的查询结果映射为特定的DTO。

然而，这个做法同样问题严重

1. 上面的例子中我们只展示了对象级的裁剪，并未展示属性级的裁剪，而且对象树的深度也很浅。如果不是这样，DTO类型的数量会激增，甚至可以用**爆炸**来形容。这时，DTO类会多得连取名字都难。开发人员甚至需要结合行业相关的命名约定来避免很长的类名。

2. DTO太多了，不同的DTO虽然不同，但相同部分也不少，具有高度的冗余。系统丧失紧凑性，开发成本和测试成本激增。

3. 一旦引入新的需求，数据库的结构发生变化，多处冗余的业务都需要修改。

为避免问题2和3，可对SQL映射片段或业务代码尽可能重用，但这会破坏系统的简单性，代码变得难以理解，这是过度使用低价值复用的必然代价。

## 3. Jimmer的优势

通过上面的论述，我们知道

- 传统ORM派：优点是直接和数据存储结构对应，提供统一视角；但缺点是只对返回数据格式进行对象级裁剪，没有普通属性级的裁剪，而且返回的数据结构难以直接利用。
- DTO Mapper派：优点是查询的到的DTO对象简单，返回的聚合根所代表的数据结构只包含单向关联；但缺点是DTO类型数量膨胀严重，虽不同但相似，开发成本和测试成本都很高。

Jimmer完美融合两派之长，走出了截然不同的第三条路。因此并不能比把Jimmer上述两个派中的任何方案做简单对比。

### 3.1. 无DTO模式：动态实体

在Jimmer中

-   实体对象是动态的，任何对象属性，无论是普通属性还是关联属性，都可以缺失。
    :::info
    对Jimmer的实体对象而言，不指定某个属性和把某个属性指定为null，完全是两码事。
    :::

-   在Java或Kotlin代码中直接读取对象的缺失属性会导致异常；然而，在JSON序列化时，缺失属性会被自动忽略，不会异常。

-   虽然声明实体类型时，不同类型之间可以定义双向关联；然而，某个具体业务需要实例化对象时，实体对象之间只能建立单向关联，保证任何数据结构都能用一个简单的聚合根对象来表达。

:::tip
动态实体本身不是DTO，但它具备DTO对象的所有特质，无DTO胜似DTO，任何实体对象树都可以直接参与HTTP交互。

动态实体是整个ORM的架构基础。
:::

### 3.2. 查询任意复杂的数据结构

完美支持对象级别和属性级别的对象形状裁切能力，用户可以从完整的关系模型中圈定出一个局部数据结构，即一个任意复杂的树结构，以返回动态实体树的方式，查询整个数据结构。

:::tip
让RDBMS具备类似于GraphQL功能。即使你的项目和GraphQL技术毫无关系，你的RDMBS也拥有它的一切优势。

Jimmer比GraphQL做得更好，它甚至支持自关联属性的递归查询。
:::

### 3.3. 修改任意复杂的数据结构

用户可以向Jimmer传递任意复杂的动态对象树，将整棵树作为一个整体用一句话保存。

:::tip
可以理解成GraphQL的逆功能。
:::

### 3.3. 强大的缓存机制

- 对用户的缓存技术选型不做任何限制，用户可以选用任何缓存技术。
- 内部支持对象缓存和关联缓存，在复杂数据结构查询中，二者在幕后按需有机结合。最终给用户呈现出的效果，就是任意复杂数据结构的缓存，而非简单对象的缓存。
- 自动保证缓存的数据一致性，只要在接受到数据库binlog推送后简单调用Jimmer的API即可。
- 缓存机制对开发人员100%透明，是否采用缓存，对业务代码没有任何影响。

:::tip
虽然RDBMS具备无以伦比的表达能力，但它有一个明显的缺点：按关系导航追踪其它数据，性能不理想。

关联缓存可以在很大程度上缓解这个问题，让RDBMS如虎添翼。
:::

### 3.4 比原生SQL更实用的强类型SQL DSL
- 在编译时发现拼写错误和类型匹配错误。
- 强类型SQL DSL可以原生SQL表达式随意混合，在统一和抽象不同的数据库的同时，允许发挥特定数据库产品独有的能力。
- 以放弃实际项目中几乎不可能被用到的个别SQL写法为代价，提供比原生SQL更便捷更实用的多表连接操作。


