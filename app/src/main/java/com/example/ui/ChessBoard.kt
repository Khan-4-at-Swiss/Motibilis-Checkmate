package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.BoardPosition
import com.example.chess.ChessMove
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle

enum class BoardTheme {
    OBSIDIAN_GOLD, WALNUT_IVORY, MIDNIGHT_EMERALD, CRIMSON_ROYALE
}

enum class PieceStyle {
    REGAL_MEDALLIONS, CLASSIC_SILHOUETTE, ROYAL_MINIMAL
}

data class ThemeColors(
    val lightSquare: Color,
    val darkSquare: Color,
    val border: Color,
    val text: Color
) {
    companion object {
        fun getColors(theme: BoardTheme): ThemeColors {
            return when (theme) {
                BoardTheme.OBSIDIAN_GOLD -> ThemeColors(
                    lightSquare = Color(0xFFF1E1B9), // Creamy Warm Gold
                    darkSquare = Color(0xFF232323),  // Matte Obsidian Black
                    border = Color(0xFFDF9E1F),      // Lustrous Royal Gold
                    text = Color(0xFFFFD700)
                )
                BoardTheme.WALNUT_IVORY -> ThemeColors(
                    lightSquare = Color(0xFFF0D9B5), // Soft Ivory
                    darkSquare = Color(0xFFB58863),  // Walnut Brown
                    border = Color(0xFF8B5A2B),      // Deep Wood
                    text = Color(0xFF3E2723)
                )
                BoardTheme.MIDNIGHT_EMERALD -> ThemeColors(
                    lightSquare = Color(0xFFE0E5D0), // Sage Gold-Cream
                    darkSquare = Color(0xFF134F30),  // Midnight Emerald Green
                    border = Color(0xFFC5A059),      // Pale Gold
                    text = Color(0xFFFFF0CA)
                )
                BoardTheme.CRIMSON_ROYALE -> ThemeColors(
                    lightSquare = Color(0xFFFFF5E6), // White Linen
                    darkSquare = Color(0xFF7A1C1C),  // Crimson Burgundy
                    border = Color(0xFFDF9E1F),      // Polished Gold
                    text = Color(0xFFFFD700)
                )
            }
        }
    }
}

