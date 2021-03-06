package com.sksamuel.elastic4s.searches.queries.geo

import org.elasticsearch.index.query.{GeoBoundingBoxQueryBuilder, GeoExecType, GeoValidationMethod, QueryBuilders}

object GeoBoundingBoxQueryBuilder {
  def apply(q: GeoBoundingBoxQueryDefinition): GeoBoundingBoxQueryBuilder = {
    val builder = QueryBuilders.geoBoundingBoxQuery(q.field)
    q.corners.foreach { case Corners(a, b, c, d) => builder.setCorners(a, b, c, d) }
    q.geohash.foreach { case (topleft, bottomright) => builder.setCorners(topleft, bottomright) }
    //q.cornersOGC.foreach { case (bl, tr) => builder.setCornersOGC(bl, tr) }
    q.validationMethod.map(_.name).map(GeoValidationMethod.valueOf).foreach(builder.setValidationMethod)
    q.ignoreUnmapped.foreach(builder.ignoreUnmapped)
    q.queryName.foreach(builder.queryName)
    q.geoExecType.map(_.name).map(GeoExecType.valueOf).foreach(builder.`type`)
    builder
  }
}
