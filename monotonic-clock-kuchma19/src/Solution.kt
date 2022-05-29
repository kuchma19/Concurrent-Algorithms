import java.lang.Integer.max

/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author : Kuchma Andrey
 */
class Solution : MonotonicClock {
    private var c1 by RegularInt(0)
    private var c2 by RegularInt(0)
    private var c by RegularInt(0)

    private var d1 by RegularInt(0)
    private var d2 by RegularInt(0)

    override fun write(time: Time) {
        val t1 = time.d1
        val t2 = time.d2
        val t = time.d3
        d1 = t1
        d2 = t2
        c = t
        c2 = d2
        c1 = d1
    }

    override fun read(): Time {
        val a1 = c1
        val a2 = c2
        val a = c
        val b2 = d2
        val b1 = d1
        val r1 = Time(a1, a2, a)
        val r2 = Time(b1, b2, a)
        return if (r1 == r2) {
            r1;
        } else {
            if (a1 == b1) {
                if (a2 == b2) {
                    r2
                } else {
                    Time(b1, b2, 0)
                }
            } else {
                Time(b1, 0, 0)
            }
        }
    }
}