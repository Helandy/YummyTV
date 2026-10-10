package su.afk.yummy.tv.data.player.extractor.alloha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest

class AllohaAssPositionFixTest : BaseUnitTest() {

    private fun assWith(
        playRes: String = "PlayResX: 1280\nPlayResY: 720\n",
        style: String = "Style: Default,Arial,40,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,2,2,10,10,60,1\n",
        dialogue: String,
    ) = """
        [Script Info]
        $playRes
        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        $style
        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        $dialogue
    """.trimIndent()

    @Test
    fun `bottom-center style with custom MarginV gets a pos tag reflecting it`() {
        val text =
            assWith(dialogue = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Hello, world!")
        val fixed = fixAssMarginPositions(text)
        assertTrue(fixed.contains("{\\pos(640,660)}Hello, world!"))
    }

    @Test
    fun `missing PlayResX or PlayResY injects synthetic values consistent with computed pos`() {
        val text =
            assWith(playRes = "", dialogue = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Hi")
        val fixed = fixAssMarginPositions(text)
        assertTrue(fixed.contains("PlayResX: 1000"))
        assertTrue(fixed.contains("PlayResY: 1000"))
        // Default style MarginV=60 from assWith(), synthetic res 1000 -> y = 1000 - 60 = 940.
        assertTrue(fixed.contains("{\\pos(500,940)}Hi"))
    }

    @Test
    fun `dialogue-level MarginV overrides the style's MarginV`() {
        val text =
            assWith(dialogue = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,200,,Custom margin")
        val fixed = fixAssMarginPositions(text)
        // playResY=720, overriding MarginV=200 -> y = 720 - 200 = 520.
        assertTrue(fixed.contains("{\\pos(640,520)}Custom margin"))
    }

    @Test
    fun `dialogue already carrying a pos override is left untouched`() {
        val dialogue =
            "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,{\\pos(100,200)}Already placed"
        val text = assWith(dialogue = dialogue)
        val fixed = fixAssMarginPositions(text)
        assertTrue(fixed.contains("{\\pos(100,200)}Already placed"))
        assertEquals(1, Regex("""\\pos\(""").findAll(fixed).count())
    }

    @Test
    fun `top-aligned style computes y from the top margin, not the bottom`() {
        val topStyle =
            "Style: Sign,Arial,40,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,2,8,10,10,30,1\n"
        val dialogue = "Dialogue: 0,0:00:01.00,0:00:03.00,Sign,,0,0,0,,Top text"
        val text = assWith(style = topStyle, dialogue = dialogue)
        val fixed = fixAssMarginPositions(text)
        // Alignment 8 (top-center): y = MarginV = 30, x = PlayResX/2 = 640.
        assertTrue(fixed.contains("{\\pos(640,30)}Top text"))
    }

    @Test
    fun `unknown style name falls back to default alignment without crashing`() {
        val dialogue = "Dialogue: 0,0:00:01.00,0:00:03.00,DoesNotExist,,0,0,0,,Fallback"
        val text = assWith(dialogue = dialogue)
        val fixed = fixAssMarginPositions(text)
        // No style match -> alignment=2, margins=0 -> y = 720, x = 640.
        assertTrue(fixed.contains("{\\pos(640,720)}Fallback"))
    }

    @Test
    fun `only the missing play res key is inserted`() {
        val text = assWith(
            playRes = "PlayResY: 720\n",
            dialogue = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Hi",
        )

        val fixed = fixAssMarginPositions(text)

        assertEquals(1, Regex("""(?m)^PlayResY:""").findAll(fixed).count())
        assertEquals(1, Regex("""(?m)^PlayResX:""").findAll(fixed).count())
        assertTrue(fixed.contains("PlayResY: 720"))
        assertTrue(fixed.contains("PlayResX: 1000"))
        // x от выдуманной ширины 1000, y от настоящей высоты 720: 720 - MarginV(60).
        assertTrue(fixed.contains("{\\pos(500,660)}Hi"))
    }

    @Test
    fun `dialogue with a move tag is left untouched`() {
        val dialogue = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,{\\move(0,0,10,10)}Moving"
        val fixed = fixAssMarginPositions(assWith(dialogue = dialogue))

        assertEquals(0, Regex("""\\pos\(""").findAll(fixed).count())
        assertTrue(fixed.contains("{\\move(0,0,10,10)}Moving"))
    }

    @Test
    fun `windows line endings are accepted and normalized`() {
        val text = assWith(dialogue = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Hello")
            .replace("\n", "\r\n")

        val fixed = fixAssMarginPositions(text)

        assertTrue(fixed.contains("{\\pos(640,660)}Hello"))
        assertTrue(!fixed.contains("\r"))
    }

    @Test
    fun `left and right aligned styles use the side margins`() {
        val left = "Style: Default,Arial,40,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,2,1,15,25,60,1\n"
        val right = left.replace(",1,15,25,60,1", ",3,15,25,60,1")
        val dialogue = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Side"

        val fixedLeft = fixAssMarginPositions(assWith(style = left, dialogue = dialogue))
        val fixedRight = fixAssMarginPositions(assWith(style = right, dialogue = dialogue))

        // Alignment 1: x = MarginL; alignment 3: x = PlayResX - MarginR.
        assertTrue(fixedLeft.contains("{\\pos(15,660)}Side"))
        assertTrue(fixedRight.contains("{\\pos(1255,660)}Side"))
    }

    @Test
    fun `middle aligned style is vertically centered`() {
        val middle = "Style: Default,Arial,40,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,2,5,10,10,60,1\n"
        val dialogue = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Middle"

        val fixed = fixAssMarginPositions(assWith(style = middle, dialogue = dialogue))

        assertTrue(fixed.contains("{\\pos(640,360)}Middle"))
    }

    @Test
    fun `commas in dialogue text are not split into fields`() {
        val dialogue = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,One, two, three"

        val fixed = fixAssMarginPositions(assWith(dialogue = dialogue))

        assertTrue(fixed.contains("{\\pos(640,660)}One, two, three"))
    }
}
