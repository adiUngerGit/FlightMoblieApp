package com.example.myapplication


import android.content.Context
import android.graphics.*
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.contains
import androidx.core.graphics.minus
import androidx.core.graphics.plus
import kotlin.math.roundToInt


class JoystickAndSlidersView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /*---------------------------------------------------------------------------------/
    /---------------------------------- SHIPPED API -----------------------------------/
    /---------------------------------------------------------------------------------*/

    var onLeftSliderValueChanged: (() -> Unit)? = null

    var onBottomSliderValueChanged: (() -> Unit)? = null

    var onJoystickValueChange: (() -> Unit)? = null

    var leftKnobValue = 0.5f
        private set

    var bottomKnobValue = 0f
        private set

    var joystickHorizontalValue = 0f
        private set

    var joystickVerticalValue = 0f
        private set


    /*---------------------------------------------------------------------------------/
    /------------------------------- COMPONENTS LOGIC ---------------------------------/
    /---------------------------------------------------------------------------------*/

    enum class DraggedObj { LeftKnob, BottomKnob, JoystickKnob }

    private var draggedObj: DraggedObj? = null


    override fun onTouchEvent(event: MotionEvent?): Boolean {

        when (event?.action) {

            MotionEvent.ACTION_DOWN -> {
                draggedObj = objAtTouchPos(event.x, event.y)

                if (draggedObj == DraggedObj.JoystickKnob)
                    shouldStopAnimation = true

                // see documentation of 'animationLock' on this page

                synchronized(animationLock) {
                    touchDrag(event.x, event.y) // so the knob jumps to pos before the finger moves
                }
            }

            MotionEvent.ACTION_MOVE -> {
                touchDrag(event.x, event.y)
            }

            MotionEvent.ACTION_UP -> {
                if (draggedObj != null)
                    animateKnobToCtr()

                draggedObj = null
            }
        }
        return true
    }

    private fun touchDrag(x: Float, y: Float) = when (draggedObj) {

        DraggedObj.LeftKnob -> {

            leftKnobDrag(x, y)
        }
        DraggedObj.BottomKnob -> {

            bottomKnobDrag(x, y)
        }
        DraggedObj.JoystickKnob -> {

            jsKnobDrag(x, y)
        }
        else -> Unit
    }

    private fun leftKnobDrag(x: Float, y: Float) {

        val boundedY = maxOf(leftKnobMinY, minOf(y, leftKnobMaxY))

        leftSliderKnob = RectF(
            leftSliderKnob.left,
            boundedY - knobHalfActualSize,
            leftSliderKnob.right,
            boundedY + knobHalfActualSize
        )
        invalidate()

        leftKnobValue = 1 - (boundedY - leftKnobMinY) / (leftKnobMaxY - leftKnobMinY)

        val callback = onLeftSliderValueChanged
        callback?.invoke() // to make it thread safe
    }

    private fun bottomKnobDrag(x: Float, y: Float) {

        val boundedX = maxOf(bottomKnobMinX, minOf(x, bottomKnobMaxX))

        bottomSliderKnob = RectF(
            boundedX - knobHalfActualSize,
            bottomSliderKnob.top,
            boundedX + knobHalfActualSize,
            bottomSliderKnob.bottom
        )
        invalidate()

        bottomKnobValue = (boundedX - bottomKnobMinX) / (bottomKnobMaxX - bottomKnobMinX) * 2 - 1

        val callback = onBottomSliderValueChanged
        callback?.invoke() // to make it thread safe
    }

    private fun jsKnobDrag(x: Float, y: Float) {

        var vectorTouchToCtr = PointF(x, y).minus(centerJsBase)

        val vecLength = vectorTouchToCtr.length()

        if (vecLength > jsActualMovingRadius) { // decrease vector's length to max possible value
            // to make jsKnob never go out of jsBase
            vectorTouchToCtr = PointF(
                vectorTouchToCtr.x / vecLength * jsActualMovingRadius,
                vectorTouchToCtr.y / vecLength * jsActualMovingRadius
            )
        }

        joystickHorizontalValue = vectorTouchToCtr.x / jsActualMovingRadius

        joystickVerticalValue = -vectorTouchToCtr.y / jsActualMovingRadius

        centerJsKnob = vectorTouchToCtr.plus(centerJsBase)

        invalidate()

        val callback = onJoystickValueChange
        callback?.invoke() // to make it thread safe
    }

    private fun objAtTouchPos(x: Float, y: Float): DraggedObj? {
        val touch = PointF(x, y)

        return when {

            leftSliderRect.contains(touch) || leftSliderKnob.contains(touch)
            -> DraggedObj.LeftKnob

            bottomSliderRect.contains(touch) || bottomSliderKnob.contains(touch)
            -> DraggedObj.BottomKnob

            (dist(touch, centerJsBase) < baseJsRadius) || (dist(touch, centerJsKnob) < knobJsRadius)
            -> DraggedObj.JoystickKnob

            else -> null
        }
    }

    private fun dist(p1: PointF, p2: PointF) = p1.minus(p2).length()


    private var shouldStopAnimation = false // those two are solely for the following scenario:
    private val animationLock = Object() // user releases jsKnob and re grabs it mid-animation.
    // animation should abort immediately, and only then, the knob should jump to the touch pos.

    private fun schedule(millis: Long, block: () -> Unit) {
        Handler().postDelayed(block, millis)
    }

    private fun animateKnobToCtr() {
        val millisBetweenFrames = 10L
        val numFrames = 10 // total animation time: 0.1 seconds

        val apiDx = joystickHorizontalValue / numFrames // the shipped api values' rate of change
        val apiDy = joystickVerticalValue / numFrames // (would be around ± 0.1 per frame)

        val pixelDx = (centerJsKnob.x - centerJsBase.x) / numFrames // d change to circle's pixels
        val pixelDy = (centerJsKnob.y - centerJsBase.y) / numFrames // (tens or hundreds per frame)

        shouldStopAnimation = false

        (0 until numFrames).forEach { numFrame ->

            schedule(millis = numFrame * millisBetweenFrames)  {

                synchronized(animationLock) { // see explanation ~20 lines above

                    if (shouldStopAnimation) // if jsKnob is grabbed by now
                        return@schedule

                    if (numFrame + 1 < numFrames) { // every frame except the last one
                        joystickHorizontalValue -= apiDx
                        joystickVerticalValue -= apiDy
                        centerJsKnob -= PointF(pixelDx, pixelDy)
                    }
                    else { // go exactly to 0; to fix floating-point small error
                        joystickHorizontalValue = 0f
                        joystickVerticalValue = 0f
                        centerJsKnob = centerJsBase
                    }

                    invalidate()

                    val callback = onJoystickValueChange
                    callback?.invoke() // to make it thread safe
                }
            }
        }
    }

    /*---------------------------------------------------------------------------------/
    /---------------------------------- DRAWING... ------------------------------------/
    /---------------------------------------------------------------------------------*/

    override fun onDraw(canvas: Canvas) {

//        canvas.drawRect(allScreenRect, bgPaint)

//        bgImage.draw(canvas)

        canvas.drawCircle(centerJsBase.x, centerJsBase.y, baseJsRadius, weakPaint)
        canvas.drawCircle(centerJsBase.x, centerJsBase.y, baseJsRadius, weakBordersPaint)

        canvas.drawCircle(centerJsKnob.x, centerJsKnob.y, knobJsRadius, strongPaint)
        canvas.drawCircle(centerJsKnob.x, centerJsKnob.y, knobJsRadius, strongBordersPaint)

        canvas.drawRect(leftSliderRect, weakPaint)
        canvas.drawRect(leftSliderRect, weakBordersPaint)

        canvas.drawRect(leftSliderKnob, strongPaint)
        canvas.drawRect(leftSliderKnob, strongBordersPaint)

        canvas.drawRect(bottomSliderRect, weakPaint)
        canvas.drawRect(bottomSliderRect, weakBordersPaint)

        canvas.drawRect(bottomSliderKnob, strongPaint)
        canvas.drawRect(bottomSliderKnob, strongBordersPaint)
    }

    private var allScreenRect = RectF() // (left, top, right, bottom of screen)

    private var knobHalfActualSize = 0f

    private var leftKnobMinY = 0f
    private var leftKnobMaxY = 0f

    private var bottomKnobMinX = 0f
    private var bottomKnobMaxX = 0f

    private var jsActualMovingRadius = 0f

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {

        allScreenRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        val screen = minOf(width, height)

        // where the js area should start
        val shift = (2 * SLIDER_PADDING + SLIDER_SHORT_SIDE) * screen

        baseJsRadius = (screen - 2 * JS_MINIMAL_PADDING * screen - shift) / 2

        centerJsBase = PointF(
            (width - shift) / 2 + shift,
            (height - shift) / 2
        )

        knobJsRadius = baseJsRadius * JS_KNOB_TO_BASE_RADII_RATIO

        jsActualMovingRadius = baseJsRadius - knobJsRadius

        centerJsKnob = PointF(
            centerJsBase.x + joystickHorizontalValue * jsActualMovingRadius,
            centerJsBase.y - joystickVerticalValue * jsActualMovingRadius
        )


        knobHalfActualSize = SLIDER_KNOB_SIDE / 2 * screen

        leftSliderRect = RectF(
            SLIDER_PADDING * screen,
            SLIDER_PADDING * screen,
            (SLIDER_PADDING + SLIDER_SHORT_SIDE) * screen,
            height - SLIDER_PADDING * screen
        )

        bottomSliderRect = RectF(
            (3 * SLIDER_PADDING + SLIDER_SHORT_SIDE) * screen,
            height - (SLIDER_PADDING + SLIDER_SHORT_SIDE) * screen,
            width - SLIDER_PADDING * screen,
            height - SLIDER_PADDING * screen
        )

        leftKnobMinY = leftSliderRect.top + knobHalfActualSize
        leftKnobMaxY = leftSliderRect.bottom - knobHalfActualSize

        bottomKnobMinX = bottomSliderRect.left + knobHalfActualSize
        bottomKnobMaxX = bottomSliderRect.right - knobHalfActualSize

        leftSliderKnob = RectF(
            (SLIDER_PADDING + SLIDER_SHORT_SIDE / 2 - SLIDER_KNOB_SIDE / 2) * screen,
            leftKnobMaxY - leftKnobValue * (leftKnobMaxY - leftKnobMinY) - knobHalfActualSize,
            (SLIDER_PADDING + SLIDER_SHORT_SIDE / 2 + SLIDER_KNOB_SIDE / 2) * screen,
            leftKnobMaxY - leftKnobValue * (leftKnobMaxY - leftKnobMinY) + knobHalfActualSize
        )

        val bottomKnobActualPixelX = (bottomKnobValue + 1) / 2 *
                (bottomKnobMaxX - bottomKnobMinX) + bottomKnobMinX

        bottomSliderKnob = RectF(
            bottomKnobActualPixelX - knobHalfActualSize,
            bottomSliderRect.centerY() - knobHalfActualSize,
            bottomKnobActualPixelX + knobHalfActualSize,
            bottomSliderRect.centerY() + knobHalfActualSize
        )
    }

    private var centerJsBase = PointF()
    private var baseJsRadius = 0f

    private var centerJsKnob = PointF()
    private var knobJsRadius = 0f

    private var leftSliderRect = RectF()
    private var leftSliderKnob = RectF()

    private var bottomSliderRect = RectF()
    private var bottomSliderKnob = RectF()


    /*---------------------------------------------------------------------------------/
    /---------------------------- VALUES TO PLAY WITH ---------------------------------/
    /---------------------------------------------------------------------------------*/

    companion object {
        // constants regarding the elements' proportions (will be multiplied by screen size)

        const val SLIDER_PADDING = 0.05f
        const val SLIDER_SHORT_SIDE = 0.065f
        const val SLIDER_KNOB_SIDE = 0.095f
        const val JS_MINIMAL_PADDING = 0.05F
        const val JS_KNOB_TO_BASE_RADII_RATIO = 0.35f
    }

    private val weakPaint = Paint().apply { // base of joystick and base of sliders
        style = Paint.Style.FILL_AND_STROKE
        color = Color.rgb(48, 46, 48)
        isAntiAlias = true
    }

    private val strongPaint = Paint().apply { // knob of joystick and knob of sliders
        style = Paint.Style.FILL_AND_STROKE
        color = Color.rgb(20, 21, 24)
        isAntiAlias = true
    }

    private val weakBordersPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.rgb(34, 32, 30)
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val strongBordersPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.rgb(8, 8, 8)
        strokeWidth = 0f
        isAntiAlias = true

        setShadowLayer(3f, 0f, 0f, Color.LTGRAY)
        setLayerType(LAYER_TYPE_SOFTWARE, this)
    }

    private val bgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.rgb(240, 240, 240)
        isAntiAlias = true
    }
}