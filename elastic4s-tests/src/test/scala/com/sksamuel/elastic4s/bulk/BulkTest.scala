package com.sksamuel.elastic4s.bulk

import com.sksamuel.elastic4s.http.ElasticDsl
import com.sksamuel.elastic4s.testkit.ResponseConverterImplicits._
import com.sksamuel.elastic4s.testkit.{DualClient, DualElasticSugar}
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.scalatest.{FlatSpec, Matchers}

class BulkTest extends FlatSpec with Matchers with ElasticDsl with DualElasticSugar with DualClient {

  import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._

  override protected def beforeRunTests() = {
    execute {
      createIndex("chemistry").mappings {
        mapping("elements").fields(
          intField("atomicweight").stored(true),
          textField("name").stored(true)
        )
        mapping("molecule").fields(
          textField("name").stored(true)
        ).parent("elements")
      }
    }.await
  }

  "bulk request" should "handle multiple index operations" in {

    execute {
      bulk(
        indexInto("chemistry/elements") fields("atomicweight" -> 2, "name" -> "helium") id 2,
        indexInto("chemistry/elements") fields("atomicweight" -> 4, "name" -> "lithium") id 4,
        indexInto("chemistry/molecule") fields("name" -> "LiH") id 1 parent "4"
      ).refresh(RefreshPolicy.IMMEDIATE)
    }.await.errors shouldBe false

    execute {
      get(2).from("chemistry/elements")
    }.await.found shouldBe true

    execute {
      get(4).from("chemistry/elements")
    }.await.found shouldBe true

    execute {
      get(1).from("chemistry/molecule").parent("4")
    }.await.found shouldBe true
  }

  it should "handle multiple update operations" in {

    execute {
      bulk(
        update(2).in("chemistry/elements") doc("atomicweight" -> 6, "name" -> "carbon"),
        update(4).in("chemistry/elements") doc("atomicweight" -> 8, "name" -> "oxygen"),
        update(1).in("chemistry/molecule") parent "4" doc("name" -> "CO")
      ).refresh(RefreshPolicy.IMMEDIATE)
    }.await.errors shouldBe false

    execute {
      get(2).from("chemistry/elements").storedFields("name")
    }.await.storedField("name").value shouldBe "carbon"

    execute {
      get(4).from("chemistry/elements").storedFields("name")
    }.await.storedField("name").value shouldBe "oxygen"

    execute {
      get(1).from("chemistry/molecule").parent("4").storedFields("name")
    }.await.storedField("name").value shouldBe "CO"
  }

  it should "handle multiple delete operations" in {

    execute {
      bulk(
        delete(2).from("chemistry/elements"),
        delete(4).from("chemistry/elements"),
        delete(1).from("chemistry/molecule").parent("4")
      ).refresh(RefreshPolicy.IMMEDIATE)
    }.await.errors shouldBe false

    execute {
      get(2).from("chemistry/elements")
    }.await.found shouldBe false

    execute {
      get(4).from("chemistry/elements")
    }.await.found shouldBe false

    execute {
      get(1).from("chemistry/molecule").parent("4")
    }.await.found shouldBe false
  }
}
