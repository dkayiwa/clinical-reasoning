package org.opencds.cqf.fhir.utility.monad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

public class EitherTest {

  @Test
  public void equals() {
    var first = Eithers.forLeft(1);
    var second = Eithers.forLeft(1);
    assertEquals(first, second);
    assertEquals(second, first);

    var third = Eithers.forRight(2);
    var fourth = Eithers.forRight(2);
    assertEquals(third, fourth);
    assertEquals(fourth, third);

    var fifth = Eithers.forLeft(1);
    var sixth = Eithers.forRight(1);
    assertNotEquals(fifth, sixth);
    assertNotEquals(sixth, fifth);
  }

  @Test
  public void properties() {
    var one = Eithers.forLeft(1);
    assertTrue(one.isLeft());
    assertFalse(one.isRight());
    assertEquals(1, one.left());
    // Either is right-biased, meaning the "get" is for the right element
    assertThrows(IllegalStateException.class, one::right);
    assertThrows(IllegalStateException.class, one::get);

    var two = Eithers.forRight(1);
    assertTrue(two.isRight());
    assertFalse(two.isLeft());
    assertEquals(1, two.right());
    assertEquals(1, two.get());
    assertThrows(IllegalStateException.class, two::left);
  }

  @Test
  public void swap() {
    var one = Eithers.forLeft(1);
    var two = Eithers.forRight(1);
    assertEquals(one, two.swap());
    assertEquals(two, one.swap());
  }

  // The law states that applying the right identity function
  // (Eithers.forRight) should be equivalent to the original either.
  // In other words, reconstructing an Either from a value
  // should equal the original eithers
  @Test
  void rightIdentity() {
    Integer b = 42;

    Either<String, Integer> right = Eithers.forRight(b);

    assertEquals(right.flatMap(Eithers::forRight), right);
  }

  // The law states that applying flatMap to a "left" either should return the
  // original either. In other words, the left value propagates
  @Test
  void leftIdentity() {
    String a = "42";

    Either<String, Integer> left = Eithers.forLeft(a);

    assertEquals(left.flatMap(x -> Eithers.forRight(x + 2)), left);
  }

  // This law states that if you chain together two functions using flatMap,
  // the order in which you apply them should not matter.
  @Test
  void associativity() {
    Function<Integer, Either<String, Double>> f = i -> Eithers.forRight(i / 2.0);
    Function<Double, Either<String, String>> g = d -> Eithers.forRight("Result: " + d);

    Either<String, Integer> m = Eithers.forRight(10);

    assertEquals(m.flatMap(f).flatMap(g), m.flatMap(x -> f.apply(x).flatMap(g)));
  }

  // Map is implemented in terms of "unit" (constructors in Java) and "bind" ("flatMap")
  // IOW, bind(m, x -> unit(f(x)))
  @Test
  void map() {
    Function<Integer, Integer> doubleInt = (Integer x) -> x * 2;
    Either<String, Integer> e = Eithers.forRight(3);

    var bound = e.flatMap(x -> Eithers.forRight(doubleInt.apply(x)));
    var map = e.map(doubleInt);

    assertEquals(bound, map);
    assertEquals(6, map.get());

    // But we also need to make sure we preserve left identity;
    Either<String, Integer> failed = Eithers.forLeft("Failed either");
    var mapFailed = failed.map(doubleInt);

    assertEquals(failed, mapFailed);
  }

  // fold is a reduction operation. Take a left or right and turn it into
  // a single value.
  @Test
  void fold() {
    var right = Eithers.<String, Integer>forRight(2);
    var left = Eithers.<String, Integer>forLeft("three");

    Function<Integer, String> foldR = x -> x.toString();
    Function<String, String> foldL = x -> x;

    var x = right.fold(foldL, foldR);
    assertEquals("2", x);

    var y = left.fold(foldL, foldR);
    assertEquals("three", y);
  }

  @Test
  void forEach() {
    var left = Eithers.<String, Integer>forLeft("three");
    var right = Eithers.<String, Integer>forRight(2);

    boolean[] flags = {false, false};

    right.forEach(x -> {
      flags[1] = true;
    });
    assertTrue(flags[1]);

    // Left for each shouldn't do anything
    left.forEach(x -> {
      flags[0] = true;
    });
    assertFalse(flags[0]);
  }

  // "stream" gives you the ability to use the standard Java Stream apis if you need to
  // left Eithers return an empty stream. Right Eithers return a stream of one.
  @Test
  void stream() {
    var left = Eithers.forLeft(1);
    var right = Eithers.forRight(1);

    assertEquals(0, left.stream().count());
    assertEquals(1, right.stream().count());
  }

  // "optional" gives you the ability to use the standard Java Optional apis if you need to
  // left Eithers return an empty optional. Right Eithers return a filled optional
  @Test
  void optional() {
    var left = Eithers.forLeft(1);
    var right = Eithers.forRight(1);

    assertFalse(left.optional().isPresent());
    assertTrue(right.optional().isPresent());
  }
}
