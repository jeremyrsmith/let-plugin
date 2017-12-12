# let-plugin

A simple Scala compiler plugin that allows `let` bindings for Scala. This is mainly to demonstrate an implementation in
preparation for a SIP proposing that `let` be added to the language.

Consider:

```scala
def wizzle(a: Int, b: Int) = {
  val temp = foo(a, b)
  bar(temp, baz(temp))
}
```

We had to pass the result of `foo` to two different functions, so we assigned it to a value. This is perfectly fine and
great.

Except, the body of `wizzle` is no longer an *expression*, syntactically speaking. It's now a `Block` (in Scala AST
terms), which is a list of "stats" and a result expression. The fact that it isn't syntactically an expression can be
witnessed by the fact that the curly braces surrounding it cannot be removed.

It's better<sup>[citation needed?]</sup> to use expressions everywhere. Certainly from a functional programming
standpoint, if everything is an expression you can be reasonably assured of purity (until a method is called which
is not an expression).

However, using a `val` doesn't mean it isn't *semantically* an expression, because it can be trivially transformed into
one - even with existing Scala syntax:

```scala
def wizzle(a: Int, b: Int) = foo(a, b) match {
  case temp => bar(temp, baz(temp))
}
```

Here, the braces *have* been eliminated, and we know we have an expression from a syntactic perspective.

But this misappropriation of `match` is misleading - we aren't doing any pattern matching; only naming the result of
`foo` so that it can be referenced multiple times in a nested expression.

This is why a `let` expression would be great:

```scala
def wizzle(a: Int, b: Int) = let (temp = foo(a, b)) in bar(temp, baz(temp))
```

All three versions of `wizzle` are semantically equivalent. But the third example, which is what this plugin enables,
allows `wizzle` to be specified as an expression without a confusing use (misuse?) of `match`.

## Supported

* Parenthetical bindings, comma-separated:
  ```scala
  let (one = expr1, two = expr2) in resultExpr
  ```
* Braced bindings, newline-separated:
  ```scala
  let {
    one = expr1
    two = expr2
  } in resultExpr
  ```

In both cases, the binding expressions are simply desugared to `val` definitions:

```scala
val one = expr1
val two = expr2
resultExpr
```

In the braced form, you can also just throw in `val` definitions (in case you need a `lazy val` or `implicit val` for
example). They'll be desugared as-is.

```scala
let {
  lazy val one = expr1
  two = expr2
} in foo(one, two)  //maybe foo is (=> A, => B) => C
```

## Unsupported

Type annotations to the bindings aren't supported - this won't get through the parser, so a compiler plugin can't do
anything about it:

```scala
let (one: Double = 10) in one + one
```

However, you can use the braced form with explicit `val`s instead. It doesn't save you any syntax, but it preserves
syntactic expression-ness!

## Arguments against

I've already made the argument *for* the `let` construct. Here are some possible arguments against:

* There are already too many things in Scala that can be done different ways. `let` would add another one. (I'd argue
  that `let` would be pretty difficult to make unclear in its usage.)
* It is pointless because it just desugars to the thing you were trying to avoid! (Hey, I said these things are
  *semantically* equivalent; this is about writing it as an expression *syntactically*.)
* Desugaring is already a minefield; don't add more junk to it! (Fair point...)
* Just shut up and use `val`s. What's the big deal? (It's not a big deal. I just think it would lead to code that's
  easier to follow.)


## What I'd really like

This implementation just desugars `let`, because that's all the power you really have in a compiler plugin. But it would
be great (in my humble opinion) for `let` to be a first-class syntactic construct in Scala, that supported these things:

```scala
// lazy binding, without using braces and writing out "lazy val ..."
let (lazy x = foo) in bar(foo)

// type annotations? Maybe? I don't think I'd ever use it,
// but parity with ValDef would be nice
let (x: Double = foo) in bar(foo)

// implicits? Maybe this wouldn't work without type annotation
// given upcoming rule changes
let (implicit x = foo) in bar()
```
  