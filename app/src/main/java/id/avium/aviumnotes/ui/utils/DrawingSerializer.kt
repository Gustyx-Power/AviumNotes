package id.avium.aviumnotes.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import id.avium.aviumnotes.ui.screens.DrawingPath
import org.json.JSONArray
import org.json.JSONObject

object DrawingSerializer {

    fun serializeDrawingPaths(paths: List<DrawingPath>): String {
        val jsonArray = JSONArray()

        paths.forEach { drawingPath ->
            val pathObj = JSONObject()
            pathObj.put("color", drawingPath.color.value.toString())
            pathObj.put("strokeWidth", drawingPath.strokeWidth)

            // Serialize path points
            val pointsArray = JSONArray()
            val pathIterator = drawingPath.path.iterator()

            // Extract path commands
            val commands = mutableListOf<PathCommand>()
            pathIterator.forEach { segment ->
                when (segment.type) {
                    androidx.compose.ui.graphics.PathSegment.Type.Move -> {
                        commands.add(PathCommand.Move(segment.points[0], segment.points[1]))
                    }
                    androidx.compose.ui.graphics.PathSegment.Type.Line -> {
                        commands.add(PathCommand.Line(segment.points[0], segment.points[1]))
                    }
                    androidx.compose.ui.graphics.PathSegment.Type.Quadratic -> {
                        commands.add(PathCommand.Quadratic(
                            segment.points[0], segment.points[1],
                            segment.points[2], segment.points[3]
                        ))
                    }
                    androidx.compose.ui.graphics.PathSegment.Type.Conic -> {
                        commands.add(PathCommand.Conic(
                            segment.points[0], segment.points[1],
                            segment.points[2], segment.points[3],
                            segment.weight
                        ))
                    }
                    androidx.compose.ui.graphics.PathSegment.Type.Cubic -> {
                        commands.add(PathCommand.Cubic(
                            segment.points[0], segment.points[1],
                            segment.points[2], segment.points[3],
                            segment.points[4], segment.points[5]
                        ))
                    }
                    androidx.compose.ui.graphics.PathSegment.Type.Close -> {
                        commands.add(PathCommand.Close)
                    }
                    androidx.compose.ui.graphics.PathSegment.Type.Done -> {
                        // Skip
                    }
                }
            }

            // Serialize commands
            commands.forEach { cmd ->
                val cmdObj = JSONObject()
                when (cmd) {
                    is PathCommand.Move -> {
                        cmdObj.put("type", "move")
                        cmdObj.put("x", cmd.x)
                        cmdObj.put("y", cmd.y)
                    }
                    is PathCommand.Line -> {
                        cmdObj.put("type", "line")
                        cmdObj.put("x", cmd.x)
                        cmdObj.put("y", cmd.y)
                    }
                    is PathCommand.Quadratic -> {
                        cmdObj.put("type", "quad")
                        cmdObj.put("x1", cmd.x1)
                        cmdObj.put("y1", cmd.y1)
                        cmdObj.put("x2", cmd.x2)
                        cmdObj.put("y2", cmd.y2)
                    }
                    is PathCommand.Conic -> {
                        cmdObj.put("type", "conic")
                        cmdObj.put("x1", cmd.x1)
                        cmdObj.put("y1", cmd.y1)
                        cmdObj.put("x2", cmd.x2)
                        cmdObj.put("y2", cmd.y2)
                        cmdObj.put("weight", cmd.weight)
                    }
                    is PathCommand.Cubic -> {
                        cmdObj.put("type", "cubic")
                        cmdObj.put("x1", cmd.x1)
                        cmdObj.put("y1", cmd.y1)
                        cmdObj.put("x2", cmd.x2)
                        cmdObj.put("y2", cmd.y2)
                        cmdObj.put("x3", cmd.x3)
                        cmdObj.put("y3", cmd.y3)
                    }
                    is PathCommand.Close -> {
                        cmdObj.put("type", "close")
                    }
                }
                pointsArray.put(cmdObj)
            }

            pathObj.put("points", pointsArray)
            jsonArray.put(pathObj)
        }

        return jsonArray.toString()
    }

    fun deserializeDrawingPaths(json: String): List<DrawingPath> {
        val paths = mutableListOf<DrawingPath>()

        try {
            val jsonArray = JSONArray(json)

            for (i in 0 until jsonArray.length()) {
                val pathObj = jsonArray.getJSONObject(i)
                val color = Color(pathObj.getString("color").toULong())
                val strokeWidth = pathObj.getDouble("strokeWidth").toFloat()

                val path = Path()
                val pointsArray = pathObj.getJSONArray("points")

                for (j in 0 until pointsArray.length()) {
                    val cmdObj = pointsArray.getJSONObject(j)
                    when (cmdObj.getString("type")) {
                        "move" -> {
                            path.moveTo(
                                cmdObj.getDouble("x").toFloat(),
                                cmdObj.getDouble("y").toFloat()
                            )
                        }
                        "line" -> {
                            path.lineTo(
                                cmdObj.getDouble("x").toFloat(),
                                cmdObj.getDouble("y").toFloat()
                            )
                        }
                        "quad" -> {
                            path.quadraticBezierTo(
                                cmdObj.getDouble("x1").toFloat(),
                                cmdObj.getDouble("y1").toFloat(),
                                cmdObj.getDouble("x2").toFloat(),
                                cmdObj.getDouble("y2").toFloat()
                            )
                        }
                        "cubic" -> {
                            path.cubicTo(
                                cmdObj.getDouble("x1").toFloat(),
                                cmdObj.getDouble("y1").toFloat(),
                                cmdObj.getDouble("x2").toFloat(),
                                cmdObj.getDouble("y2").toFloat(),
                                cmdObj.getDouble("x3").toFloat(),
                                cmdObj.getDouble("y3").toFloat()
                            )
                        }
                        "close" -> {
                            path.close()
                        }
                    }
                }

                paths.add(DrawingPath(path, color, strokeWidth))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return paths
    }

    private sealed class PathCommand {
        data class Move(val x: Float, val y: Float) : PathCommand()
        data class Line(val x: Float, val y: Float) : PathCommand()
        data class Quadratic(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : PathCommand()
        data class Conic(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val weight: Float) : PathCommand()
        data class Cubic(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x3: Float, val y3: Float) : PathCommand()
        object Close : PathCommand()
    }
}