@Composable
fun ChessBoard(
    board: Array<Array<ChessPiece?>>,
    selectedPos: BoardPosition?,
    legalMoves: List<ChessMove>,
    lastMove: ChessMove?,
    kingInCheckPos: BoardPosition?,
    isFlipped: Boolean,
    boardTheme: BoardTheme,
    pieceStyle: PieceStyle,
    onCellClick: (BoardPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = remember(boardTheme) { ThemeColors.getColors(boardTheme) }
    val cellIndices = if (isFlipped) (7 downTo 0).toList() else (0..7).toList()
    val density = LocalDensity.current
    
    // Outer Frame with deep 3D shadow and wood-gilded finish
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(28.dp, RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2E2E2E), Color(0xFF0C0C0C))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 5.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFD4AF37), Color(0xFFF1D279), Color(0xFF9A7B4F),
                        Color(0xFFD4AF37), Color(0xFFFFF0CA), Color(0xFFD4AF37)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(10.dp) // Outer thick board frame margin
            .background(Color.Black, RoundedCornerShape(10.dp))
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (r in cellIndices) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (c in cellIndices) {
                        val pos = BoardPosition(r, c)
                        val piece = board[r][c]
                        val isLight = (r + c) % 2 == 0
                        val squareBg = if (isLight) themeColors.lightSquare else themeColors.darkSquare
                        
                        // Highlights state
                        val isSelected = selectedPos == pos
                        val isLastMoveSrc = lastMove?.from == pos
                        val isLastMoveDest = lastMove?.to == pos
                        val isCheckingKing = kingInCheckPos == pos
                        
                        val isLegalTarget = legalMoves.any { it.to == pos }
                        val isLegalCapture = isLegalTarget && piece != null

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(squareBg)
                                .drawBehind {
                                    // Soft overlay highlights
                                    if (isSelected) {
                                        drawRect(Color(0x75D4AF37)) // Rich Gold Highlight
                                    } else if (isLastMoveSrc || isLastMoveDest) {
                                        drawRect(Color(0x409A7B4F)) // Rich Brass Highlight
                                    }
                                    if (isCheckingKing) {
                                        drawRect(Color(0xA5D32F2F)) // Vivid Crimson Warning
                                    }
                                    
                                    // Realistic 3D Square Bevel borders
                                    val bWidth = 2.dp.toPx()
                                    // Highlight on top & left
                                    drawLine(
                                        color = if (isLight) Color(0x65FFFFFF) else Color(0x2AFFFFFF),
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, 0f),
                                        strokeWidth = bWidth
                                    )
                                    drawLine(
                                        color = if (isLight) Color(0x65FFFFFF) else Color(0x2AFFFFFF),
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, size.height),
                                        strokeWidth = bWidth
                                    )
                                    // Shadow on bottom & right
                                    drawLine(
                                        color = if (isLight) Color(0x35000000) else Color(0x70000000),
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = bWidth
                                    )
                                    drawLine(
                                        color = if (isLight) Color(0x35000000) else Color(0x70000000),
                                        start = Offset(size.width, 0f),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = bWidth
                                    )
                                }
                                .clickable { onCellClick(pos) }
                                .testTag("square_${r}_${c}"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Draw chess piece with 3D contact shadow and upright rotation
                            if (piece != null) {
                                // 1. Ground contact shadow
                                Box(
                                    modifier = Modifier
                                        .size(32.dp, 12.dp)
                                        .align(Alignment.BottomCenter)
                                        .offset(y = (-3).dp)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(Color(0x85000000), Color(0x00000000))
                                            ),
                                            shape = CircleShape
                                        )
                                )
                                
                                // 2. Upright standing physical piece
                                ChessPieceItem(
                                    piece = piece,
                                    pieceStyle = pieceStyle,
                                    modifier = Modifier
                                        .fillMaxSize(0.85f)
                                        .align(Alignment.Center)
                                        .graphicsLayer {
                                            translationY = -density.run { 4.dp.toPx() } // Elevate 4dp vertically for 3D standing depth
                                        }
                                )
                            }

                            // Legal Move indicator dot
                            if (isLegalTarget) {
                                if (isLegalCapture) {
                                    // Elegant golden circular ring on opponent pieces
                                    Canvas(modifier = Modifier.fillMaxSize(0.85f)) {
                                        drawCircle(
                                            color = Color(0xFFD4AF37),
                                            radius = size.minDimension / 2f,
                                            style = Stroke(width = 3.dp.toPx())
                                        )
                                    }
                                } else {
                                    // Elegant soft gold dot on empty squares
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(Color(0xC0D4AF37), CircleShape)
                                            .border(1.dp, Color(0xFFFFF0CA), CircleShape)
                                    )
                                }
                            }

                            // Coordinate Labels (only along margins)
                            val labelStyle = TextStyle(
                                color = if (isLight) themeColors.darkSquare.copy(alpha = 0.5f) else themeColors.lightSquare.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // File letter along the bottom row of the board
                            if (r == if (isFlipped) 0 else 7) {
                                val fileChar = ('a' + c).toString()
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 2.dp, end = 2.dp),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    Text(text = fileChar, style = labelStyle)
                                }
                            }

                            // Rank number along the left col of the board
                            if (c == if (isFlipped) 7 else 0) {
                                val rankNum = (8 - r).toString()
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 2.dp, start = 2.dp),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    Text(text = rankNum, style = labelStyle)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Imperial 3D Golden Corner Brackets
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(16.dp)
                .border(2.5.dp, Color(0xFFD4AF37), RoundedCornerShape(topStart = 4.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(16.dp)
                .border(2.5.dp, Color(0xFFD4AF37), RoundedCornerShape(topEnd = 4.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(16.dp)
                .border(2.5.dp, Color(0xFFD4AF37), RoundedCornerShape(bottomStart = 4.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(16.dp)
                .border(2.5.dp, Color(0xFFD4AF37), RoundedCornerShape(bottomEnd = 4.dp))
        )
    }
}

@Composable
fun ChessPieceItem(
    piece: ChessPiece,
    pieceStyle: PieceStyle,
    modifier: Modifier = Modifier
) {
    val isWhite = piece.color == PieceColor.WHITE
    
    when (pieceStyle) {
        PieceStyle.REGAL_MEDALLIONS -> {
            // ULTRA-REALISTIC 3D METALLIC PIECE
            val bgGradient = if (isWhite) {
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFDF5), Color(0xFFECE4CE), Color(0xFFC0A678))
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(Color(0xFF3C3C3C), Color(0xFF242424), Color(0xFF0F0F0F))
                )
            }
            
            val borderBrush = if (isWhite) {
                Brush.linearGradient(
                    colors = listOf(Color(0xFFFFF0CA), Color(0xFFD4AF37), Color(0xFF9A7B4F))
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(Color(0xFF9A7B4F), Color(0xFF503606), Color(0xFF2A1C02))
                )
            }

            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                // 3D Extrusion Side Wall Thickness
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = 3.dp)
                        .background(if (isWhite) Color(0xFF8C6212) else Color(0xFF000000), CircleShape)
                )

                // Main Medallion Face
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(4.dp, CircleShape)
                        .background(bgGradient, CircleShape)
                        .border(1.5.dp, borderBrush, CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Highlights layer inside
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(0.5.dp, if (isWhite) Color(0xFFFFFFFF) else Color(0x35FFFFFF), CircleShape)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val symbol = piece.getSymbol()
                        val textBrush = if (isWhite) {
                            Brush.verticalGradient(listOf(Color(0xFF4A3710), Color(0xFF1F1504)))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFFFFF2D3), Color(0xFFD4AF37)))
                        }
                        
                        Box(contentAlignment = Alignment.Center) {
                            // 3D Engraving Shadow
                            Text(
                                text = symbol,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWhite) Color(0x70FFFFFF) else Color(0x90000000),
                                modifier = Modifier.offset(x = (-1).dp, y = (-1).dp)
                            )
                            Text(
                                text = symbol,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWhite) Color(0x90000000) else Color(0x70FFFFFF),
                                modifier = Modifier.offset(x = 1.5.dp, y = 1.5.dp)
                            )
                            // Main Symbol text
                            Text(
                                text = symbol,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                style = androidx.compose.ui.text.TextStyle(brush = textBrush)
                            )
                        }
                    }

                    // Glossy shine highlight arc at the top-left
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 10.dp, y = 10.dp)
                            .size(12.dp, 6.dp)
                            .background(Color(0x70FFFFFF), CircleShape)
                    )
                }
            }
        }
        
        PieceStyle.CLASSIC_SILHOUETTE -> {
            // REALISTIC 3D FROSTED GLASS PIECES
            val bgGradient = if (isWhite) {
                Brush.radialGradient(
                    colors = listOf(Color(0xE6FFFFFF), Color(0xB3F0D9B5), Color(0x80D4AF37))
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(Color(0xE62C2C2C), Color(0xB3111111), Color(0x809A7B4F))
                )
            }
            
            val borderBrush = if (isWhite) {
                Brush.linearGradient(
                    colors = listOf(Color(0xCCFFFFFF), Color(0x66D4AF37))
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(Color(0xCC9A7B4F), Color(0x40000000))
                )
            }

            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                // 3D Glass Thickness base
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = 3.dp)
                        .background(if (isWhite) Color(0x60000000) else Color(0x90000000), CircleShape)
                )

                // Glass Face
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgGradient, CircleShape)
                        .border(1.dp, borderBrush, CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val symbol = piece.getSymbol()
                    val textGradient = if (isWhite) {
                        Brush.verticalGradient(listOf(Color(0xFF553C11), Color(0xFF221100)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFFFFF7E6), Color(0xFFD4AF37)))
                    }

                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = symbol,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isWhite) Color(0x50000000) else Color(0x50FFFFFF),
                            modifier = Modifier.offset(x = 1.dp, y = 1.5.dp)
                        )
                        Text(
                            text = symbol,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = androidx.compose.ui.text.TextStyle(brush = textGradient)
                        )
                    }

                    // High shine glass reflection
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 8.dp, y = 8.dp)
                            .size(10.dp, 5.dp)
                            .background(Color(0xD0FFFFFF), CircleShape)
                    )
                }
            }
        }

        PieceStyle.ROYAL_MINIMAL -> {
            // NEON 3D FLOATING HOLOGRAM
            val neonColor = if (isWhite) Color(0xFF00E5FF) else Color(0xFFFF9100)
            
            val infiniteTransition = rememberInfiniteTransition(label = "hologram")
            val floatOffset by infiniteTransition.animateFloat(
                initialValue = -4f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "float"
            )

            Box(
                modifier = modifier
                    .graphicsLayer {
                        translationY = floatOffset
                    },
                contentAlignment = Alignment.Center
            ) {
                // Neon glow halo ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.5.dp, neonColor, CircleShape)
                        .background(neonColor.copy(alpha = 0.08f), CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = piece.getSymbol(),
                        fontSize = 36.sp,
                        color = neonColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.drawBehind {
                            // Render holographic bloom/glow behind the symbol text
                            drawCircle(
                                color = neonColor.copy(alpha = 0.25f),
                                radius = size.minDimension / 2.2f
                            )
                        }
                    )
                }
            }
        }
    }
}

// Wrapper to hold TextStyle
private fun TextStyle(color: Color, fontSize: androidx.compose.ui.unit.TextUnit, fontWeight: FontWeight): androidx.compose.ui.text.TextStyle {
    return androidx.compose.ui.text.TextStyle(
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight
    )
}
